package com.osinergmin.firma.desktop.ui.login;

import com.osinergmin.firma.desktop.App;
import com.osinergmin.firma.desktop.ui.UiBrand;
import com.osinergmin.firma.desktop.ui.UiTheme;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.util.Objects;

public final class LoginWindowOpener {

    public static final double LOGIN_SCENE_WIDTH = 1040;
    public static final double LOGIN_SCENE_HEIGHT = 700;

    private static LoginController lastController;

    private LoginWindowOpener() {
    }

    public static LoginController getController() {
        return lastController;
    }

    public static void showPrimary(Stage primary) throws IOException {
        primary.initStyle(StageStyle.UNDECORATED);
        loadLoginOnto(primary);
        primary.show();
    }

    /** Vuelve a la pantalla de acceso usando el mismo {@link Stage}. */
    public static void returnToLogin(Stage stage) throws IOException {
        stage.setMaximized(false);
        stage.setFullScreen(false);
        stage.setIconified(false);
        loadLoginOnto(stage);
        stage.centerOnScreen();
        stage.show();
    }

    private static void loadLoginOnto(Stage stage) throws IOException {
        stage.setTitle("Osinergmin Firmador");
        var url = Objects.requireNonNull(
                App.class.getResource("fxml/login/LoginWindow.fxml"),
                "fxml/login/LoginWindow.fxml");
        FXMLLoader loader = new FXMLLoader(url);
        Parent root = loader.load();
        lastController = loader.getController();
        lastController.setStage(stage);

        Scene scene = new Scene(root, LOGIN_SCENE_WIDTH, LOGIN_SCENE_HEIGHT);
        UiTheme.apply(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(640);
        stage.setScene(scene);
        UiBrand.applyStageIcon(stage);
    }
}
