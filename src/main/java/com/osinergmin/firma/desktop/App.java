package com.osinergmin.firma.desktop;



import com.osinergmin.firma.desktop.integration.LaunchContext;

import com.osinergmin.firma.desktop.service.signing.SigningTempCleanup;

import com.osinergmin.firma.desktop.ui.UiFonts;
import com.osinergmin.firma.desktop.ui.login.LoginController;
import com.osinergmin.firma.desktop.ui.login.LoginWindowOpener;



import javafx.application.Application;

import javafx.stage.Stage;



import java.io.IOException;



/** Punto de entrada JavaFX — acceso institucional Osinergmin. */

public class App extends Application {



    @Override

    public void start(Stage stage) throws IOException {
        UiFonts.ensureLoaded();
        LaunchContext.init(this);



        var invokeConfig = LaunchContext.get().invokeConfig();



        LoginWindowOpener.showPrimary(stage);



        if (invokeConfig.hasToken()) {

            LoginController loginController = LoginWindowOpener.getController();

            if (loginController != null) {

                loginController.startParametriaTokenEntry(invokeConfig);

            }

        }

    }



    @Override

    public void stop() {

        SigningTempCleanup.purgeWorkDirectory();

    }



    public static void main(String[] args) {
        // Windows pasa la URL osinergmin-firmador://... como argumento; sin esto se ignora y queda el PDF demo.
        launch(args);
    }

}

