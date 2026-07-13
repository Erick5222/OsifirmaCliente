package com.osinergmin.firma.desktop.service.signing;

import com.osinergmin.firma.desktop.config.ClientConfiguration;
import com.osinergmin.firma.desktop.core.signing.SigningSessionContext;
import com.osinergmin.firma.desktop.config.SigningAdminSettings;
import com.osinergmin.firma.desktop.integration.invoke.SigningParam;
import com.osinergmin.firma.desktop.integration.invoke.SigningPositionPresets;
import pe.gob.osinergmin.firma.bean.ParametrosFirma;

import java.nio.file.Path;
import java.util.Optional;

/** Construye {@link ParametrosFirma} a partir de la parametria de invocacion y el visor PDF. */
final class SigningParametersBuilder {

    Optional<String> validateTsaForSignatureLevel(SigningParam param) {
        int nivel = SigningAdminSettings.effectiveSignatureLevelCode(param);
        if (nivel <= 1) {
            return Optional.empty();
        }
        String tsaUrl = SigningAdminSettings.effectiveWebTsa(param);
        if (tsaUrl != null && !tsaUrl.isBlank()) {
            return Optional.empty();
        }
        String levelCode = SigningAdminSettings.effectiveSignatureLevel(param);
        return Optional.of(
                "Nivel de firma "
                        + levelCode
                        + " (nivel "
                        + nivel
                        + ") requiere servidor TSA (URL). "
                        + "Configure webTsa en parametria.json o URL TSA en Administracion.");
    }

    Optional<String> validateBeforeSign(
            SigningParam param,
            Optional<Path> stampImage,
            Optional<SignaturePlacement> placement,
            boolean requireDragPlacement) {
        Optional<String> tsaError = validateTsaForSignatureLevel(param);
        if (tsaError.isPresent()) {
            return tsaError;
        }
        if (param.signatureFormat() != 1) {
            return Optional.empty();
        }
        if (param.isTagPositioningMode() && !param.usesTagPlacement()) {
            return Optional.of(
                    "positioningMode TA requiere signatureTagName (ej. GYAURI o «GYAURI» en el PDF).");
        }
        if (!param.applyImage()) {
            return Optional.empty();
        }
        if (stampImage.isEmpty()) {
            String configuredPath = ClientConfiguration.getSigningImagePath();
            String hint =
                    configuredPath.isBlank()
                            ? "No se encontro el sello grafico empaquetado del firmador."
                            : "No se encontro la imagen en signing.image.path: " + configuredPath;
            return Optional.of("Para sello grafico: " + hint);
        }
        if (requireDragPlacement && placement.isEmpty()) {
            return Optional.of(
                    "Debe arrastrar el sello al PDF y pulsar «Continuar con la firma» (FirmaArrastrar=true).");
        }
        return Optional.empty();
    }

    ParametrosFirma build(
            SigningWorkPaths paths,
            String certificateAlias,
            SigningParam param,
            Optional<Path> stampImage,
            Optional<SignaturePlacement> placement,
            boolean allowDragPlacement) {
        ParametrosFirma params = new ParametrosFirma();
        params.setTsl(SigningAdminSettings.effectiveTslUrl());
        params.setVerificaTsl(SigningAdminSettings.effectiveVerifyTsl());
        params.setCertificado(certificateAlias);
        params.setRutaDocumento(paths.inputPath().toAbsolutePath().toString());
        params.setDocFirmado(paths.outputPath().toAbsolutePath().toString());
        params.setStandardFirma(param.standardFirma());
        params.setTipoEnvoltura(1);
        params.setRazon(param.signatureReason());
        params.setTextSize(param.stampTextSize());
        params.setNivel(SigningAdminSettings.effectiveSignatureLevelCode(param));
        params.setFirmaUnica(false);
        params.setInfoContacto(nullToEmpty(param.signatureTextTemplate()));
        params.setTsa(SigningAdminSettings.effectiveWebTsa(param));
        params.setUsuarioTsa(SigningAdminSettings.effectiveUserTsa(param));
        params.setClaveTsa(SigningAdminSettings.effectivePasswordTsa(param));

        if (param.applyImage() && stampImage.isPresent()) {
            params.setImagen(stampImage.get().toAbsolutePath().toString());
        }
        params.setTipoFirma(param.signatureStyle());

        if (param.signatureFormat() == 1) {
            applyPosition(params, param, paths.inputPath(), placement, allowDragPlacement);
        }
        return params;
    }

