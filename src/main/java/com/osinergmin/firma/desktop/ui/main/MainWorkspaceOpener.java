package com.osinergmin.firma.desktop.ui.main;

import com.osinergmin.firma.desktop.App;
import com.osinergmin.firma.desktop.ui.UiBrand;
import com.osinergmin.firma.desktop.ui.UiTheme;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public final class MainWorkspaceOpener {

    public static final double MAIN_WIDTH = 1260;
    public static final double MAIN_HEIGHT = 820;

    private MainWorkspaceOpener() {
    }

    /** Reemplaza la escena del stage (ej. después del login validado). */
    public static void show(Stage stage) throws IOException {
        var url = Objects.requireNonNull(
                App.class.getResource("fxml/main/MainWorkspace.fxml"),
                "fxml/main/MainWorkspace.fxml");
        FXMLLoader loader = new FXMLLoader(url);
        Parent root = loader.load();
        MainWorkspaceController controller = loader.getController();
        controller.setStage(stage);
        controller.bindStageDependentBehavior();

        Scene scene = new Scene(root, MAIN_WIDTH, MAIN_HEIGHT);
        UiTheme.apply(scene);
        stage.setTitle("Osinergmin — Firma digital");
        stage.setMinWidth(1020);
        stage.setMinHeight(680);
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();
        stage.toFront();
        UiBrand.applyStageIcon(stage);
    }
}
