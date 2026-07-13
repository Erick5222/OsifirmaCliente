package com.osinergmin.firma.desktop.service.signing;

import com.osinergmin.firma.desktop.config.ClientConfiguration;
import com.osinergmin.firma.desktop.core.signing.SigningSessionContext;
import com.osinergmin.firma.desktop.integration.invoke.InvokeConfig;
import com.osinergmin.firma.desktop.integration.invoke.SigningParam;
import com.osinergmin.firma.desktop.pdf.PdfViewerConfig;
import pe.gob.osinergmin.firma.bean.ParametrosFirma;
import pe.gob.osinergmin.firma.servicios.ServicioFirma;
import pe.gob.osinergmin.firma.servicios.ServicioFirmaCAdES;
import pe.gob.osinergmin.firma.servicios.ServicioFirmaXAdES;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/** Firma PAdES, XAdES y CAdES delegando en firmaOsinergmin. */
public final class DocumentSigningService {

    private final SigningParametersBuilder parametersBuilder = new SigningParametersBuilder();

    public String describePlacementForLog(InvokeConfig invokeConfig, Optional<SignaturePlacement> placement) {
        SigningParam param =
                invokeConfig != null ? invokeConfig.signingParam() : SigningParam.defaults();
        boolean allowDrag = invokeConfig == null || invokeConfig.isFirmaArrastrar();
        return parametersBuilder.describeForLog(param, placement, allowDrag);
    }

    /**
     * Resuelve el marcador en el PDF antes de firmar. Devuelve mensaje de error si no se encuentra.
     */
    public Optional<String> resolveTagPlacementOrError(PdfViewerConfig document, SigningParam param) {
        if (param == null || param.signatureFormat() != 1) {
            SigningSessionContext.clearResolvedTagPosition();
            return Optional.empty();
        }
        if (param.isTagPositioningMode() && !param.usesTagPlacement()) {
            SigningSessionContext.clearResolvedTagPosition();
            return Optional.of(
                    "positioningMode=TA pero signatureTagName esta vacio. "
                            + "Use parametria CP-15 (o copiela a parametria.json) y recompile el firmador.");
        }
        if (!param.usesTagPlacement()) {
            SigningSessionContext.clearResolvedTagPosition();
            return Optional.empty();
        }
        String marker = param.effectiveTagMarker();
        if (marker.isBlank()) {
            return Optional.of("signatureTagName vacio; no se puede usar positioningMode TA.");
        }
        Optional<PdfTagPositionResolver.TagPosition> resolved =
                PdfTagPositionResolver.findFromDocument(document, marker);
        if (resolved.isEmpty()) {
            SigningSessionContext.clearResolvedTagPosition();
            return Optional.of(
                    "Marcador '"
                            + marker
                            + "' no encontrado en el documento. "
                            + "Compruebe que el PDF tiene texto seleccionable (no escaneado).");
        }
        SigningSessionContext.setResolvedTagPosition(resolved.get());
        return Optional.empty();
    }

