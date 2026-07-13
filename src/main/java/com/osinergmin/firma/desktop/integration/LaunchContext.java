package com.osinergmin.firma.desktop.integration;

import com.osinergmin.firma.desktop.integration.invoke.InvokeConfig;
import com.osinergmin.firma.desktop.integration.invoke.InvokeConfigResolver;
import com.osinergmin.firma.desktop.integration.invoke.SigningParam;
import com.osinergmin.firma.desktop.pdf.PdfViewerConfig;
import javafx.application.Application;

import java.nio.file.Path;
import java.util.Optional;

/** Contexto de arranque: parametria resuelta y documento PDF para pruebas. */
public final class LaunchContext {

    private static volatile LaunchContext instance;

    private final InvokeConfig invokeConfig;
    private final Optional<Path> documentPath;

    private LaunchContext(InvokeConfig invokeConfig, Optional<Path> documentPath) {
        this.invokeConfig = invokeConfig;
        this.documentPath = documentPath;
    }

    public static void init(Application application) {
        InvokeConfig config = InvokeConfigResolver.resolve(application);
        Optional<Path> doc = DocumentPathResolver.resolve(config);
        instance = new LaunchContext(config, doc);
        if (config.hasInlineDocument()) {
            System.out.println(
                    "[LaunchContext] Documento inline (bytes) -> "
                            + config.signingParam().fileName());
        } else if (config.usesDocumentUrl()) {
            System.out.println("[LaunchContext] Documento por URL -> " + config.getDocumentoUrl());
        } else if (doc.isPresent()) {
            System.out.println("[LaunchContext] Documento: " + doc.get().toAbsolutePath());
        } else if (config.usesAlfrescoDocument()) {
            System.out.println("[LaunchContext] Documento Alfresco nodeId=" + config.getIdDocumentoAlfresco());
        } else {
            System.out.println("[LaunchContext] Sin documento local; se usara PDF de demostracion.");
        }
        SigningParam param = config.signingParam();
        System.out.println(
                "[LaunchContext] Param firma: formato="
                        + param.signatureFormat()
                        + " tag="
                        + param.signatureTagName()
                        + " modoPos="
                        + param.positioningMode()
                        + " xy="
                        + param.positionX()
                        + ","
                        + param.positionY()
                        + " arrastrar="
                        + config.isFirmaArrastrar());
    }

    public static LaunchContext get() {
        return instance != null ? instance : new LaunchContext(InvokeConfig.defaultDemo(), Optional.empty());
    }

    public InvokeConfig invokeConfig() {
        return invokeConfig;
    }

    public Optional<Path> documentPath() {
        return documentPath;
    }

    public PdfViewerConfig pdfViewerConfig() {
        if (invokeConfig.usesAlfrescoDocument()) {
            throw new IllegalStateException("Documento Alfresco: cargar via AlfrescoContentClient.");
        }
        if (invokeConfig.usesDocumentUrl()) {
            throw new IllegalStateException("Documento URL: cargar via DocumentUrlClient.");
        }
        if (invokeConfig.hasInlineDocument()) {
            byte[] bytes = invokeConfig.documentBytes().orElseThrow();
            SigningParam param = invokeConfig.signingParam();
            return PdfViewerConfig.fromInMemory(bytes, param.fileName());
        }
        if (documentPath.isPresent()) {
            return PdfViewerConfig.fromLocalFile(documentPath.get());
        }
        return PdfViewerConfig.documentoPruebaDemo();
    }
}
