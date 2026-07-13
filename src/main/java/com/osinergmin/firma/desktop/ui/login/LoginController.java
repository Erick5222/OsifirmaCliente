package com.osinergmin.firma.desktop.ui.login;

import com.osinergmin.firma.desktop.core.auth.LoginRememberMeStore;
import com.osinergmin.firma.desktop.core.certificate.CertificateCatalog;
import com.osinergmin.firma.desktop.integration.invoke.InvokeConfig;
import com.osinergmin.firma.desktop.service.auth.AuthenticationService;
import com.osinergmin.firma.desktop.service.auth.LoginResult;
import com.osinergmin.firma.desktop.service.auth.TokenValidationResult;
import com.osinergmin.firma.desktop.service.certificate.CertificateListResult;
import com.osinergmin.firma.desktop.service.certificate.CertificateService;
import com.osinergmin.firma.desktop.ui.dialog.InstitutionalDialog;
import com.osinergmin.firma.desktop.ui.main.MainWorkspaceOpener;
import javafx.animation.Interpolator;
import javafx.animation.PauseTransition;
import javafx.animation.RotateTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.security.KeyStore;

import pe.gob.osinergmin.firma.servicios.ServicioCertificado;

/**
 * Pantalla de acceso institucional con autenticacion contra firma-digital-service.
 */
public class LoginController {

    private static final String GLYPH_PENDING = "•";
    private static final String GLYPH_WORKING = "\u27F3";
    private static final String GLYPH_DONE = "\u2713";

    @FXML
    private BorderPane mainShell;

    @FXML
    private StackPane loginFormRoot;

    @FXML
    private VBox validationModalRoot;

    @FXML
    private StackPane validationOverlay;

    @FXML
    private ProgressBar validationProgressBar;

    @FXML
    private HBox validationRow1;

    @FXML
    private HBox validationRow2;

    @FXML
    private HBox validationRow3;

    @FXML
    private Label validationGlyph1;

    @FXML
    private Label validationGlyph2;

    @FXML
    private Label validationGlyph3;

    @FXML
    private Label validationText1;

    @FXML
    private Label validationText2;

    @FXML
    private Label validationText3;

    @FXML
    private HBox dragChromeLeft;

    @FXML
    private Label connectionLabel;

    @FXML
    private Button maximizeButton;

    @FXML
    private TextField userField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private CheckBox rememberCheck;

    @FXML
    private HBox errorBox;

    @FXML
    private Label errorMessageLabel;

    @FXML
    private Button loginSubmitButton;

    private Stage stage;
    private double dragStartX;
    private double dragStartY;

    private final RotateTransition glyphSpinner = new RotateTransition(Duration.millis(760));
    private Label spinningGlyph;

    private final AuthenticationService authenticationService = new AuthenticationService();
    private final CertificateService certificateService = new CertificateService();
    private volatile boolean serviceOnline;

    void setStage(Stage stage) {
        this.stage = stage;
    }

    /** Arranque con token en parametria: oculta login y valida en segundo plano. */
    public void startParametriaTokenEntry(InvokeConfig config) {
        Platform.runLater(this::showParametriaTokenValidationOverlay);

        Thread worker = new Thread(() -> {
            TokenValidationResult result = authenticationService.validateParametriaToken(config);
            Platform.runLater(() -> handleParametriaTokenValidationResult(config, result));
        }, "firmador-parametria-token");
        worker.setDaemon(true);
        worker.start();
    }

    private void handleParametriaTokenValidationResult(InvokeConfig config, TokenValidationResult result) {
        if (result != null && result.success()) {
            authenticationService.applyParametriaSession(config);
            System.out.println("[LoginController] Token parametria valido: " + result.message());
            validationText1.setText("Sesion validada");
            beginServiceValidationIntro("Sesion validada");
            return;
        }
        hideValidationVisualState();
        validationOverlay.setVisible(false);
        validationOverlay.setManaged(false);
        validationOverlay.setMouseTransparent(true);
        mainShell.setEffect(null);
        showLoginForm();
        showError(result != null ? result.message() : "Token invalido o expirado.");
    }