    /** Parametros para XAdES / CAdES (sin sello grafico ni coordenadas PDF). */
    ParametrosFirma buildEnvelope(
            SigningWorkPaths paths, String certificateAlias, SigningParam param) {
        ParametrosFirma params = new ParametrosFirma();
        params.setTsl(SigningAdminSettings.effectiveTslUrl());
        params.setVerificaTsl(SigningAdminSettings.effectiveVerifyTsl());
        params.setCertificado(certificateAlias);
        params.setRutaDocumento(paths.inputPath().toAbsolutePath().toString());
        params.setDocFirmado(paths.outputPath().toAbsolutePath().toString());
        params.setStandardFirma(param.standardFirma());
        params.setTipoEnvoltura(1);
        params.setRazon(param.signatureReason());
        params.setNivel(SigningAdminSettings.effectiveSignatureLevelCode(param));
        params.setFirmaUnica(false);
        params.setInfoContacto(nullToEmpty(param.signatureTextTemplate()));
        params.setTsa(SigningAdminSettings.effectiveWebTsa(param));
        params.setUsuarioTsa(SigningAdminSettings.effectiveUserTsa(param));
        params.setClaveTsa(SigningAdminSettings.effectivePasswordTsa(param));
        return params;
    }

    private static void applyPosition(
            ParametrosFirma params,
            SigningParam param,
            Path inputPdf,
            Optional<SignaturePlacement> placement,
            boolean allowDragPlacement) {
        if (allowDragPlacement && placement.isPresent()) {
            SignaturePlacement sp = placement.get();
            params.setPagina(sp.pageNumberOneBased());
            params.setPosx(convertPosX(sp, param.signatureStyle()));
            params.setPosy(convertPosY(sp, param.signatureStyle()));
            return;
        }

        Optional<PdfTagPositionResolver.TagPosition> resolvedTag =
                SigningSessionContext.getResolvedTagPosition();
        if (resolvedTag.isPresent()) {
            PdfTagPositionResolver.TagPosition t = resolvedTag.get();
            params.setPagina(t.pageOneBased());
            params.setPosx(t.posX());
            params.setPosy(t.posY());
            System.out.println(
                    "[SigningParametersBuilder] Aplicando tag resuelto: pagina="
                            + t.pageOneBased()
                            + " posx="
                            + t.posX()
                            + " posy="
                            + t.posY());
            return;
        }

        if (param.usesTagPlacement() && inputPdf != null) {
            String marker = param.effectiveTagMarker();
            if (!marker.isBlank()) {
                Optional<PdfTagPositionResolver.TagPosition> tag =
                        PdfTagPositionResolver.find(inputPdf, marker);
                if (tag.isPresent()) {
                    PdfTagPositionResolver.TagPosition t = tag.get();
                    params.setPagina(t.pageOneBased());
                    params.setPosx(t.posX());
                    params.setPosy(t.posY());
                    return;
                }
                throw new IllegalStateException(
                        "Marcador '"
                                + marker
                                + "' no encontrado en el PDF. Verifique que el texto existe como capa de texto (no solo imagen).");
            }
        }

        params.setPagina(param.stampPage());
        if (param.hasPositioningMode() && !param.isTagPositioningMode()) {
            int[] preset = SigningPositionPresets.resolve(param.positioningMode());
            if (preset != null) {
                params.setPosx(preset[0]);
                params.setPosy(preset[1]);
                return;
            }
        }
        params.setPosx(param.positionX());
        params.setPosy(param.positionY());
    }

