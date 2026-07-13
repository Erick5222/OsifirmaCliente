package com.osinergmin.firma.desktop.ui.viewer;

import com.osinergmin.firma.desktop.App;
import com.osinergmin.firma.desktop.pdf.PdfViewerConfig;
import com.osinergmin.firma.desktop.service.signing.SignaturePlacement;
import com.osinergmin.firma.desktop.ui.dialog.InstitutionalDialog;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.rendering.PDFRenderer;

import javafx.embed.swing.SwingFXUtils;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Visor PDF renderizado con Apache PDFBox, con simulación de colocación de firma por arrastre.
 */
public class DocumentViewerController {

    private static final DataFormat SIGNATURE_DRAG_FORMAT =
            new DataFormat("application/vnd.osinergmin.signature-sim.v1");

    private static final float BASE_RENDER_DPI = 110f;
    private static final int ZOOM_STEP = 25;

    @FXML
    private Label fileNameLabel;

    @FXML
    private Label pageLabel;

    @FXML
    private Label zoomPercentLabel;

    @FXML
    private Label footerLabel;

    @FXML
    private Button prevPageButton;

    @FXML
    private Button nextPageButton;

    @FXML
    private ScrollPane scrollPane;

    @FXML
    private ImageView pageImageView;

    @FXML
    private Pane signatureOverlayPane;

    @FXML
    private VBox signatureDragChip;

    @FXML
    private HBox signatureRow;

    @FXML
    private HBox continueRow;

    @FXML
    private Button continueSigningButton;

    @FXML
    private Button backToSummaryButton;

    private Stage hostStage;
    private Runnable onContinueSigning;
    private Consumer<Optional<SignaturePlacement>> onPlacementCapturedBeforeContinue;
    private Runnable onBackToSummary;
    private boolean signingUiVisible = true;
    private boolean dragPlacementEnabled = true;

    private PDDocument document;
    private PDFRenderer renderer;
    private String classpathResource;
    private String localFilePath;
    private String displayFileName;
    private int currentPageIndex;
    private int zoomPercent = 100;
    private int lastRenderToken;
    private long stampSequence;

    public void setHostStage(Stage hostStage) {
        this.hostStage = hostStage;
    }

    public void setOnContinueSigning(Runnable onContinueSigning) {
        this.onContinueSigning = onContinueSigning;
    }

    public void setOnPlacementCapturedBeforeContinue(
            Consumer<Optional<SignaturePlacement>> onPlacementCapturedBeforeContinue) {
        this.onPlacementCapturedBeforeContinue = onPlacementCapturedBeforeContinue;
    }

    public void setOnBackToSummary(Runnable onBackToSummary) {
        this.onBackToSummary = onBackToSummary;
        applySigningUiVisibility();
    }

    public void setSigningUiVisible(boolean visible) {
        signingUiVisible = visible;
        applySigningUiVisibility();
    }

    /** Controla si se muestra y captura el arrastre del sello (independiente del visor PDF). */
    public void setDragPlacementEnabled(boolean enabled) {
        dragPlacementEnabled = enabled;
        if (!dragPlacementEnabled) {
            clearSignatureStamps();
        }
        applySigningUiVisibility();
    }

    private void applySigningUiVisibility() {
        boolean showDragControls = signingUiVisible && dragPlacementEnabled;
        if (signatureRow != null) {
            signatureRow.setVisible(showDragControls);
            signatureRow.setManaged(showDragControls);
        }
        if (continueSigningButton != null) {
            continueSigningButton.setVisible(signingUiVisible);
            continueSigningButton.setManaged(signingUiVisible);
        }
        boolean showBack = !signingUiVisible && onBackToSummary != null;
        if (backToSummaryButton != null) {
            backToSummaryButton.setVisible(showBack);
            backToSummaryButton.setManaged(showBack);
        }
        if (continueRow != null) {
            boolean rowVisible = signingUiVisible || showBack;
            continueRow.setVisible(rowVisible);
            continueRow.setManaged(rowVisible);
        }
        if (signatureOverlayPane != null) {
            boolean overlayInteractive = signingUiVisible && dragPlacementEnabled;
            signatureOverlayPane.setMouseTransparent(!overlayInteractive);
            if (!overlayInteractive) {
                clearSignatureStamps();
            }
        }
    }