    private void showParametriaTokenValidationOverlay() {
        hideLoginForm();
        hideValidationVisualState();
        resetTaskRowUi(validationRow1, validationGlyph1, validationText1);
        resetTaskRowUi(validationRow2, validationGlyph2, validationText2);
        resetTaskRowUi(validationRow3, validationGlyph3, validationText3);
        validationText1.setText("Validando sesion...");
        validationText2.setText("Cargando certificados locales...");
        validationText3.setText("Inicializando aplicacion...");
        validationProgressBar.setProgress(0.1);
        validationOverlay.setVisible(true);
        validationOverlay.setManaged(true);
        validationOverlay.setMouseTransparent(false);
        mainShell.setEffect(new GaussianBlur(14));
        setGlyphWorking(validationGlyph1);
        setRowActive(validationRow1, validationText1);
        setRowPending(validationRow2, validationText2);
        setRowPending(validationRow3, validationText3);
    }

    private void hideLoginForm() {
        if (loginFormRoot != null) {
            loginFormRoot.setVisible(false);
            loginFormRoot.setManaged(false);
        }
    }

    private void showLoginForm() {
        if (loginFormRoot != null) {
            loginFormRoot.setVisible(true);
            loginFormRoot.setManaged(true);
        }
    }

    @FXML
    private void initialize() {
        glyphSpinner.setCycleCount(RotateTransition.INDEFINITE);
        glyphSpinner.setInterpolator(Interpolator.LINEAR);
        glyphSpinner.setByAngle(360);
        /* StackPane estira hijos al alto de la ventana; el modal debe ceñirse al contenido. */
        validationModalRoot.setMaxHeight(Region.USE_PREF_SIZE);
        validationModalRoot.setMaxWidth(Region.USE_PREF_SIZE);
        configureWindowDrag();
        restoreRememberMe();
        runConnectivityProbe();
    }

    /** Restaura usuario guardado si «Recordarme» estaba activo en el ultimo acceso. */
    private void restoreRememberMe() {
        LoginRememberMeStore.getSavedUsername().ifPresent(username -> {
            userField.setText(username);
            rememberCheck.setSelected(true);
        });
    }

    /** Guarda o borra el usuario segun el estado del checkbox al iniciar sesion. */
    private void persistRememberMe(String username) {
        if (rememberCheck.isSelected()) {
            LoginRememberMeStore.save(username);
        } else {
            LoginRememberMeStore.clear();
        }
    }