    private static int convertPosX(SignaturePlacement sp, int signatureStyle) {
        if (sp.overlayWidth() <= 0) {
            return ClientConfiguration.getSigningDefaultPosX();
        }
        double pageWidth = sp.pageWidthPoints();
        double leftX = (sp.overlayX() / sp.overlayWidth()) * pageWidth;
        double stampWidthPt = (sp.stampWidth() / sp.overlayWidth()) * pageWidth;
        double signatureWidthPt = estimateSignatureWidthPoints(stampWidthPt, signatureStyle);
        leftX = clamp(leftX, 0, Math.max(0, pageWidth - signatureWidthPt));
        return (int) Math.round(leftX);
    }

    private static int convertPosY(SignaturePlacement sp, int signatureStyle) {
        if (sp.overlayHeight() <= 0) {
            return ClientConfiguration.getSigningDefaultPosY();
        }
        double pageHeight = sp.pageHeightPoints();
        double stampHeightPt = (sp.stampHeight() / sp.overlayHeight()) * pageHeight;
        double signatureHeightPt = estimateSignatureHeightPoints(stampHeightPt, signatureStyle);
        // Mismo sistema que positioningMode/presets: posy crece hacia abajo (origen arriba-izquierda).
        double posY = (sp.overlayY() / sp.overlayHeight()) * pageHeight;
        posY = clamp(posY, 0, Math.max(0, pageHeight - signatureHeightPt));
        return (int) Math.round(posY);
    }

    /** Estilo 1 (logo+texto derecha) genera un campo mas ancho que el chip del visor. */
    private static double estimateSignatureWidthPoints(double stampWidthPt, int signatureStyle) {
        return switch (signatureStyle) {
            case 1 -> Math.max(stampWidthPt * 2.4, stampWidthPt + 80);
            case 2 -> Math.max(stampWidthPt * 1.2, stampWidthPt + 20);
            case 3 -> stampWidthPt;
            default -> Math.max(stampWidthPt * 1.1, stampWidthPt + 10);
        };
    }

    private static double estimateSignatureHeightPoints(double stampHeightPt, int signatureStyle) {
        return switch (signatureStyle) {
            case 2 -> Math.max(stampHeightPt * 1.8, stampHeightPt + 40);
            default -> Math.max(stampHeightPt * 1.1, stampHeightPt + 8);
        };
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(value, max));
    }

    String describeForLog(SigningParam param, Optional<SignaturePlacement> placement, boolean allowDragPlacement) {
        if (param.signatureFormat() != 1) {
            return "Formato " + param.standardFirma() + ": firma sin visor PDF.";
        }
        if (allowDragPlacement && placement.isPresent()) {
            SignaturePlacement sp = placement.get();
            return String.format(
                    "Posicion sello: pagina %d, posx=%d, posy=%d (visor)",
                    sp.pageNumberOneBased(), convertPosX(sp, param.signatureStyle()), convertPosY(sp, param.signatureStyle()));
        }
        if (SigningSessionContext.getResolvedTagPosition().isPresent()) {
            PdfTagPositionResolver.TagPosition t =
                    SigningSessionContext.getResolvedTagPosition().get();
            return String.format(
                    "Posicion sello: marcador '%s' -> pagina %d, posx=%d, posy=%d",
                    param.effectiveTagMarker(),
                    t.pageOneBased(),
                    t.posX(),
                    t.posY());
        }
        if (param.usesTagPlacement() && !param.effectiveTagMarker().isBlank()) {
            return String.format(
                    "Posicion sello: marcador '%s' (positioningMode=%s) — pendiente de resolver",
                    param.effectiveTagMarker(),
                    param.hasPositioningMode() ? param.positioningMode() : "TA");
        }
        if (param.hasPositioningMode()) {
            int[] preset = SigningPositionPresets.resolve(param.positioningMode());
            if (preset != null) {
                return String.format(
                        "Posicion sello: pagina %d, modo %s -> posx=%d, posy=%d",
                        param.stampPage(), param.positioningMode(), preset[0], preset[1]);
            }
        }
        return String.format(
                "Posicion sello: pagina %d, posx=%d, posy=%d (param)",
                param.stampPage(), param.positionX(), param.positionY());
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