    public String getDisplayFileName() {
        return displayFileName != null ? displayFileName : "";
    }

    /** Captura la posición del sello gráfico en la página visible (si existe). */
    public Optional<SignaturePlacement> captureSignaturePlacement() {
        if (!dragPlacementEnabled) {
            return Optional.empty();
        }
        if (signatureOverlayPane == null || document == null) {
            return Optional.empty();
        }
        if (signatureOverlayPane.getChildren().isEmpty()) {
            return Optional.empty();
        }
        if (!(signatureOverlayPane.getChildren().get(signatureOverlayPane.getChildren().size() - 1)
                instanceof Region stamp)) {
            return Optional.empty();
        }

        PDRectangle pageBox = resolvePageBox(currentPageIndex);
        double[] overlaySize = resolveOverlayDimensions();
        double overlayWidth = overlaySize[0];
        double overlayHeight = overlaySize[1];
        if (overlayWidth <= 0 || overlayHeight <= 0) {
            return Optional.empty();
        }

        stamp.applyCss();
        stamp.layout();
        double stampWidth = Math.max(stamp.getWidth(), stamp.prefWidth(-1));
        double stampHeight = Math.max(stamp.getHeight(), stamp.prefHeight(-1));

        return Optional.of(
                new SignaturePlacement(
                        currentPageIndex + 1,
                        stamp.getLayoutX(),
                        stamp.getLayoutY(),
                        overlayWidth,
                        overlayHeight,
                        stampWidth,
                        stampHeight,
                        pageBox.getWidth(),
                        pageBox.getHeight()));
    }

    private PDRectangle resolvePageBox(int pageIndex) {
        PDRectangle cropBox = document.getPage(pageIndex).getCropBox();
        if (cropBox != null && cropBox.getWidth() > 0 && cropBox.getHeight() > 0) {
            return cropBox;
        }
        return document.getPage(pageIndex).getMediaBox();
    }

    private double[] resolveOverlayDimensions() {
        double width = signatureOverlayPane.getWidth();
        double height = signatureOverlayPane.getHeight();
        if (width > 0 && height > 0) {
            return new double[] {width, height};
        }
        Image image = pageImageView != null ? pageImageView.getImage() : null;
        if (image != null && image.getWidth() > 0 && image.getHeight() > 0) {
            return new double[] {image.getWidth(), image.getHeight()};
        }
        return new double[] {0, 0};
    }

    @FXML
    private void initialize() {
        if (footerLabel != null) {
            footerLabel.setText("V2.4.0-STABLE | SISTEMA DE FIRMA DIGITAL OSINERGMIN");
        }
        if (scrollPane != null) {
            scrollPane.setFitToWidth(true);
        }
        configureSignatureDragDrop();
        applySigningUiVisibility();
        if (signatureDragChip != null) {
            signatureDragChip.setDisable(document == null);
        }
    }

    /** Muestra titulo mientras se descarga el documento (Alfresco u origen remoto). */
    public void showLoadingPlaceholder(String message) {
        release();
        displayFileName = message != null ? message : "Cargando documento...";
        if (fileNameLabel != null) {
            fileNameLabel.setText(displayFileName);
        }
        if (pageLabel != null) {
            pageLabel.setText("—");
        }
        if (zoomPercentLabel != null) {
            zoomPercentLabel.setText("—");
        }
        if (signatureDragChip != null) {
            signatureDragChip.setDisable(true);
        }
    }

    /** Carga un PDF desde el classpath y muestra la primera pagina. */
    public void open(PdfViewerConfig config) {
        open(config, true);
    }