    private void configureWindowDrag() {
        dragChromeLeft.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (!stage.isMaximized()) {
                dragStartX = e.getSceneX();
                dragStartY = e.getSceneY();
            }
        });
        dragChromeLeft.addEventFilter(MouseEvent.MOUSE_DRAGGED, e -> {
            if (!stage.isMaximized()) {
                stage.setX(e.getScreenX() - dragStartX);
                stage.setY(e.getScreenY() - dragStartY);
            }
        });
        dragChromeLeft.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
            if (e.getClickCount() == 2) {
                onMaximize();
            }
        });
    }

    private void runConnectivityProbe() {
        setConnectionChipClass("connection-pill-checking");
        connectionLabel.setText("Comprobando conexión…");

        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                return authenticationService.isServiceReachable();
            }
        };
        task.setOnSucceeded(ev -> applyConnectivityResult(Boolean.TRUE.equals(task.getValue())));
        task.setOnFailed(ev -> applyConnectivityOffline());
        Thread t = new Thread(task, "firmador-connectivity");
        t.setDaemon(true);
        t.start();
    }

    private void applyConnectivityResult(boolean online) {
        serviceOnline = online;
        Platform.runLater(() -> {
            if (online) {
                setConnectionChipClass("connection-pill-online");
                connectionLabel.setText("Servicio disponible");
            } else {
                applyConnectivityOffline();
            }
        });
    }

    private void applyConnectivityOffline() {
        serviceOnline = false;
        Platform.runLater(() -> {
            setConnectionChipClass("connection-pill-offline");
            connectionLabel.setText("Sin conexión al servicio");
        });
    }

    private void setConnectionChipClass(String stateClass) {
        connectionLabel.getStyleClass().removeAll(
                "connection-pill-checking", "connection-pill-online", "connection-pill-offline");
        if (!connectionLabel.getStyleClass().contains("connection-pill")) {
            connectionLabel.getStyleClass().add("connection-pill");
        }
        connectionLabel.getStyleClass().add(stateClass);
    }

    @FXML
    private void onMinimize() {
        stage.setIconified(true);
    }

    @FXML
    private void onMaximize() {
        boolean next = !stage.isMaximized();
        stage.setMaximized(next);
        maximizeButton.setText(next ? "❐" : "□");
    }

    @FXML
    private void onClose() {
        if (stage != null) {
            stage.close();
        } else {
            Platform.exit();
        }
    }

    @FXML
    private void onOpenSettings() {
        InstitutionalDialog.info(
                stage,
                "Configuración",
                "La configuración del cliente se integrará en una próxima versión.");
    }

    @FXML
    private void onHelp() {
        InstitutionalDialog.info(
                stage,
                "Ayuda",
                "Para asistencia use el enlace «Soporte técnico» del pie de ventana.");
    }

    @FXML
    private void onLogin() {
        String u = userField.getText() != null ? userField.getText().trim() : "";
        String p = passwordField.getText() != null ? passwordField.getText() : "";
        if (u.isEmpty() || p.isEmpty()) {
            hideValidationVisualState();
            showError("Complete usuario y contraseña.");
            return;
        }
        if (!serviceOnline) {
            hideValidationVisualState();
            showError("Sin conexion al servicio. Inicie el API en localhost:8080 e intente de nuevo.");
            return;
        }
        errorBox.setVisible(false);
        errorBox.setManaged(false);
        hideValidationVisualState();
        loginSubmitButton.setDisable(true);

        Thread loginThread = new Thread(() -> {
            try {
                LoginResult result = authenticationService.login(u, p);
                Platform.runLater(() -> handleLoginResult(result));
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    loginSubmitButton.setDisable(false);
                    String detail = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
                    showError("Error al iniciar sesion: " + detail);
                    passwordField.clear();
                });
            }
        }, "firmador-auth-login");
        loginThread.setDaemon(true);
        loginThread.start();
    }

    private void handleLoginResult(LoginResult result) {
        if (result == null || !result.success()) {
            loginSubmitButton.setDisable(false);
            showError(result != null ? result.message() : "No se pudo iniciar sesion.");
            passwordField.clear();
            return;
        }
        String username = userField.getText() != null ? userField.getText().trim() : "";
        persistRememberMe(username);
        beginServiceValidationIntro();
    }

    private void showError(String message) {
        errorMessageLabel.setText(message);
        errorBox.setVisible(true);
        errorBox.setManaged(true);
    }

    @FXML
    private void onFooterServiceStatus() {
        InstitutionalDialog.info(
                stage,
                "Estado del servicio",
                "El panel de estado se conectará al endpoint corporativo cuando esté disponible.");
    }

    @FXML
    private void onFooterSupport() {
        InstitutionalDialog.info(stage, "Soporte técnico", "Consulte los canales oficiales de su organización.");
    }

    /** Restablece animacion y barra del overlay de validacion. */
    private void hideValidationVisualState() {
        glyphSpinner.stop();
        spinningGlyph = null;
        if (validationProgressBar != null) {
            validationProgressBar.setProgress(0);
        }
    }

    private void beginServiceValidationIntro() {
        beginServiceValidationIntro(null);
    }

    private void beginServiceValidationIntro(String completedStep1Text) {
        hideValidationVisualState();
        hideLoginForm();
        loginSubmitButton.setDisable(true);

        validationOverlay.setVisible(true);
        validationOverlay.setManaged(true);
        validationOverlay.setMouseTransparent(false);
        mainShell.setEffect(new GaussianBlur(14));

        resetTaskRowUi(validationRow1, validationGlyph1, validationText1);
        resetTaskRowUi(validationRow2, validationGlyph2, validationText2);
        resetTaskRowUi(validationRow3, validationGlyph3, validationText3);
        validationText1.setText("Validando librerias criptograficas...");
        validationText2.setText("Cargando certificados locales...");
        validationText3.setText("Inicializando sesion...");
        validationProgressBar.setProgress(0.1);
        setRowPending(validationRow2, validationText2);
        setRowPending(validationRow3, validationText3);

        if (completedStep1Text != null && !completedStep1Text.isBlank()) {
            validationText1.setText(completedStep1Text);
            setGlyphDone(validationGlyph1);
            setRowDone(validationRow1, validationText1);
            validationProgressBar.setProgress(0.25);
            runCertificateLoadingStep();
            return;
        }

        setGlyphWorking(validationGlyph1);
        setRowActive(validationRow1, validationText1);

        Thread cryptoThread =
                new Thread(
                        () -> {
                            boolean cryptoReady = verifyCryptoLibraries();
                            Platform.runLater(
                                    () -> {
                                        if (!cryptoReady) {
                                            abortValidationIntro(
                                                    "No se pudieron validar las librerias criptograficas.");
                                            return;
                                        }
                                        validationText1.setText("Librerias criptograficas validadas");
                                        setGlyphDone(validationGlyph1);
                                        setRowDone(validationRow1, validationText1);
                                        validationProgressBar.setProgress(0.25);
                                        runCertificateLoadingStep();
                                    });
                        },
                        "firmador-crypto-check");
        cryptoThread.setDaemon(true);
        cryptoThread.start();
    }

    private boolean verifyCryptoLibraries() {
        try {
            new ServicioCertificado();
            KeyStore keyStore = KeyStore.getInstance("Windows-MY");
            keyStore.load(null, null);
            return true;
        } catch (Exception ex) {
            System.err.println("[LoginController] Error validando librerias: " + ex.getMessage());
            return false;
        }
    }

    private void runCertificateLoadingStep() {
        setGlyphWorking(validationGlyph2);
        setRowActive(validationRow2, validationText2);
        validationText2.setText("Cargando certificados locales...");
        validationProgressBar.setProgress(0.4);

        Thread certThread =
                new Thread(
                        () -> {
                            CertificateListResult result = certificateService.listInstalled();
                            CertificateCatalog.set(result);
                            Platform.runLater(() -> completeCertificateLoadingStep(result));
                        },
                        "firmador-login-certificates");
        certThread.setDaemon(true);
        certThread.start();
    }

    private void completeCertificateLoadingStep(CertificateListResult result) {
        if (result == null || !result.success()) {
            String detail = result != null ? result.message() : "Error desconocido";
            validationText2.setText("Error al cargar certificados: " + detail);
        } else if (result.noCertificatesFound()) {
            validationText2.setText("Sin certificados de firma en este equipo");
        } else {
            int count = result.certificates().size();
            validationText2.setText(
                    count + (count == 1 ? " certificado local detectado" : " certificados locales detectados"));
        }

        setGlyphDone(validationGlyph2);
        setRowDone(validationRow2, validationText2);
        validationProgressBar.setProgress(0.72);
        runSessionInitializationStep();
    }

    private void runSessionInitializationStep() {
        setGlyphWorking(validationGlyph3);
        setRowActive(validationRow3, validationText3);
        validationText3.setText("Inicializando sesion...");

        PauseTransition pause = new PauseTransition(Duration.millis(450));
        pause.setOnFinished(
                event -> {
                    validationText3.setText("Sesion lista");
                    setGlyphDone(validationGlyph3);
                    setRowDone(validationRow3, validationText3);
                    validationProgressBar.setProgress(1.0);

                    PauseTransition finishPause = new PauseTransition(Duration.millis(420));
                    finishPause.setOnFinished(ev -> finishValidationAndReturnToInteraction());
                    finishPause.play();
                });
        pause.play();
    }

    private void abortValidationIntro(String message) {
        CertificateCatalog.clear();
        hideValidationVisualState();
        validationOverlay.setVisible(false);
        validationOverlay.setManaged(false);
        validationOverlay.setMouseTransparent(true);
        mainShell.setEffect(null);
        showLoginForm();
        loginSubmitButton.setDisable(false);
        showError(message);
    }

    private void resetTaskRowUi(HBox row, Label glyph, Label text) {
        row.getStyleClass().removeAll("validation-task-row-pending",
                "validation-task-row-active", "validation-task-row-done");
        row.getStyleClass().add("validation-task-row-pending");
        glyph.getStyleClass().removeAll(
                "validation-glyph-pending", "validation-glyph-active", "validation-glyph-done");
        glyph.setText(GLYPH_PENDING);
        glyph.getStyleClass().add("validation-glyph-pending");
        glyph.setRotate(0);

        text.getStyleClass().removeAll("validation-task-label-pending",
                "validation-task-label-active", "validation-task-label-done");
        text.getStyleClass().add("validation-task-label-pending");
    }

    private void setRowPending(HBox row, Label textLabel) {
        row.getStyleClass().removeAll("validation-task-row-active", "validation-task-row-done");
        row.getStyleClass().add("validation-task-row-pending");

        textLabel.getStyleClass().removeAll(
                "validation-task-label-active", "validation-task-label-done");
        textLabel.getStyleClass().add("validation-task-label-pending");
    }

    private void setRowActive(HBox row, Label textLabel) {
        row.getStyleClass().removeAll("validation-task-row-pending", "validation-task-row-done");
        row.getStyleClass().add("validation-task-row-active");

        textLabel.getStyleClass().removeAll(
                "validation-task-label-pending", "validation-task-label-done");
        textLabel.getStyleClass().add("validation-task-label-active");
    }

    private void setRowDone(HBox row, Label textLabel) {
        row.getStyleClass().removeAll("validation-task-row-pending", "validation-task-row-active");
        row.getStyleClass().add("validation-task-row-done");

        textLabel.getStyleClass().removeAll(
                "validation-task-label-active", "validation-task-label-pending");
        textLabel.getStyleClass().add("validation-task-label-done");
    }

    private void setGlyphWorking(Label glyph) {
        if (spinningGlyph != null && spinningGlyph != glyph) {
            glyphSpinner.stop();
            spinningGlyph.setRotate(0);
        }
        spinningGlyph = glyph;
        glyph.getStyleClass().removeAll("validation-glyph-pending", "validation-glyph-done");
        glyph.getStyleClass().add("validation-glyph-active");
        glyph.setText(GLYPH_WORKING);
        glyphSpinner.stop();
        glyph.setRotate(0);
        glyphSpinner.setNode(glyph);
        glyphSpinner.playFromStart();
    }

    private void setGlyphDone(Label glyph) {
        glyphSpinner.stop();
        if (spinningGlyph == glyph) {
            spinningGlyph = null;
        }
        glyph.setRotate(0);
        glyph.getStyleClass().removeAll("validation-glyph-pending", "validation-glyph-active");
        glyph.getStyleClass().add("validation-glyph-done");
        glyph.setText(GLYPH_DONE);
    }

    private void finishValidationAndReturnToInteraction() {
        glyphSpinner.stop();
        validationOverlay.setVisible(false);
        validationOverlay.setManaged(false);
        validationOverlay.setMouseTransparent(true);
        mainShell.setEffect(null);
        loginSubmitButton.setDisable(false);
        hideValidationVisualState();

        try {
            MainWorkspaceOpener.show(stage);
        } catch (IOException ex) {
            InstitutionalDialog.error(
                    stage,
                    "No se pudo cargar el escritorio",
                    ex.getMessage() != null ? ex.getMessage() : ex.toString());
        }
    }
}
