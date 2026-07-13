package com.osinergmin.firma.desktop.service.verification;

import com.osinergmin.firma.desktop.config.SigningAdminSettings;
import com.osinergmin.firma.desktop.integration.invoke.SigningParam;
import com.osinergmin.firma.desktop.pdf.PdfViewerConfig;
import com.osinergmin.firma.desktop.service.signing.SigningDocumentNaming;
import pe.gob.osinergmin.firma.bean.ParametrosVerificacion;
import pe.gob.osinergmin.firma.servicios.ServicioVerificacion;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

/** Verifica documentos firmados PAdES, XAdES o CAdES. */
public final class DocumentVerificationService {

    public VerificationResult verify(
            PdfViewerConfig signedDocument, Optional<byte[]> originalBytes, SigningParam param) {
        if (signedDocument == null) {
            return VerificationResult.failure("No hay documento firmado para verificar.");
        }
        int format = param != null ? param.signatureFormat() : 1;

        Path tempSigned = null;
        Path tempOriginal = null;
        try {
            tempSigned = writeTemp(signedDocument, SigningDocumentNaming.outputExtensionFor(param));
            ParametrosVerificacion params = new ParametrosVerificacion();
            params.setDoc(tempSigned.toAbsolutePath().toString());
            params.setTsl(SigningAdminSettings.effectiveTslUrl());
            params.setVerificaTsl(SigningAdminSettings.effectiveVerifyTsl());

            if (format == 3 && originalBytes.isPresent() && originalBytes.get().length > 0) {
                String origExt =
                        SigningDocumentNaming.inputExtensionFor(
                                param, inferOriginalName(signedDocument.displayFileName()));
                tempOriginal = Files.createTempFile("osinergmin-orig-", origExt);
                Files.write(tempOriginal, originalBytes.get());
                params.setDocOriginal(tempOriginal.toAbsolutePath().toString());
            } else {
                params.setDocOriginal("");
            }

            return VerificationResult.from(new ServicioVerificacion().verificar(params));
        } catch (Exception ex) {
            String message = ex.getMessage();
            if (message == null || message.isBlank()) {
                message = ex.getClass().getSimpleName();
            }
            return VerificationResult.failure(message);
        } finally {
            deleteIfExists(tempSigned);
            deleteIfExists(tempOriginal);
        }
    }

    private static String inferOriginalName(String signedName) {
        if (signedName == null) {
            return "documento.bin";
        }
        String name = signedName.trim();
        if (name.endsWith("[F].p7s")) {
            return name.substring(0, name.length() - 7) + ".bin";
        }
        return name;
    }

    private static Path writeTemp(PdfViewerConfig document, String suffix) throws IOException {
        Path temp = Files.createTempFile("osinergmin-verify-", suffix);
        if (document.usesLocalFile()) {
            Files.copy(Path.of(document.fileSystemPath()), temp);
        } else {
            Files.write(temp, document.readDocumentBytes());
        }
        return temp;
    }

    private static void deleteIfExists(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // best effort
        }
    }
}