    /** Carga un PDF; {@code signingUiVisible=false} oculta controles de firma (solo lectura). */
    public void open(PdfViewerConfig config, boolean signingUiVisible) {
        Objects.requireNonNull(config, "config");
        setSigningUiVisible(signingUiVisible);
        releaseDocumentOnly();
        this.classpathResource = config.classpathResourceAbsolute();
        this.localFilePath = config.fileSystemPath();
        this.displayFileName = config.displayFileName();
        this.zoomPercent = config.initialZoomPercent();
        if (footerLabel != null && config.footerCaption() != null && !config.footerCaption().isBlank()) {
            footerLabel.setText(config.footerCaption());
        }

        if (fileNameLabel != null) {
            fileNameLabel.setText(displayFileName);
        }

        try {
            if (config.usesInMemoryBytes()) {
                document = Loader.loadPDF(config.inMemoryBytes());
            } else if (config.usesLocalFile()) {
                Path file = Path.of(config.fileSystemPath());
                if (!Files.isRegularFile(file)) {
                    throw new IOException("No se encontro el archivo: " + file);
                }
                document = Loader.loadPDF(file.toFile());
            } else {
                try (InputStream in = App.class.getResourceAsStream(classpathResource)) {
                    if (in == null) {
                        throw new IOException("No se encontro el recurso: " + classpathResource);
                    }
                    byte[] bytes = in.readAllBytes();
                    document = Loader.loadPDF(bytes);
                }
            }
            renderer = new PDFRenderer(document);
            currentPageIndex = 0;
            updateNavState();
            renderCurrentPageAsync();
        } catch (IOException e) {
            InstitutionalDialog.error(
                    dialogOwner(),
                    "Error al abrir PDF",
                    "No se pudo abrir el documento PDF:\n" + e.getMessage());
        }
    }

    /** Libera el documento abierto (memoria nativa / descriptor). */
    public void release() {
        releaseDocumentOnly();
        signingUiVisible = true;
        applySigningUiVisibility();
    }

    private void releaseDocumentOnly() {
        lastRenderToken++;
        clearSignatureStamps();
        if (document != null) {
            try {
                document.close();
            } catch (IOException ignored) {
                // best effort
            }
            document = null;
        }
        renderer = null;
        localFilePath = null;
        classpathResource = null;
        if (pageImageView != null) {
            pageImageView.setImage(null);
        }
        syncOverlayToImage(null);
    }

    @FXML
    private void onPrevPage() {
        if (document == null || currentPageIndex <= 0) {
            return;
        }
        currentPageIndex--;
        updateNavState();
        renderCurrentPageAsync();
    }

    @FXML
    private void onNextPage() {
        if (document == null || currentPageIndex >= document.getNumberOfPages() - 1) {
            return;
        }
        currentPageIndex++;
        updateNavState();
        renderCurrentPageAsync();
    }

    @FXML
    private void onZoomOut() {
        zoomPercent = Math.max(25, zoomPercent - ZOOM_STEP);
        updateNavState();
        renderCurrentPageAsync();
    }

    @FXML
    private void onZoomIn() {
        zoomPercent = Math.min(400, zoomPercent + ZOOM_STEP);
        updateNavState();
        renderCurrentPageAsync();
    }

    @FXML
    private void onPrint() {
        InstitutionalDialog.info(
                dialogOwner(),
                "Impresión",
                "La impresión mediante cola institucional se integrará con el motor de firma y la impresora de red autorizada.");
    }

