package com.osinergmin.firma.desktop.ui.main;

import com.osinergmin.firma.desktop.config.ClientConfiguration;
import com.osinergmin.firma.desktop.config.SigningAdminConfig;
import com.osinergmin.firma.desktop.config.SigningAdminConfigStore;
import com.osinergmin.firma.desktop.core.auth.AdminAccessPolicy;
import com.osinergmin.firma.desktop.core.auth.AuthSession;
import com.osinergmin.firma.desktop.core.certificate.CertificateCatalog;
import com.osinergmin.firma.desktop.core.document.SigningDocumentContext;
import com.osinergmin.firma.desktop.core.signing.SigningSessionContext;
import com.osinergmin.firma.desktop.integration.LaunchContext;
import com.osinergmin.firma.desktop.integration.invoke.InvokeConfig;
import com.osinergmin.firma.desktop.integration.invoke.SigningParam;
import com.osinergmin.firma.desktop.pdf.PdfViewerConfig;
import com.osinergmin.firma.desktop.service.audit.BitacoraEventPublisher;
import com.osinergmin.firma.desktop.service.auth.AuthenticationService;
import com.osinergmin.firma.desktop.service.certificate.CertificateListResult;
import com.osinergmin.firma.desktop.service.certificate.CertificateService;
import com.osinergmin.firma.desktop.service.certificate.InstalledCertificate;
import com.osinergmin.firma.desktop.service.document.AlfrescoContentClient;
import com.osinergmin.firma.desktop.service.document.AlfrescoContentResult;
import com.osinergmin.firma.desktop.service.document.AlfrescoDestinationResolver;
import com.osinergmin.firma.desktop.service.document.AlfrescoUploadClient;
import com.osinergmin.firma.desktop.service.document.AlfrescoUploadFileNameResolver;
import com.osinergmin.firma.desktop.service.document.AlfrescoUploadResult;
import com.osinergmin.firma.desktop.service.document.DocumentUrlClient;
import com.osinergmin.firma.desktop.service.document.DocumentUrlResult;
import com.osinergmin.firma.desktop.service.signing.DocumentSigningService;
import com.osinergmin.firma.desktop.service.signing.SigningDocumentNaming;
import com.osinergmin.firma.desktop.service.signing.SignaturePlacement;
import com.osinergmin.firma.desktop.service.signing.SigningCallbackClient;
import com.osinergmin.firma.desktop.service.signing.SigningResult;
import com.osinergmin.firma.desktop.service.verification.PadesVerificationService;
import com.osinergmin.firma.desktop.service.verification.VerificationResult;
import com.osinergmin.firma.desktop.ui.dialog.InstitutionalDialog;
import com.osinergmin.firma.desktop.ui.login.LoginWindowOpener;
import com.osinergmin.firma.desktop.ui.viewer.DocumentViewerController;

import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.concurrent.Task;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import javafx.application.Platform;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import javafx.util.Duration;

/**
 * Escritorio institucional: selector de certificado → visor → proceso de firma.
 */
public class MainWorkspaceController {

    private static final String SIDEBAR_NAV_ACTIVE_CLASS = "nav-sidebar-row-active";
    private static final double INTERFAZ_MINIMA_WIDTH = 520;
    private static final double INTERFAZ_MINIMA_HEIGHT = 220;

    @FXML
    private StackPane sidebarNavFirmar;

    // @FXML private StackPane sidebarNavHistorial; // menu comentado en MainWorkspace.fxml

    @FXML
    private StackPane sidebarNavAdministracion;

    @FXML
    private VBox mainSidebar;

    private List<StackPane> sidebarPrimaryNavRows;

    @FXML
    private HBox dragChromeLeft;

    @FXML
    private Button maximizeButton;

    @FXML
    private VBox certificateSelectorCard;

    @FXML
    private VBox documentViewerCard;

    @FXML
    private DocumentViewerController pdfViewerController;

    @FXML
    private TableView<CertificateTableRow> certificateTable;

    @FXML
    private VBox signingCard;

    @FXML
    private Label signingModalTitle;

    @FXML
    private HBox signingStepper;

    @FXML
    private HBox signingConsoleHeader;

    @FXML
    private VBox signingConsoleSection;

    @FXML
    private HBox signingFooter;

    @FXML
    private Label execSpinGlyph;

    @FXML
    private Label signingDocSubtitle;

    @FXML
    private Label progressCaptionLabel;

    @FXML
    private Label etaLabel;

    @FXML
    private ProgressBar signingProgressBar;

    @FXML
    private TextArea signingLogArea;

    @FXML
    private VBox signingSuccessCard;

    @FXML
    private Label successMainSubLabel;

    @FXML
    private Label successVerificationStatusValue;

    @FXML
    private Label successIntegrityValue;

    @FXML
    private Label successSignaturesValue;

    @FXML
    private Label successVerificationDateValue;

    @FXML
    private Button successOpenDocumentButton;

    @FXML
    private VBox adminConfigCard;

    @FXML
    private TextField adminTslUrlField;

    @FXML
    private CheckBox adminVerifyTslCheck;

    @FXML
    private TextField adminTsaUrlField;

    @FXML
    private TextField adminTsaUserField;

    @FXML
    private PasswordField adminTsaPasswordField;

    @FXML
    private ComboBox<String> adminSignatureLevelCombo;

    @FXML
    private TextField adminAlgorithmField;

    private Stage stage;
    private double dragStartX;
    private double dragStartY;

    /** Nombre mostrado en el paso de firma (coincide con el visor). */
    private String signingDocumentTitle = "documento_prueba.pdf";

    private final RotateTransition execSpinAnimation = new RotateTransition(Duration.millis(900));
    private final CertificateService certificateService = new CertificateService();
    private final AlfrescoContentClient alfrescoContentClient = new AlfrescoContentClient();
    private final AlfrescoUploadClient alfrescoUploadClient = new AlfrescoUploadClient();
    private final DocumentSigningService documentSigningService = new DocumentSigningService();
    private final PadesVerificationService verificationService = new PadesVerificationService();
    private boolean certificateLoadAlertShown;
    private Task<SigningResult> signingTask;
    private VerificationResult lastVerificationResult;
    private WorkspaceView workspaceViewBeforeAdmin = WorkspaceView.CERTIFICATE_SELECTOR;

    void setStage(Stage stage) {
        this.stage = stage;
    }