    public SigningResult sign(
            PdfViewerConfig sourceDocument,
            String certificateAlias,
            InvokeConfig invokeConfig,
            Optional<SignaturePlacement> placement) {
        if (sourceDocument == null) {
            return SigningResult.failure("No hay documento para firmar.");
        }
        if (certificateAlias == null || certificateAlias.isBlank()) {
            return SigningResult.failure("No hay certificado seleccionado.");
        }

        SigningParam param =
                invokeConfig != null ? invokeConfig.signingParam() : SigningParam.defaults();
        SigningDocumentNaming.OptionalFormatValidation formatCheck =
                SigningDocumentNaming.validateForSigning(param, sourceDocument.displayFileName());
        if (!formatCheck.valid()) {
            return SigningResult.failure(formatCheck.message());
        }

        SigningWorkPaths paths = null;
        Path stampImagePath = null;
        try {
            boolean isPades = param.signatureFormat() == 1;
            Optional<Path> stampImage =
                    isPades ? SigningInvokeResources.resolveStampImagePath(invokeConfig) : Optional.empty();
            stampImagePath = stampImage.orElse(null);

            boolean allowDragPlacement =
                    isPades && (invokeConfig == null || invokeConfig.isFirmaArrastrar());
            boolean requireDragPlacement =
                    allowDragPlacement
                            && invokeConfig != null
                            && !invokeConfig.shouldSkipDocumentViewer();
            Optional<String> validationError =
                    parametersBuilder.validateBeforeSign(
                            param, stampImage, placement, requireDragPlacement);
            if (validationError.isPresent()) {
                return SigningResult.failure(validationError.get());
            }

            paths = SigningWorkPathResolver.prepare(sourceDocument, param);

            if (isPades && param.usesTagPlacement()) {
                String marker = param.effectiveTagMarker();
                if (!PdfTagPositionResolver.eraseMarker(paths.inputPath(), marker)) {
                    paths.cleanupOnFailure();
                    return SigningResult.failure(
                            "No se pudo eliminar el marcador '" + marker + "' del PDF antes de firmar.");
                }
            }

            ParametrosFirma libraryParams;
            try {
                libraryParams =
                        isPades
                                ? parametersBuilder.build(
                                        paths,
                                        certificateAlias.trim(),
                                        param,
                                        stampImage,
                                        allowDragPlacement ? placement : Optional.empty(),
                                        allowDragPlacement)
                                : parametersBuilder.buildEnvelope(paths, certificateAlias.trim(), param);
            } catch (IllegalStateException ex) {
                paths.cleanupOnFailure();
                return SigningResult.failure(ex.getMessage());
            }

            if (isPades && param.usesTagPlacement()) {
                System.out.println(
                        "[DocumentSigningService] Coordenadas enviadas al motor: pagina="
                                + libraryParams.getPagina()
                                + " posx="
                                + libraryParams.getPosx()
                                + " posy="
                                + libraryParams.getPosy());
            }

            SigningEngineResult engine = runSigningEngine(param.signatureFormat(), libraryParams);

            if (!engine.success()) {
                String message =
                        normalizeFailureMessage(
                                param.standardFirma(), engine.message(), stampImage);
                System.err.println("[DocumentSigningService] Motor firma: " + message);
                paths.cleanupOnFailure();
                return SigningResult.failure(message);
            }

            if (!Files.isRegularFile(paths.outputPath())) {
                paths.cleanupOnFailure();
                return SigningResult.failure("No se genero el archivo firmado.");
            }

            byte[] signedBytes = Files.readAllBytes(paths.outputPath());
            String signedName =
                    SigningDocumentNaming.buildSignedDisplayName(
                            sourceDocument.displayFileName(), param);
            PdfViewerConfig signedConfig = PdfViewerConfig.fromInMemory(signedBytes, signedName);
            paths.cleanupAllTemporary();
            deleteIfTemporary(stampImagePath, invokeConfig);
            return SigningResult.success(safeMessage(engine.message()), signedConfig);
        } catch (Exception ex) {
            if (paths != null) {
                paths.cleanupOnFailure();
            }
            deleteIfTemporary(stampImagePath, invokeConfig);
            String message = ex.getMessage();
            if (message == null || message.isBlank()) {
                message = ex.getClass().getSimpleName();
            }
            return SigningResult.failure(message);
        }
    }

    private static SigningEngineResult runSigningEngine(int signatureFormat, ParametrosFirma params) {
        return switch (signatureFormat) {
            case 2 -> {
                ServicioFirmaXAdES svc = new ServicioFirmaXAdES();
                svc.firmar(params);
                yield new SigningEngineResult(svc.isEstado(), svc.getMensaje());
            }
            case 3 -> {
                ServicioFirmaCAdES svc = new ServicioFirmaCAdES();
                svc.firmar(params);
                yield new SigningEngineResult(svc.isEstado(), svc.getMensaje());
            }
            default -> {
                ServicioFirma svc = new ServicioFirma();
                svc.firmar(params);
                yield new SigningEngineResult(svc.isEstado(), svc.getMensaje());
            }
        };
    }

    private record SigningEngineResult(boolean success, String message) {}

    private static void deleteIfTemporary(Path stampImagePath, InvokeConfig invokeConfig) {
        if (stampImagePath == null || invokeConfig == null || !invokeConfig.stampBytes().isPresent()) {
            return;
        }
        try {
            Files.deleteIfExists(stampImagePath);
        } catch (Exception ignored) {
            // best effort
        }
    }

    private static String safeMessage(String message) {
        return message == null || message.isBlank() ? "Firma completada." : message.trim();
    }

    private static String normalizeFailureMessage(
            String standard, String libraryMessage, Optional<Path> stampImage) {
        if (libraryMessage != null && !libraryMessage.isBlank()) {
            String trimmed = libraryMessage.trim();
            if (trimmed.contains("Host name") || trimmed.contains("NullPointerException")) {
                return "La firma "
                        + standard
                        + " fallo al aplicar sello de tiempo (TSA). "
                        + "Nivel T o LTA requiere URL TSA valida en parametria o panel Administracion.";
            }
            return trimmed;
        }
        if (!"PAdES".equals(standard)) {
            return "La firma " + standard + " fallo en el motor criptografico.";
        }
        if (stampImage.isEmpty()) {
            String configuredPath = ClientConfiguration.getSigningImagePath();
            if (configuredPath.isBlank()) {
                return "La firma PAdES fallo. Configure signing.image.path o envie stamp en Base64.";
            }
            return "La firma PAdES fallo. Verifique signing.image.path: " + configuredPath;
        }
        return "La firma PAdES fallo en el motor criptografico. "
                + "Revise certificado, TSL y parametros del sello.";
    }
}