    @FXML
    private void onDownload() {
        boolean fromFile = localFilePath != null && !localFilePath.isBlank();
        boolean fromClasspath = classpathResource != null && !classpathResource.isBlank();
        if (!fromFile && !fromClasspath) {
            return;
        }
        Window owner = hostStage != null ? hostStage : fileNameLabel.getScene().getWindow();
        var chooser = new FileChooser();
        chooser.setTitle("Guardar copia del documento");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        chooser.setInitialFileName(displayFileName != null ? displayFileName : "documento.pdf");
        var target = chooser.showSaveDialog(owner);
        if (target == null) {
            return;
        }
        try {
            if (fromFile) {
                Files.copy(Path.of(localFilePath), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } else {
                try (InputStream in = App.class.getResourceAsStream(classpathResource)) {
                    if (in == null) {
                        throw new IOException("Recurso no disponible.");
                    }
                    Files.copy(in, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            }
            InstitutionalDialog.success(dialogOwner(), "Descarga completada", "Archivo guardado correctamente.");
        } catch (IOException e) {
            InstitutionalDialog.error(dialogOwner(), "Error al guardar", "No se pudo guardar:\n" + e.getMessage());
        }
    }

    @FXML
    private void onContinueSigning() {
        Optional<SignaturePlacement> captured = captureSignaturePlacement();
        if (dragPlacementEnabled && captured.isEmpty()) {
            InstitutionalDialog.warning(
                    dialogOwner(),
                    "Sello no colocado",
                    "Arrastre el sello al documento PDF y vuelva a pulsar «Continuar con la firma».");
            return;
        }
        if (onPlacementCapturedBeforeContinue != null) {
            onPlacementCapturedBeforeContinue.accept(captured);
        }
        if (onContinueSigning != null) {
            onContinueSigning.run();
        }
    }

    @FXML
    private void onBackToSummary() {
        if (onBackToSummary != null) {
            onBackToSummary.run();
        }
    }

    private void configureSignatureDragDrop() {
        if (signatureDragChip != null) {
            signatureDragChip.setOnDragDetected(
                    event -> {
                        if (!dragPlacementEnabled || document == null || signatureOverlayPane == null) {
                            event.consume();
                            return;
                        }
                        Dragboard db = signatureDragChip.startDragAndDrop(TransferMode.COPY);
                        ClipboardContent cc = new ClipboardContent();
                        cc.put(SIGNATURE_DRAG_FORMAT, "sim");
                        db.setContent(cc);
                        event.consume();
                    });
        }
        if (signatureOverlayPane != null) {
            signatureOverlayPane.setOnDragOver(
                    event -> {
                        if (!dragPlacementEnabled) {
                            event.consume();
                            return;
                        }
                        if (event.getGestureSource() == signatureDragChip
                                && event.getDragboard().hasContent(SIGNATURE_DRAG_FORMAT)) {
                            event.acceptTransferModes(TransferMode.COPY);
                        }
                        event.consume();
                    });
            signatureOverlayPane.setOnDragDropped(
                    event -> {
                        if (!dragPlacementEnabled) {
                            event.setDropCompleted(false);
                            event.consume();
                            return;
                        }
                        Dragboard db = event.getDragboard();
                        boolean ok = false;
                        if (db.hasContent(SIGNATURE_DRAG_FORMAT)) {
                            addSignatureStampAt(event.getX(), event.getY());
                            ok = true;
                        }
                        event.setDropCompleted(ok);
                        event.consume();
                    });
        }
    }

    private void clearSignatureStamps() {
        if (signatureOverlayPane != null) {
            signatureOverlayPane.getChildren().clear();
        }
    }

    private void syncOverlayToImage(Image img) {
        if (signatureOverlayPane == null) {
            return;
        }
        if (img == null) {
            signatureOverlayPane.setMinSize(0, 0);
            signatureOverlayPane.setPrefSize(0, 0);
            signatureOverlayPane.setMaxSize(0, 0);
            return;
        }
        double w = img.getWidth();
        double h = img.getHeight();
        signatureOverlayPane.setMinSize(w, h);
        signatureOverlayPane.setPrefSize(w, h);
        signatureOverlayPane.setMaxSize(w, h);
    }

    private void addSignatureStampAt(double x, double y) {
        if (!dragPlacementEnabled || signatureOverlayPane == null) {
            return;
        }
        Region stamp = buildStampNode();
        stamp.applyCss();
        stamp.layout();
        double sw = Math.max(stamp.prefWidth(-1), stamp.getBoundsInLocal().getWidth());
        double sh = Math.max(stamp.prefHeight(-1), stamp.getBoundsInLocal().getHeight());
        if (sw <= 0) {
            sw = 160;
        }
        if (sh <= 0) {
            sh = 72;
        }
        double ow = signatureOverlayPane.getWidth();
        double oh = signatureOverlayPane.getHeight();
        double lx = x - sw / 2;
        double ly = y - sh / 2;
        lx = clamp(lx, 0, Math.max(0, ow - sw));
        ly = clamp(ly, 0, Math.max(0, oh - sh));
        stamp.setLayoutX(lx);
        stamp.setLayoutY(ly);
        wireStampDrag(stamp);
        signatureOverlayPane.getChildren().add(stamp);
    }

    private Region buildStampNode() {
        long id = ++stampSequence;
        VBox root = new VBox(6);
        root.getStyleClass().add("signature-stamp");
        root.setPadding(new Insets(8, 10, 10, 10));
        root.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        HBox top = new HBox();
        Region grow = new Region();
        HBox.setHgrow(grow, Priority.ALWAYS);
        Button close = new Button("✕");
        close.getStyleClass().add("signature-stamp-close");
        close.setFocusTraversable(false);
        close.setOnAction(
                e -> {
                    var parent = root.getParent();
                    if (parent instanceof Pane p) {
                        p.getChildren().remove(root);
                    }
                });
        top.getChildren().addAll(grow, close);
        top.setAlignment(Pos.CENTER_RIGHT);

        Label l1 = new Label("Firma digital");
        l1.getStyleClass().add("signature-stamp-line1");
        Label l2 = new Label("Osinergmin (simulación)");
        l2.getStyleClass().add("signature-stamp-line2");
        Label l3 = new Label("Ref. sim. OS-" + (10_000 + id));
        l3.getStyleClass().add("signature-stamp-line3");
        VBox textCol = new VBox(2);
        textCol.setAlignment(Pos.CENTER);
        textCol.getChildren().addAll(l1, l2, l3);

        root.getChildren().addAll(top, textCol);
        return root;
    }

    private void wireStampDrag(Region stamp) {
        final double[] pressInOverlay = new double[2];
        final double[] startLayout = new double[2];

        stamp.setOnMousePressed(
                e -> {
                    stamp.toFront();
                    Point2D p = signatureOverlayPane.sceneToLocal(e.getSceneX(), e.getSceneY());
                    pressInOverlay[0] = p.getX();
                    pressInOverlay[1] = p.getY();
                    startLayout[0] = stamp.getLayoutX();
                    startLayout[1] = stamp.getLayoutY();
                    e.consume();
                });
        stamp.setOnMouseDragged(
                e -> {
                    Point2D cur = signatureOverlayPane.sceneToLocal(e.getSceneX(), e.getSceneY());
                    double dx = cur.getX() - pressInOverlay[0];
                    double dy = cur.getY() - pressInOverlay[1];
                    double w = stamp.getWidth();
                    double h = stamp.getHeight();
                    double nx = startLayout[0] + dx;
                    double ny = startLayout[1] + dy;
                    stamp.setLayoutX(clamp(nx, 0, Math.max(0, signatureOverlayPane.getWidth() - w)));
                    stamp.setLayoutY(clamp(ny, 0, Math.max(0, signatureOverlayPane.getHeight() - h)));
                    e.consume();
                });
    }

    private Window dialogOwner() {
        if (hostStage != null) {
            return hostStage;
        }
        if (fileNameLabel != null && fileNameLabel.getScene() != null) {
            return fileNameLabel.getScene().getWindow();
        }
        return null;
    }

    private static double clamp(double v, double min, double max) {
        if (max < min) {
            return min;
        }
        return Math.min(max, Math.max(min, v));
    }

    private void updateNavState() {
        int total = document != null ? document.getNumberOfPages() : 0;
        if (pageLabel != null) {
            if (total == 0) {
                pageLabel.setText("Página — de —");
            } else {
                pageLabel.setText("Página " + (currentPageIndex + 1) + " de " + total);
            }
        }
        if (zoomPercentLabel != null) {
            zoomPercentLabel.setText(zoomPercent + "%");
        }
        if (prevPageButton != null) {
            prevPageButton.setDisable(document == null || currentPageIndex <= 0);
        }
        if (nextPageButton != null) {
            nextPageButton.setDisable(document == null || currentPageIndex >= total - 1);
        }
        if (signatureDragChip != null) {
            signatureDragChip.setDisable(document == null);
        }
    }

    private void renderCurrentPageAsync() {
        if (document == null || renderer == null) {
            return;
        }
        final int token = ++lastRenderToken;
        final int page = currentPageIndex;
        final float dpi = BASE_RENDER_DPI * (zoomPercent / 100f);

        Task<Image> task =
                new Task<>() {
                    @Override
                    protected Image call() throws Exception {
                        BufferedImage bi = renderer.renderImageWithDPI(page, dpi);
                        return SwingFXUtils.toFXImage(bi, null);
                    }
                };
        task.setOnSucceeded(
                e -> {
                    if (token != lastRenderToken) {
                        return;
                    }
                    Image img = task.getValue();
                    if (img != null && pageImageView != null) {
                        clearSignatureStamps();
                        pageImageView.setImage(img);
                        syncOverlayToImage(img);
                    }
                });
        task.setOnFailed(
                e -> {
                    Throwable ex = task.getException();
                    String msg = ex != null ? ex.getMessage() : "Error al renderizar la página.";
                    Platform.runLater(
                            () ->
                                    InstitutionalDialog.error(
                                            dialogOwner(),
                                            "Error de renderizado",
                                            msg));
                });
        Thread t = new Thread(task, "pdf-render");
        t.setDaemon(true);
        t.start();
    }
}
