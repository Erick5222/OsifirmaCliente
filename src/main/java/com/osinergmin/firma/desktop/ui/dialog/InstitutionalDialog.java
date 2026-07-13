package com.osinergmin.firma.desktop.ui.dialog;

import com.osinergmin.firma.desktop.ui.UiTheme;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

/**
 * Diálogos modales con apariencia institucional Osinergmin (sustituye {@code Alert} nativo).
 */
public final class InstitutionalDialog {

    public enum Kind {
        INFO("Información", "ℹ", "institutional-dialog-icon-info"),
        SUCCESS("Operación exitosa", "✓", "institutional-dialog-icon-success"),
        WARNING("Advertencia", "!", "institutional-dialog-icon-warning"),
        ERROR("Error", "✕", "institutional-dialog-icon-error");

        private final String defaultHeader;
        private final String glyph;
        private final String iconStyleClass;

        Kind(String defaultHeader, String glyph, String iconStyleClass) {
            this.defaultHeader = defaultHeader;
            this.glyph = glyph;
            this.iconStyleClass = iconStyleClass;
        }
    }

    private InstitutionalDialog() {}

    public static void info(Window owner, String header, String message) {
        show(Kind.INFO, owner, header, message);
    }

    public static void info(Window owner, String message) {
        info(owner, Kind.INFO.defaultHeader, message);
    }

    public static void success(Window owner, String header, String message) {
        show(Kind.SUCCESS, owner, header, message);
    }

    public static void success(Window owner, String message) {
        success(owner, Kind.SUCCESS.defaultHeader, message);
    }

    public static void warning(Window owner, String header, String message) {
        show(Kind.WARNING, owner, header, message);
    }

    public static void warning(Window owner, String message) {
        warning(owner, Kind.WARNING.defaultHeader, message);
    }

    public static void error(Window owner, String header, String message) {
        show(Kind.ERROR, owner, header, message);
    }

    public static void error(Window owner, String message) {
        error(owner, Kind.ERROR.defaultHeader, message);
    }

    public static void show(Kind kind, Window owner, String header, String message) {
        String resolvedHeader = header == null || header.isBlank() ? kind.defaultHeader : header.trim();
        String resolvedMessage = message == null ? "" : message.trim();

        Stage dialogStage = new Stage(StageStyle.UNDECORATED);
        if (owner != null) {
            dialogStage.initOwner(owner);
        }
        dialogStage.initModality(Modality.APPLICATION_MODAL);

        Runnable closeAction = dialogStage::close;
        VBox root = buildContent(kind, resolvedHeader, resolvedMessage, closeAction);

        Scene scene = new Scene(root);
        UiTheme.apply(scene);
        dialogStage.setScene(scene);
        dialogStage.sizeToScene();

        scene.setOnKeyPressed(
                event -> {
                    if (event.getCode() == KeyCode.ESCAPE || event.getCode() == KeyCode.ENTER) {
                        closeAction.run();
                    }
                });

        if (owner != null) {
            dialogStage.setOnShown(
                    event -> {
                        double x = owner.getX() + (owner.getWidth() - dialogStage.getWidth()) / 2;
                        double y = owner.getY() + (owner.getHeight() - dialogStage.getHeight()) / 2;
                        dialogStage.setX(x);
                        dialogStage.setY(y);
                    });
        }

        dialogStage.showAndWait();
    }

    private static VBox buildContent(Kind kind, String header, String message, Runnable onClose) {
        VBox root = new VBox();
        root.getStyleClass().add("institutional-dialog-root");
        root.setFillWidth(true);

        HBox headerBar = new HBox();
        headerBar.getStyleClass().add("institutional-dialog-header");
        headerBar.setAlignment(Pos.CENTER_LEFT);

        javafx.scene.control.Label titleLabel = new javafx.scene.control.Label(header);
        titleLabel.getStyleClass().add("institutional-dialog-header-title");
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(420);
        headerBar.getChildren().add(titleLabel);

        HBox body = new HBox(16);
        body.getStyleClass().add("institutional-dialog-body");
        body.setAlignment(Pos.TOP_LEFT);

        javafx.scene.control.Label icon = new javafx.scene.control.Label(kind.glyph);
        icon.getStyleClass().addAll("institutional-dialog-icon", kind.iconStyleClass);
        icon.setAlignment(Pos.CENTER);

        javafx.scene.control.Label messageLabel = new javafx.scene.control.Label(message);
        messageLabel.getStyleClass().add("institutional-dialog-message");
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(420);
        HBox.setHgrow(messageLabel, Priority.ALWAYS);

        body.getChildren().addAll(icon, messageLabel);

        HBox footer = new HBox();
        footer.getStyleClass().add("institutional-dialog-footer");
        footer.setAlignment(Pos.CENTER_RIGHT);

        Button acceptButton = new Button("Aceptar");
        acceptButton.getStyleClass().addAll("button", "cert-modal-btn", "cert-modal-btn-primary");
        acceptButton.setDefaultButton(true);
        acceptButton.setFocusTraversable(false);
        acceptButton.setOnAction(event -> onClose.run());

        footer.getChildren().add(acceptButton);
        root.getChildren().addAll(headerBar, body, footer);
        return root;
    }
}