    @FXML
    private void initialize() {
        execSpinAnimation.setNode(execSpinGlyph);
        execSpinAnimation.setCycleCount(RotateTransition.INDEFINITE);
        execSpinAnimation.setInterpolator(Interpolator.LINEAR);
        execSpinAnimation.setByAngle(360);

        configureCertificateTable();
        configureAdminForm();
        loadInstalledCertificates();
        showCertificateSelectorOnly();

        sidebarPrimaryNavRows = List.of(sidebarNavFirmar, sidebarNavAdministracion);
        applySidebarNavHighlight(sidebarNavFirmar);

        signingProgressBar.setProgress(0);
        progressCaptionLabel.setText("Progreso global: 0 %");
        etaLabel.setText("Listo para firmar");
        if (isInterfazMinimaActive()) {
            applyInterfazMinimaSigningCardLayout();
        }
    }

    /** Llamar después de asignar el {@link Stage} (FXML {@code initialize()} corre antes). */
    void bindStageDependentBehavior() {
        BitacoraEventPublisher.setUiLogSink(line -> Platform.runLater(() -> appendSigningLogLine(line)));
        logBitacoraStartupDiagnostics();
        configureWindowDrag();
        refreshAdminNavVisibility();
        if (isInterfazMinimaActive()) {
            applyInterfazMinimaWorkspaceLayout();
            System.out.println(
                    "[MainWorkspace] interfazMinima activo: UI compacta y cierre automatico al terminar.");
        }
        if (pdfViewerController != null) {
            pdfViewerController.setHostStage(stage);
            pdfViewerController.setOnPlacementCapturedBeforeContinue(this::storeSignaturePlacementFromViewer);
            pdfViewerController.setOnContinueSigning(this::showSigningProcessOnly);
            pdfViewerController.setOnBackToSummary(this::showSigningSuccessScreenFromViewer);
        }
    }

    private void logBitacoraStartupDiagnostics() {
        System.out.println(
                "[Bitacora] enabled="
                        + ClientConfiguration.isBitacoraEnabled()
                        + " url="
                        + ClientConfiguration.getBitacoraEventoUrl());
        InvokeConfig invokeConfig = LaunchContext.get().invokeConfig();
        if (invokeConfig == null) {
            return;
        }
        System.out.println(
                "[Bitacora] idSolicitud="
                        + (invokeConfig.getIdSolicitud().isBlank()
                                ? "(vacio)"
                                : invokeConfig.getIdSolicitud()));
        boolean hasSessionToken = AuthSession.getAccessToken().isPresent();
        boolean hasParametriaToken = !invokeConfig.getToken().isBlank();
        if (!hasSessionToken && !hasParametriaToken) {
            System.err.println(
                    "[Bitacora] AVISO: sin token JWT. Defina variable FIRMA_JWT_TOKEN, "
                            + "-Dfirma.jwt.token o inicie sesion en el firmador antes de firmar.");
            appendSigningLogLine(
                    "AVISO Bitacora: sin JWT (FIRMA_JWT_TOKEN / login). Los eventos no se enviaran al servicio.");
        } else {
            System.out.println(
                    "[Bitacora] token disponible desde "
                            + (hasSessionToken ? "AuthSession" : "parametria"));
        }
    }

