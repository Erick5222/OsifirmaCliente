package com.osinergmin.firma.desktop.integration.invoke;

import com.osinergmin.firma.desktop.config.ClientConfiguration;
import javafx.application.Application;

import java.nio.file.Path;

public final class InvokeConfigResolver {

    private static volatile InvokeConfig lastResolved = InvokeConfig.defaultDemo();

    private InvokeConfigResolver() {
    }

    public static InvokeConfig resolve(Application application) {
        InvokeConfig config;
        if (ClientConfiguration.isParametriaEnabled()) {
            Path path = ClientConfiguration.findParametriaFile().orElseGet(ClientConfiguration::getParametriaFilePath);
            config = ParametriaLoader.load(path)
                    .orElseGet(() -> {
                        System.err.println("[InvokeConfigResolver] parametria.enabled=true pero no se cargo "
                                + path + " (user.dir=" + System.getProperty("user.dir", ".")
                                + "); se usan argumentos de aplicacion.");
                        return ExternalInvokeParams.fromApplication(application).toInvokeConfig();
                    });
            System.out.println("[InvokeConfigResolver] Parametria desde " + path + " -> " + config
                    + ", skipViewer=" + config.shouldSkipDocumentViewer());
        } else {
            config = ExternalInvokeParams.fromApplication(application).toInvokeConfig();
            System.out.println("[InvokeConfigResolver] Parametria desde argumentos -> " + config);
        }
        lastResolved = config;
        return config;
    }

    public static InvokeConfig getLastResolved() {
        return lastResolved;
    }
}