    private void configureCertificateTable() {
        certificateTable.setFixedCellSize(36);
        certificateTable.setPlaceholder(new Label("Cargando certificados instalados…"));

        TableColumn<CertificateTableRow, String> tipoCol = new TableColumn<>("TIPO");
        tipoCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getTipo()));
        tipoCol.setPrefWidth(112);

        TableColumn<CertificateTableRow, String> titularCol = new TableColumn<>("Titular");
        titularCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getTitular()));
        titularCol.setPrefWidth(168);

        TableColumn<CertificateTableRow, String> emisorCol = new TableColumn<>("Entidad emisora");
        emisorCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getEmisor()));
        emisorCol.setPrefWidth(188);

        TableColumn<CertificateTableRow, String> expCol = new TableColumn<>("Expiración");
        expCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getExpiracion()));
        expCol.setPrefWidth(96);

        TableColumn<CertificateTableRow, String> estadoCol = new TableColumn<>("Estado");
        estadoCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getEstadoLabel()));
        estadoCol.setCellFactory(
                col -> new TableCell<>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setGraphic(null);
                            return;
                        }
                        Label pill = new Label(item);
                        pill.getStyleClass().setAll("cert-status-pill");
                        if ("EXPIRADO".equals(item)) {
                            pill.getStyleClass().add("cert-status-pill-expired");
                        } else {
                            pill.getStyleClass().add("cert-status-pill-verified");
                        }
                        setGraphic(pill);
                    }
                });
        estadoCol.setPrefWidth(108);

        certificateTable.getColumns().setAll(tipoCol, titularCol, emisorCol, expCol, estadoCol);
        certificateTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        certificateTable.setRowFactory(
                tv -> {
                    TableRow<CertificateTableRow> row = new TableRow<>();
                    row.itemProperty()
                            .addListener(
                                    (obs, previous, rowItem) -> {
                                        row.getStyleClass().remove("cert-row-expired");
                                        if (rowItem != null && rowItem.isExpired()) {
                                            row.getStyleClass().add("cert-row-expired");
                                        }
                                    });
                    return row;
                });
    }

    private void loadInstalledCertificates() {
        certificateTable.setItems(FXCollections.observableArrayList());
        certificateTable.setPlaceholder(new Label("Cargando certificados instalados…"));
        certificateLoadAlertShown = false;

        CertificateCatalog.getIfLoaded()
                .ifPresentOrElse(
                        result -> applyCertificateListResult(result),
                        () -> {
                            Thread loader =
                                    new Thread(
                                            () -> {
                                                CertificateListResult result = certificateService.listInstalled();
                                                CertificateCatalog.set(result);
                                                Platform.runLater(() -> applyCertificateListResult(result));
                                            },
                                            "firmador-certificates");
                            loader.setDaemon(true);
                            loader.start();
                        });
    }

    private void applyCertificateListResult(CertificateListResult result) {
        if (!result.success()) {
            certificateTable.setItems(FXCollections.observableArrayList());
            certificateTable.setPlaceholder(new Label("No se pudieron cargar los certificados."));
            showCertificateLoadAlert(
                    InstitutionalDialog.Kind.ERROR,
                    "Error al cargar certificados",
                    result.message());
            return;
        }

        if (result.noCertificatesFound()) {
            certificateTable.setItems(FXCollections.observableArrayList());
            certificateTable.setPlaceholder(new Label("No hay certificados de firma en este equipo."));
            showCertificateLoadAlert(
                    InstitutionalDialog.Kind.INFO,
                    "Sin certificados",
                    result.message());
            return;
        }

        ObservableList<CertificateTableRow> rows = FXCollections.observableArrayList();
        for (InstalledCertificate certificate : result.certificates()) {
            rows.add(CertificateTableRow.fromInstalled(certificate));
        }
        certificateTable.setItems(rows);
        certificateTable.setPlaceholder(new Label("No hay certificados de firma en este equipo."));
        if (!isInterfazMinimaActive()) {
            selectCertificateFromParametria();
        }
    }

    private void selectCertificateFromParametria() {
        String credential = LaunchContext.get().invokeConfig().getCredential();
        if (credential.isBlank()) {
            selectFirstValidCertificate();
            return;
        }
        String wanted = credential.trim().toLowerCase();
        for (CertificateTableRow row : certificateTable.getItems()) {
            if (row == null || row.isExpired()) {
                continue;
            }
            String alias = row.getCertificate().libraryAlias();
            if (alias != null
                    && (alias.equalsIgnoreCase(credential.trim())
                            || alias.toLowerCase().contains(wanted))) {
                certificateTable.getSelectionModel().select(row);
                return;
            }
        }
        selectFirstValidCertificate();
    }

    private void selectFirstValidCertificate() {
        for (CertificateTableRow row : certificateTable.getItems()) {
            if (row != null && !row.isExpired()) {
                certificateTable.getSelectionModel().select(row);
                return;
            }
        }
        if (!certificateTable.getItems().isEmpty()) {
            certificateTable.getSelectionModel().selectFirst();
        }
    }

    private void showCertificateLoadAlert(InstitutionalDialog.Kind kind, String header, String content) {
        if (certificateLoadAlertShown) {
            return;
        }
        certificateLoadAlertShown = true;
        InstitutionalDialog.show(kind, stage, header, content);
    }

    private void applySidebarNavHighlight(StackPane selectedRow) {
        for (StackPane row : sidebarPrimaryNavRows) {
            row.getStyleClass().remove(SIDEBAR_NAV_ACTIVE_CLASS);
        }
        if (selectedRow != null && !selectedRow.getStyleClass().contains(SIDEBAR_NAV_ACTIVE_CLASS)) {
            selectedRow.getStyleClass().add(SIDEBAR_NAV_ACTIVE_CLASS);
        }
    }

    private void showCertificateSelectorOnly() {
        stopSigningTask();
        execSpinAnimation.stop();
        SigningDocumentContext.clear();
        SigningSessionContext.clear();
        if (pdfViewerController != null) {
            pdfViewerController.release();
        }
        hideAllContentCards();
        certificateSelectorCard.setVisible(true);
        certificateSelectorCard.setManaged(true);
    }

    private void showDocumentViewerOnly() {
        InvokeConfig invokeConfig = LaunchContext.get().invokeConfig();
        if (!invokeConfig.canUsePdfViewer()) {
            if (invokeConfig.shouldSkipDocumentViewer()) {
                System.out.println("[MainWorkspace] Visor PDF omitido (sinVisor, soloFirma o formato no PAdES).");
            }
            showSigningProcessOnly();
            return;
        }
        stopSigningTask();
        execSpinAnimation.stop();
        hideAllContentCards();
        documentViewerCard.setVisible(true);
        documentViewerCard.setManaged(true);
    }

    private void showSigningProcessOnly() {
        hideAllContentCards();
        signingCard.setVisible(true);
        signingCard.setManaged(true);

        if (isInterfazMinimaActive()) {
            applyInterfazMinimaSigningCardLayout();
            signingDocSubtitle.setText(signingDocumentTitle);
        } else {
            signingDocSubtitle.setText("Documento: " + signingDocumentTitle);
        }

        startRealSigning();
        execSpinAnimation.playFromStart();
        if (isInterfazMinimaActive()) {
            Platform.runLater(this::applyInterfazMinimaSigningCardLayout);
        }
    }

    private void showSigningProcessCardOnly() {
        hideAllContentCards();
        signingCard.setVisible(true);
        signingCard.setManaged(true);
    }

    private void showSuccessScreenOnly() {
        hideAllContentCards();
        signingSuccessCard.setVisible(true);
        signingSuccessCard.setManaged(true);
    }

    private void storeSignaturePlacementFromViewer(Optional<SignaturePlacement> placement) {
        SigningSessionContext.clearSignaturePlacement();
        InvokeConfig invokeConfig = LaunchContext.get().invokeConfig();
        if (!invokeConfig.isFirmaArrastrar()) {
            return;
        }
        placement.ifPresent(SigningSessionContext::setSignaturePlacement);
    }

    private void stopSigningTask() {
        if (signingTask != null) {
            signingTask.cancel();
            signingTask = null;
        }
    }

    private void startRealSigning() {
        stopSigningTask();

        Optional<InstalledCertificate> certificate = SigningSessionContext.getSelectedCertificate();
        Optional<PdfViewerConfig> document = SigningDocumentContext.getPreparedDocument();
        if (certificate.isEmpty()) {
            handleSigningFailure("No hay certificado seleccionado para firmar.");
            return;
        }
        if (document.isEmpty()) {
            handleSigningFailure("No hay documento preparado para firmar.");
            return;
        }

        InvokeConfig invokeConfig = LaunchContext.get().invokeConfig();
        if (isInterfazMinimaActive()) {
            signingProgressBar.setProgress(0.1);
            etaLabel.setText("Preparando firma…");
        } else {
            signingProgressBar.setProgress(0.1);
            progressCaptionLabel.setText("Progreso global: 10 %");
            String standard = invokeConfig.signingParam().standardFirma();
            etaLabel.setText("Ejecutando firma " + standard + "…");
            signingLogArea.setText("");
        }
        appendSigningLogLine("Iniciando motor de firma " + invokeConfig.signingParam().standardFirma() + " (firmaOsinergmin)…");
        appendSigningLogLine("Certificado: " + certificate.get().libraryAlias());
        Optional<SignaturePlacement> placement =
                invokeConfig.isFirmaArrastrar()
                        ? SigningSessionContext.getSignaturePlacement()
                        : Optional.empty();
        SigningParam signingParam = invokeConfig.signingParam();
        appendSigningLogLine(
                String.format(
                        "Parametria: formato=%d, tag=%s, modoPos=%s",
                        signingParam.signatureFormat(),
                        signingParam.effectiveTagMarker().isBlank()
                                ? "(vacio — revisar parametria.json)"
                                : signingParam.effectiveTagMarker(),
                        signingParam.hasPositioningMode()
                                ? signingParam.positioningMode()
                                : "(sin modo)"));
        Optional<String> tagError =
                documentSigningService.resolveTagPlacementOrError(document.get(), signingParam);
        if (tagError.isPresent()) {
            handleSigningFailure(tagError.get());
            return;
        }

        if (!invokeConfig.isFirmaArrastrar()) {
            if (signingParam.usesTagPlacement() || signingParam.isTagPositioningMode()) {
                appendSigningLogLine("Firma por marcador de texto (FirmaArrastrar=false).");
            } else {
                appendSigningLogLine("Posicion fija desde parametria (FirmaArrastrar=false).");
            }
        } else if (!invokeConfig.shouldSkipDocumentViewer()
                && placement.isEmpty()
                && invokeConfig.signingParam().applyImage()) {
            handleSigningFailure(
                    "Debe arrastrar el sello al PDF antes de firmar (FirmaArrastrar=true).");
            return;
        }
        appendSigningLogLine(documentSigningService.describePlacementForLog(invokeConfig, placement));

        InstalledCertificate selectedCertificate = certificate.get();
        PdfViewerConfig sourceDocument = document.get();
        Optional<SignaturePlacement> signingPlacement = placement;
        try {
            SigningSessionContext.setOriginalDocumentBytes(sourceDocument.readDocumentBytes());
        } catch (IOException ex) {
            handleSigningFailure("No se pudo leer el documento original: " + ex.getMessage());
            return;
        }

        BitacoraEventPublisher.dispatchSignStart(invokeConfig, selectedCertificate);

        signingTask =
                new Task<>() {
                    @Override
                    protected SigningResult call() {
                        updateMessage("Preparando documento y parámetros de firma…");
                        return documentSigningService.sign(
                                sourceDocument,
                                selectedCertificate.libraryAlias(),
                                invokeConfig,
                                signingPlacement);
                    }
                };

        signingTask.messageProperty()
                .addListener(
                        (obs, previous, message) -> {
                            if (message != null && !message.isBlank()) {
                                Platform.runLater(() -> appendSigningLogLine(message));
                            }
                        });

        signingTask.setOnRunning(
                event -> {
                    signingProgressBar.setProgress(0.35);
                    if (isInterfazMinimaActive()) {
                        etaLabel.setText("Firmando documento…");
                    } else {
                        progressCaptionLabel.setText("Progreso global: 35 %");
                    }
                });

        signingTask.setOnSucceeded(
                event ->
                        Platform.runLater(
                                () -> {
                                    SigningResult result = signingTask.getValue();
                                    signingTask = null;
                                    handleSigningResult(result);
                                }));

        signingTask.setOnFailed(
                event ->
                        Platform.runLater(
                                () -> {
                                    Task<SigningResult> failedTask = signingTask;
                                    signingTask = null;
                                    Throwable ex = failedTask != null ? failedTask.getException() : null;
                                    String message =
                                            ex != null && ex.getMessage() != null
                                                    ? ex.getMessage()
                                                    : "Error inesperado durante la firma.";
                                    handleSigningFailure(message);
                                }));

        Thread worker = new Thread(signingTask, "firmador-documento");
        worker.setDaemon(true);
        worker.start();
    }

    private void handleSigningResult(SigningResult result) {
        execSpinAnimation.stop();
        InvokeConfig invokeConfig = LaunchContext.get().invokeConfig();
        if (result != null && result.success()) {
            PdfViewerConfig signed = result.signedDocument();
            SigningDocumentContext.commitSignedDocument(signed);
            signingDocumentTitle = signed.displayFileName();
            signingProgressBar.setProgress(1.0);
            if (isInterfazMinimaActive()) {
                etaLabel.setText("Completado");
            } else {
                progressCaptionLabel.setText("Progreso global: 100 %");
                etaLabel.setText("Completado");
            }
            appendSigningLogLine(result.message());
            appendSigningLogLine("Documento firmado listo para descarga: " + signed.displayFileName());
            lastVerificationResult =
                    verificationService.verify(
                            signed,
                            SigningSessionContext.getOriginalDocumentBytes(),
                            invokeConfig.signingParam());
            populateSuccessVerificationUi(lastVerificationResult);
            appendSigningLogLine(lastVerificationResult.logLine());
            InstalledCertificate certificate = SigningSessionContext.getSelectedCertificate().orElse(null);
            byte[] originalBytes = SigningSessionContext.getOriginalDocumentBytes().orElse(new byte[0]);
            BitacoraEventPublisher.dispatchSignSuccess(
                    invokeConfig, certificate, signed, originalBytes, result.message());
            if (invokeConfig != null && invokeConfig.isCerrarAlTerminar()) {
                appendSigningLogLine("cerrarAlTerminar activo: finalizando tareas y cerrando aplicacion...");
                Thread exitThread =
                        new Thread(
                                () -> {
                                    runSigningCallbackIfConfigured(invokeConfig, true, null, signed);
                                    runAlfrescoUploadIfConfigured(invokeConfig, signed);
                                    Platform.runLater(this::closePrimaryWindow);
                                },
                                "firmador-auto-close");
                exitThread.setDaemon(false);
                exitThread.start();
            } else {
                dispatchSigningCallbackAsync(invokeConfig, true, null, signed);
                dispatchAlfrescoUploadAsync(invokeConfig, signed);
                showSigningSuccessScreen();
            }
            return;
        }

        String message = result != null ? result.message() : "No se obtuvo respuesta del motor de firma.";
        dispatchSigningCallbackAsync(invokeConfig, false, message, null);
        handleSigningFailure(message);
    }

    private void dispatchSigningCallbackAsync(
            InvokeConfig invokeConfig, boolean success, String errorMessage, PdfViewerConfig signedDocument) {
        if (invokeConfig == null || !invokeConfig.hasCallbackUrl()) {
            return;
        }
        Thread callbackThread =
                new Thread(
                        () ->
                                runSigningCallbackIfConfigured(
                                        invokeConfig, success, errorMessage, signedDocument),
                        "firmador-signing-callback");
        callbackThread.setDaemon(true);
        callbackThread.start();
    }

    private void runSigningCallbackIfConfigured(
            InvokeConfig invokeConfig, boolean success, String errorMessage, PdfViewerConfig signedDocument) {
        if (invokeConfig == null || !invokeConfig.hasCallbackUrl()) {
            return;
        }
        byte[] originalBytes = SigningSessionContext.getOriginalDocumentBytes().orElse(new byte[0]);
        InstalledCertificate certificate = SigningSessionContext.getSelectedCertificate().orElse(null);
        Optional<String> callbackResult =
                SigningCallbackClient.postIfConfigured(
                        invokeConfig, success, errorMessage, originalBytes, signedDocument, certificate);
        callbackResult.ifPresent(line -> Platform.runLater(() -> appendSigningLogLine(line)));
    }

    private void dispatchAlfrescoUploadAsync(InvokeConfig invokeConfig, PdfViewerConfig signedDocument) {
        if (invokeConfig == null
                || signedDocument == null
                || !AlfrescoDestinationResolver.shouldUploadAfterSign(invokeConfig)) {
            return;
        }
        Thread uploadThread =
                new Thread(
                        () -> runAlfrescoUploadIfConfigured(invokeConfig, signedDocument),
                        "firmador-alfresco-upload");
        uploadThread.setDaemon(true);
        uploadThread.start();
    }

    private void runAlfrescoUploadIfConfigured(InvokeConfig invokeConfig, PdfViewerConfig signedDocument) {
        if (invokeConfig == null
                || signedDocument == null
                || !AlfrescoDestinationResolver.shouldUploadAfterSign(invokeConfig)) {
            return;
        }
        String nodeId =
                AlfrescoDestinationResolver.resolveUploadNodeId(invokeConfig).orElse("");
        if (nodeId.isBlank()) {
            System.out.println("[MainWorkspace] Alfresco upload omitido: nodeId destino vacio.");
            return;
        }
        String accessToken =
                AuthSession.getAccessToken()
                        .orElseGet(() -> invokeConfig.getToken().isBlank() ? "" : invokeConfig.getToken());
        if (accessToken.isBlank()) {
            System.err.println("[MainWorkspace] Alfresco upload cancelado: sin token Bearer.");
            appendSigningLogLine("AVISO: sin token para subir el documento firmado a Alfresco.");
            return;
        }

        Integer sourceVersion =
                invokeConfig.usesAlfrescoVersioning()
                        ? invokeConfig.getDocumentVersion().orElse(0)
                        : null;
        String uploadFileName = AlfrescoUploadFileNameResolver.resolve(invokeConfig, signedDocument);
        System.out.println(
                "[MainWorkspace] Iniciando subida Alfresco -> nodeId="
                        + nodeId
                        + " | fileName="
                        + uploadFileName
                        + (sourceVersion != null && sourceVersion > 0
                                ? " | sourceVersion=" + sourceVersion
                                : ""));
        appendSigningLogLine(
                "Subiendo documento firmado a Alfresco (" + nodeId + ") como " + uploadFileName + "...");
        try {
            AlfrescoUploadResult result =
                    alfrescoUploadClient.uploadSignedDocument(
                            nodeId,
                            accessToken,
                            uploadFileName,
                            signedDocument.readDocumentBytes(),
                            SigningDocumentNaming.guessMimeType(uploadFileName),
                            sourceVersion);
            Platform.runLater(
                    () -> {
                        if (result.success()) {
                            appendSigningLogLine("Alfresco: " + result.message());
                        } else {
                            appendSigningLogLine("AVISO Alfresco: " + result.message());
                        }
                    });
        } catch (IOException ex) {
            Platform.runLater(
                    () ->
                            appendSigningLogLine(
                                    "AVISO Alfresco: "
                                            + (ex.getMessage() != null
                                                    ? ex.getMessage()
                                                    : "Error al leer documento firmado.")));
        }
    }

    private void handleSigningFailure(String message) {
        execSpinAnimation.stop();
        InvokeConfig invokeConfig = LaunchContext.get().invokeConfig();
        InstalledCertificate certificate = SigningSessionContext.getSelectedCertificate().orElse(null);
        BitacoraEventPublisher.dispatchSignError(invokeConfig, certificate, message);
        signingProgressBar.setProgress(0);
        progressCaptionLabel.setText("Progreso global: 0 %");
        etaLabel.setText("Error en la firma");
        String resolved =
                message == null || message.isBlank()
                        ? "Ocurrió un error durante la firma. Revise la consola de depuración."
                        : message.trim();
        appendSigningLogLine("ERROR: " + resolved);
        InstitutionalDialog.error(
                stage,
                "Error al firmar",
                resolved);
    }

    private void showSigningSuccessScreen() {
        execSpinAnimation.stop();
        hideAllContentCards();
        signingSuccessCard.setVisible(true);
        signingSuccessCard.setManaged(true);
        updateSuccessOpenDocumentButtonVisibility();
    }

    /** El visor PDF solo aplica al resultado firmado; CAdES/XAdES solo descargan. */
    private void updateSuccessOpenDocumentButtonVisibility() {
        boolean showViewer =
                resolveSignedDocument().map(PdfViewerConfig::isPdf).orElse(false);
        if (successOpenDocumentButton != null) {
            successOpenDocumentButton.setVisible(showViewer);
            successOpenDocumentButton.setManaged(showViewer);
        }
    }

    private void populateSuccessVerificationUi(VerificationResult verification) {
        if (verification == null) {
            return;
        }
        if (successMainSubLabel != null) {
            successMainSubLabel.setText(verification.successSubtitle());
        }
        if (successVerificationStatusValue != null) {
            successVerificationStatusValue.setText(verification.estadoLabel());
        }
        if (successIntegrityValue != null) {
            successIntegrityValue.setText(verification.integridadLabel());
        }
        if (successSignaturesValue != null) {
            successSignaturesValue.setText(verification.firmasResumenLabel());
        }
        if (successVerificationDateValue != null) {
            successVerificationDateValue.setText(verification.fechaValidacionLabel());
        }
    }

    private void showSigningSuccessScreenFromViewer() {
        hideAllContentCards();
        signingSuccessCard.setVisible(true);
        signingSuccessCard.setManaged(true);
        updateSuccessOpenDocumentButtonVisibility();
    }

    private void appendSigningLogLine(String line) {
        if (line == null || line.isBlank()) {
            return;
        }
        System.out.println("[Firma] " + line);
        if (isInterfazMinimaActive()) {
            if (etaLabel != null && !line.startsWith("ERROR:") && !line.startsWith("AVISO")) {
                etaLabel.setText(shortStatusLine(line));
            }
            return;
        }
        signingLogArea.appendText(line + "\n");
        signingLogArea.positionCaret(signingLogArea.getText().length());
    }

    private boolean isInterfazMinimaActive() {
        InvokeConfig invokeConfig = LaunchContext.get().invokeConfig();
        return invokeConfig != null && invokeConfig.isInterfazMinima();
    }

    private void applyInterfazMinimaWorkspaceLayout() {
        setNodeVisible(mainSidebar, false);
        if (stage != null && !stage.isMaximized()) {
            stage.setWidth(INTERFAZ_MINIMA_WIDTH);
            stage.setHeight(INTERFAZ_MINIMA_HEIGHT);
            stage.setMinWidth(400);
            stage.setMinHeight(160);
        }
        applyInterfazMinimaSigningCardLayout();
    }

    private void applyInterfazMinimaSigningCardLayout() {
        setNodeVisible(signingStepper, false);
        setNodeVisible(signingConsoleSection, false);
        setNodeVisible(signingConsoleHeader, false);
        setNodeVisible(signingLogArea, false);
        setNodeVisible(signingFooter, false);
        if (progressCaptionLabel != null) {
            progressCaptionLabel.setVisible(false);
            progressCaptionLabel.setManaged(false);
        }
        if (signingCard != null) {
            signingCard.setMaxWidth(480);
        }
        if (signingModalTitle != null) {
            signingModalTitle.setText("Firmando documento…");
        }
        if (signingDocSubtitle != null) {
            signingDocSubtitle.setVisible(false);
            signingDocSubtitle.setManaged(false);
        }
        if (etaLabel != null) {
            etaLabel.setText("Iniciando firma…");
        }
    }

    private static void setNodeVisible(javafx.scene.Node node, boolean visible) {
        if (node != null) {
            node.setVisible(visible);
            node.setManaged(visible);
        }
    }

    private static String shortStatusLine(String line) {
        String trimmed = line.trim();
        if (trimmed.length() <= 72) {
            return trimmed;
        }
        return trimmed.substring(0, 69) + "…";
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

    /** Cierra el {@link Stage} principal y, con implicitExit por defecto, termina la aplicación. */
    private void closePrimaryWindow() {
        if (stage != null) {
            stage.close();
        } else {
            Platform.exit();
        }
    }

    @FXML
    private void onCloseWindow() {
        closePrimaryWindow();
    }

    @FXML
    private void onSidebarNavFirmar() {
        applySidebarNavHighlight(sidebarNavFirmar);
    }

    // Historial deshabilitado (ver bloque comentado en MainWorkspace.fxml)
    // @FXML
    // private void onSidebarNavHistorial() { ... }

    @FXML
    private void onSidebarNavAdministracion() {
        if (!AdminAccessPolicy.hasAdminAccess()) {
            InstitutionalDialog.warning(
                    stage,
                    "Acceso denegado",
                    "Su sesion no incluye roles de administracion de firma.\n"
                            + "Roles requeridos (config): "
                            + String.join(", ", AdminAccessPolicy.configuredAdminRoles()));
            return;
        }
        workspaceViewBeforeAdmin = detectCurrentWorkspaceView();
        showAdminConfigOnly();
        applySidebarNavHighlight(sidebarNavAdministracion);
    }

    @FXML
    private void onAdminBack() {
        restoreWorkspaceView(workspaceViewBeforeAdmin);
        applySidebarNavHighlight(sidebarNavFirmar);
    }

    @FXML
    private void onAdminSave() {
        try {
            SigningAdminConfig config = readAdminForm();
            SigningAdminConfigStore.save(config);
            InstitutionalDialog.success(
                    stage,
                    "Configuracion guardada",
                    "La configuracion de firma se guardo en:\n"
                            + SigningAdminConfigStore.getConfigFilePath());
        } catch (IOException ex) {
            InstitutionalDialog.error(
                    stage,
                    "Error al guardar",
                    "No se pudo guardar la configuracion:\n" + ex.getMessage());
        }
    }

    @FXML
    private void onAdminRestoreDefaults() {
        SigningAdminConfig defaults = SigningAdminConfig.defaults();
        populateAdminForm(defaults);
        InstitutionalDialog.info(
                stage,
                "Valores restaurados",
                "Se cargaron los valores por defecto. Pulse «Guardar configuracion» para persistirlos.");
    }

    void refreshAdminNavVisibility() {
        boolean admin = AdminAccessPolicy.hasAdminAccess();
        if (sidebarNavAdministracion != null) {
            sidebarNavAdministracion.setVisible(admin);
            sidebarNavAdministracion.setManaged(admin);
        }
    }

    private void configureAdminForm() {
        if (adminSignatureLevelCombo != null) {
            adminSignatureLevelCombo.setItems(FXCollections.observableArrayList("B", "T", "LTA"));
        }
        populateAdminForm(SigningAdminConfigStore.load());
    }

    private void populateAdminForm(SigningAdminConfig config) {
        if (config == null) {
            return;
        }
        if (adminTslUrlField != null) {
            adminTslUrlField.setText(config.tslUrl());
        }
        if (adminVerifyTslCheck != null) {
            adminVerifyTslCheck.setSelected(config.verifyTsl());
        }
        if (adminTsaUrlField != null) {
            adminTsaUrlField.setText(config.tsaUrl());
        }
        if (adminTsaUserField != null) {
            adminTsaUserField.setText(config.tsaUser());
        }
        if (adminTsaPasswordField != null) {
            adminTsaPasswordField.setText(config.tsaPassword());
        }
        if (adminSignatureLevelCombo != null) {
            adminSignatureLevelCombo.getSelectionModel().select(config.signatureLevel());
        }
        if (adminAlgorithmField != null) {
            adminAlgorithmField.setText(config.signatureAlgorithmNote());
        }
    }

    private SigningAdminConfig readAdminForm() {
        String level =
                adminSignatureLevelCombo != null
                                && adminSignatureLevelCombo.getSelectionModel().getSelectedItem() != null
                        ? adminSignatureLevelCombo.getSelectionModel().getSelectedItem()
                        : "B";
        return new SigningAdminConfig(
                adminTslUrlField != null ? adminTslUrlField.getText() : "",
                adminVerifyTslCheck != null && adminVerifyTslCheck.isSelected(),
                adminTsaUrlField != null ? adminTsaUrlField.getText() : "",
                adminTsaUserField != null ? adminTsaUserField.getText() : "",
                adminTsaPasswordField != null ? adminTsaPasswordField.getText() : "",
                level,
                adminAlgorithmField != null ? adminAlgorithmField.getText() : "");
    }

    private void showAdminConfigOnly() {
        hideAllContentCards();
        populateAdminForm(SigningAdminConfigStore.load());
        adminConfigCard.setVisible(true);
        adminConfigCard.setManaged(true);
    }

    private void hideAllContentCards() {
        if (certificateSelectorCard != null) {
            certificateSelectorCard.setVisible(false);
            certificateSelectorCard.setManaged(false);
        }
        if (documentViewerCard != null) {
            documentViewerCard.setVisible(false);
            documentViewerCard.setManaged(false);
        }
        if (signingCard != null) {
            signingCard.setVisible(false);
            signingCard.setManaged(false);
        }
        if (signingSuccessCard != null) {
            signingSuccessCard.setVisible(false);
            signingSuccessCard.setManaged(false);
        }
        if (adminConfigCard != null) {
            adminConfigCard.setVisible(false);
            adminConfigCard.setManaged(false);
        }
    }

    private WorkspaceView detectCurrentWorkspaceView() {
        if (signingSuccessCard != null && signingSuccessCard.isVisible()) {
            return WorkspaceView.SUCCESS_SCREEN;
        }
        if (signingCard != null && signingCard.isVisible()) {
            return WorkspaceView.SIGNING_PROCESS;
        }
        if (documentViewerCard != null && documentViewerCard.isVisible()) {
            return WorkspaceView.DOCUMENT_VIEWER;
        }
        return WorkspaceView.CERTIFICATE_SELECTOR;
    }

    private void restoreWorkspaceView(WorkspaceView view) {
        WorkspaceView target = view != null ? view : WorkspaceView.CERTIFICATE_SELECTOR;
        switch (target) {
            case DOCUMENT_VIEWER -> {
                hideAllContentCards();
                documentViewerCard.setVisible(true);
                documentViewerCard.setManaged(true);
            }
            case SIGNING_PROCESS -> showSigningProcessCardOnly();
            case SUCCESS_SCREEN -> showSuccessScreenOnly();
            default -> showCertificateSelectorOnly();
        }
    }

    private void returnToCertificateSelector() {
        selectFirstValidCertificate();
        showCertificateSelectorOnly();
        applySidebarNavHighlight(sidebarNavFirmar);
    }

    @FXML
    private void onNuevoProceso() {
        returnToCertificateSelector();
    }

    @FXML
    private void onLogoutQuick() {
        stopSigningTask();
        execSpinAnimation.stop();
        CertificateCatalog.clear();
        SigningDocumentContext.clear();
        SigningSessionContext.clear();
        new AuthenticationService().logout();
        try {
            LoginWindowOpener.returnToLogin(stage);
        } catch (IOException e) {
            InstitutionalDialog.error(
                    stage,
                    "Error de navegación",
                    "No se pudo volver al inicio de sesión:\n" + e.getMessage());
        }
    }

    @FXML
    private void onCancelCertificateSelector() {
        InstitutionalDialog.info(
                stage,
                "Selección cancelada",
                "Puede reanudar la selección con «+ Nuevo proceso» o al volver a «Firmar documento».");
    }

    @FXML
    private void onConfirmCertificateSelection() {
        CertificateTableRow selected = certificateTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            InstitutionalDialog.warning(stage, "Certificado requerido", "Seleccione un certificado en la tabla.");
            return;
        }
        if (selected.isExpired()) {
            InstitutionalDialog.warning(
                    stage,
                    "Certificado expirado",
                    "Este certificado está expirado. Elija uno con estado «VERIFICADO».");
            return;
        }
        InvokeConfig invokeConfig = LaunchContext.get().invokeConfig();
        System.out.println("[MainWorkspace] Confirmar certificado -> " + invokeConfig);

        SigningSessionContext.setSelectedCertificate(selected.getCertificate());
        SigningSessionContext.clearSignaturePlacement();

        if (!invokeConfig.shouldSkipDocumentViewer() && pdfViewerController == null) {
            InstitutionalDialog.error(stage, "Visor no disponible", "El visor de documentos no está disponible.");
            return;
        }

        if (invokeConfig.usesAlfrescoDocument()) {
            openAlfrescoDocument(invokeConfig);
            return;
        }

        if (invokeConfig.usesDocumentUrl()) {
            openUrlDocument(invokeConfig);
            return;
        }

        if (invokeConfig.hasInlineDocument()) {
            proceedWithDocument(LaunchContext.get().pdfViewerConfig(), invokeConfig);
            return;
        }

        PdfViewerConfig viewerConfig = LaunchContext.get().pdfViewerConfig();
        proceedWithDocument(viewerConfig, invokeConfig);
    }

    private void openUrlDocument(InvokeConfig invokeConfig) {
        String accessToken =
                AuthSession.getAccessToken()
                        .orElseGet(() -> invokeConfig.getToken().isBlank() ? "" : invokeConfig.getToken());
        if (!invokeConfig.shouldSkipDocumentViewer()) {
            certificateSelectorCard.setVisible(false);
            certificateSelectorCard.setManaged(false);
            signingCard.setVisible(false);
            signingCard.setManaged(false);
            signingSuccessCard.setVisible(false);
            signingSuccessCard.setManaged(false);
            documentViewerCard.setVisible(true);
            documentViewerCard.setManaged(true);
            pdfViewerController.showLoadingPlaceholder("Descargando documento...");
        } else {
            signingDocumentTitle = "Descargando documento...";
        }

        Thread loader =
                new Thread(
                        () -> {
                            DocumentUrlResult result =
                                    DocumentUrlClient.download(
                                            invokeConfig.getDocumentoUrl(),
                                            accessToken.isBlank() ? Optional.empty() : Optional.of(accessToken));
                            Platform.runLater(() -> applyDownloadedDocumentResult(result, invokeConfig));
                        },
                        "firmador-url-content");
        loader.setDaemon(true);
        loader.start();
    }

    private void applyDownloadedDocumentResult(DocumentUrlResult result, InvokeConfig invokeConfig) {
        if (result == null || !result.success()) {
            showCertificateSelectorOnly();
            InstitutionalDialog.error(
                    stage,
                    "Documento no disponible",
                    result != null ? result.message() : "No se pudo descargar el documento.");
            return;
        }
        PdfViewerConfig viewerConfig = PdfViewerConfig.fromInMemory(result.pdfBytes(), result.fileName());
        proceedWithDocument(viewerConfig, invokeConfig);
    }

    private void proceedWithDocument(PdfViewerConfig viewerConfig, InvokeConfig invokeConfig) {
        signingDocumentTitle = viewerConfig.displayFileName();
        SigningDocumentContext.set(viewerConfig);

        if (invokeConfig.shouldSkipDocumentViewer()) {
            hideDocumentViewer();
            showSigningProcessOnly();
            if (invokeConfig.signingParam().signatureFormat() != 1) {
                System.out.println(
                        "[MainWorkspace] Formato "
                                + invokeConfig.signingParam().standardFirma()
                                + ": sin visor PDF.");
            } else {
                System.out.println("[MainWorkspace] sinVisor activo: se omite visor PDF.");
            }
            return;
        }

        if (pdfViewerController == null) {
            InstitutionalDialog.error(stage, "Visor no disponible", "El visor de documentos no está disponible.");
            return;
        }
        showDocumentViewerOnly();
        pdfViewerController.setDragPlacementEnabled(invokeConfig.isFirmaArrastrar());
        pdfViewerController.open(viewerConfig, true);
    }

    private void showDocumentViewerReadOnly(PdfViewerConfig viewerConfig) {
        if (pdfViewerController == null) {
            InstitutionalDialog.error(stage, "Visor no disponible", "El visor de documentos no esta disponible.");
            return;
        }
        stopSigningTask();
        execSpinAnimation.stop();
        certificateSelectorCard.setVisible(false);
        certificateSelectorCard.setManaged(false);
        signingCard.setVisible(false);
        signingCard.setManaged(false);
        signingSuccessCard.setVisible(false);
        signingSuccessCard.setManaged(false);
        documentViewerCard.setVisible(true);
        documentViewerCard.setManaged(true);
        pdfViewerController.open(viewerConfig, false);
    }

    private Optional<PdfViewerConfig> resolveSignedDocument() {
        return SigningDocumentContext.getSignedDocument();
    }

    private void hideDocumentViewer() {
        if (pdfViewerController != null) {
            pdfViewerController.release();
        }
        documentViewerCard.setVisible(false);
        documentViewerCard.setManaged(false);
    }

    private void openAlfrescoDocument(InvokeConfig invokeConfig) {
        String accessToken =
                AuthSession.getAccessToken()
                        .orElseGet(() -> invokeConfig.getToken().isBlank() ? "" : invokeConfig.getToken());
        if (accessToken.isBlank()) {
            InstitutionalDialog.warning(
                    stage,
                    "Sesión requerida",
                    "No hay token de sesion para descargar el documento de Alfresco.");
            return;
        }

        if (!invokeConfig.shouldSkipDocumentViewer()) {
            certificateSelectorCard.setVisible(false);
            certificateSelectorCard.setManaged(false);
            signingCard.setVisible(false);
            signingCard.setManaged(false);
            signingSuccessCard.setVisible(false);
            signingSuccessCard.setManaged(false);
            documentViewerCard.setVisible(true);
            documentViewerCard.setManaged(true);
            pdfViewerController.showLoadingPlaceholder("Descargando documento de Alfresco...");
        } else {
            signingDocumentTitle = "Descargando documento...";
        }

        String nodeId = invokeConfig.getIdDocumentoAlfresco();
        Integer version =
                invokeConfig.getDocumentVersion().isPresent()
                        ? invokeConfig.getDocumentVersion().getAsInt()
                        : null;
        Thread loader =
                new Thread(
                        () -> {
                            AlfrescoContentResult result =
                                    alfrescoContentClient.fetchPdfContent(nodeId, accessToken, version);
                            Platform.runLater(() -> applyAlfrescoDocumentResult(result));
                        },
                        "firmador-alfresco-content");
        loader.setDaemon(true);
        loader.start();
    }

    private void applyAlfrescoDocumentResult(AlfrescoContentResult result) {
        if (result == null || !result.success()) {
            showCertificateSelectorOnly();
            InstitutionalDialog.error(
                    stage,
                    "Documento no disponible",
                    result != null ? result.message() : "No se pudo cargar el documento.");
            return;
        }

        InvokeConfig invokeConfig = LaunchContext.get().invokeConfig();
        PdfViewerConfig viewerConfig = PdfViewerConfig.fromInMemory(result.pdfBytes(), result.fileName());
        proceedWithDocument(viewerConfig, invokeConfig);
    }

    @FXML
    private void onCancelSigning() {
        showCertificateSelectorOnly();
        InstitutionalDialog.info(stage, "Operación cancelada", "El proceso de firma se canceló.");
    }

    @FXML
    private void onSuccessDownloadDocument() {
        Optional<PdfViewerConfig> document = resolveSignedDocument();
        if (document.isEmpty()) {
            InstitutionalDialog.warning(
                    stage,
                    "Sin documento firmado",
                    "No hay documento firmado disponible. Complete el proceso de firma antes de descargar.");
            return;
        }

        PdfViewerConfig config = document.get();
        String saveName = config.suggestedSaveFileName();
        String ext = saveName.contains(".") ? saveName.substring(saveName.lastIndexOf('.')) : "";
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Guardar documento");
        if (".xml".equalsIgnoreCase(ext)) {
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("XML", "*.xml"));
        } else if (".p7s".equalsIgnoreCase(ext)) {
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Firma CAdES", "*.p7s"));
        } else {
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        }
        chooser.setInitialFileName(saveName);
        java.io.File target = chooser.showSaveDialog(stage);
        if (target == null) {
            return;
        }

        try {
            Path targetPath = target.toPath();
            String fileName = targetPath.getFileName().toString();
            if (!ext.isEmpty() && !fileName.toLowerCase().endsWith(ext.toLowerCase())) {
                targetPath = targetPath.resolveSibling(fileName + ext);
            }
            Files.write(targetPath, config.readDocumentBytes());
            InstitutionalDialog.success(
                    stage,
                    "Documento guardado",
                    "Documento guardado en:\n" + targetPath.toAbsolutePath());
        } catch (IOException e) {
            InstitutionalDialog.error(
                    stage,
                    "Error al guardar",
                    "No se pudo guardar el documento:\n" + e.getMessage());
        }
    }

    @FXML
    private void onSuccessOpenSigned() {
        Optional<PdfViewerConfig> document = resolveSignedDocument();
        if (document.isEmpty()) {
            InstitutionalDialog.warning(
                    stage,
                    "Sin documento firmado",
                    "No hay documento firmado disponible. Complete el proceso de firma antes de visualizar.");
            return;
        }
        showDocumentViewerReadOnly(document.get());
    }

    @FXML
    private void onSuccessTechnicalDetail() {
        if (lastVerificationResult == null) {
            InstitutionalDialog.info(
                    stage,
                    "Detalle técnico",
                    "No hay resultado de verificacion disponible.");
            return;
        }
        InstitutionalDialog.info(
                stage,
                "Detalle técnico de verificacion",
                lastVerificationResult.technicalDetailText());
    }

    @FXML
    private void onSigningSuccessClose() {
        closePrimaryWindow();
    }

    @FXML
    private void onFooterSupport() {
        InstitutionalDialog.info(stage, "Soporte técnico", "Consulte los canales oficiales de soporte.");
    }

    @FXML
    private void onFooterPolicy() {
        InstitutionalDialog.info(
                stage,
                "Políticas de seguridad",
                "Referencia institucional; enlace público cuando esté disponible.");
    }
}
