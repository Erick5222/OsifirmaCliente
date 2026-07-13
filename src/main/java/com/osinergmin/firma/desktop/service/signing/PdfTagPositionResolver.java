package com.osinergmin.firma.desktop.service.signing;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.awt.Color;

import com.osinergmin.firma.desktop.pdf.PdfViewerConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Localiza un marcador de texto en el PDF (ej. GYAURI o «GYAURI») y devuelve pagina/coordenadas para PAdES. */
public final class PdfTagPositionResolver {

    public record TagPosition(int pageOneBased, int posX, int posY) {}

    private PdfTagPositionResolver() {}

    public static Optional<TagPosition> findFromDocument(PdfViewerConfig document, String tagName) {
        if (document == null || !document.isPdf()) {
            System.err.println("[PdfTagPositionResolver] El documento no es PDF; no se puede buscar tag.");
            return Optional.empty();
        }
        try {
            byte[] bytes = document.readDocumentBytes();
            if (bytes == null || bytes.length == 0) {
                return Optional.empty();
            }
            Path temp =
                    Files.createTempFile("firmador-tag-", ".pdf");
            try {
                Files.write(temp, bytes);
                return find(temp, tagName);
            } finally {
                Files.deleteIfExists(temp);
            }
        } catch (IOException ex) {
            System.err.println("[PdfTagPositionResolver] Error leyendo documento: " + ex.getMessage());
            return Optional.empty();
        }
    }

    public static Optional<TagPosition> find(Path pdfPath, String tagName) {
        if (pdfPath == null || !Files.isRegularFile(pdfPath)) {
            System.err.println("[PdfTagPositionResolver] PDF de entrada invalido: " + pdfPath);
            return Optional.empty();
        }
        List<String> needles = markersFor(tagName);
        if (needles.isEmpty()) {
            return Optional.empty();
        }
        try (PDDocument document = Loader.loadPDF(pdfPath.toFile())) {
            for (int pageIndex = 0; pageIndex < document.getNumberOfPages(); pageIndex++) {
                Optional<PageMatch> match = locateOnPage(document, pageIndex, needles);
                if (match.isEmpty()) {
                    continue;
                }
                TagPosition position = toTagPosition(document.getPage(pageIndex), match.get());
                PageMatch pageMatch = match.get();
                int originalIndex = pageMatch.index().compactToOriginalIndex(pageMatch.compactStart());
                System.out.println(
                        "[PdfTagPositionResolver] Marcador '"
                                + tagName
                                + "' en pagina "
                                + position.pageOneBased()
                                + " -> posx="
                                + position.posX()
                                + ", posy="
                                + position.posY()
                                + " (fragmento: \""
                                + pageMatch.index().snippetAround(originalIndex, 24)
                                + "\")");
                return Optional.of(position);
            }
            System.err.println(
                    "[PdfTagPositionResolver] Marcador '"
                            + tagName
                            + "' no encontrado en "
                            + pdfPath.getFileName()
                            + ". Texto compacto pag.1 (muestra): \""
                            + firstPageSample(document)
                            + "\"");
        } catch (IOException ex) {
            System.err.println("[PdfTagPositionResolver] Error leyendo PDF: " + ex.getMessage());
            return Optional.empty();
        }
        return Optional.empty();
    }

    /**
     * Oculta el marcador de texto en el PDF (ej. «GYAURI») antes de firmar, cubriendolo con blanco.
     *
     * @return {@code true} si se elimino al menos una ocurrencia
     */
    public static boolean eraseMarker(Path pdfPath, String tagName) throws IOException {
        if (pdfPath == null || !Files.isRegularFile(pdfPath)) {
            return false;
        }
        List<String> needles = markersFor(tagName);
        if (needles.isEmpty()) {
            return false;
        }
        try (PDDocument document = Loader.loadPDF(Files.readAllBytes(pdfPath))) {
            boolean removed = false;
            for (int pageIndex = 0; pageIndex < document.getNumberOfPages(); pageIndex++) {
                Optional<PageMatch> match = locateOnPage(document, pageIndex, needles);
                if (match.isEmpty()) {
                    continue;
                }
                coverMatchWithWhite(document, document.getPage(pageIndex), match.get());
                removed = true;
            }
            if (removed) {
                Path temp = Files.createTempFile("firmador-tag-erase-", ".pdf");
                try {
                    document.save(temp.toFile());
                    Files.move(temp, pdfPath, StandardCopyOption.REPLACE_EXISTING);
                } finally {
                    Files.deleteIfExists(temp);
                }
                System.out.println(
                        "[PdfTagPositionResolver] Marcador '"
                                + tagName
                                + "' eliminado del PDF antes de firmar.");
            }
            return removed;
        }
    }

    private static String firstPageSample(PDDocument document) throws IOException {
        if (document.getNumberOfPages() == 0) {
            return "";
        }
        PageTextIndex index = extractPageIndex(document, 1);
        String sample = index.compactText();
        return sample.length() > 120 ? sample.substring(0, 120) + "…" : sample;
    }

    static List<String> markersFor(String tagName) {
        if (tagName == null || tagName.isBlank()) {
            return List.of();
        }
        String core = normalizeForSearch(tagName.trim());
        List<String> markers = new ArrayList<>();
        markers.add(core);
        markers.add(normalizeForSearch("«" + tagName.trim() + "»"));
        return markers.stream()
                .filter(s -> !s.isBlank())
                .distinct()
                .sorted((left, right) -> Integer.compare(right.length(), left.length()))
                .toList();
    }

    private static PageTextIndex extractPageIndex(PDDocument document, int pageOneBased) throws IOException {
        PageCollector collector = new PageCollector();
        collector.setSortByPosition(true);
        collector.setStartPage(pageOneBased);
        collector.setEndPage(pageOneBased);
        collector.getText(document);
        return collector.index();
    }

    private record PageMatch(int pageIndex, int compactStart, String matchedNeedle, PageTextIndex index) {}

    private static Optional<PageMatch> locateOnPage(
            PDDocument document, int pageIndex, List<String> needles) throws IOException {
        PageTextIndex index = extractPageIndex(document, pageIndex + 1);
        Optional<NeedleHit> hit = locateNeedle(index.compactText(), needles);
        if (hit.isEmpty()) {
            return Optional.empty();
        }
        NeedleHit needleHit = hit.get();
        return Optional.of(new PageMatch(pageIndex, needleHit.compactStart(), needleHit.needle(), index));
    }

    private record NeedleHit(int compactStart, String needle) {}

    private static Optional<NeedleHit> locateNeedle(String compactHaystack, List<String> needles) {
        if (compactHaystack == null || compactHaystack.isBlank()) {
            return Optional.empty();
        }
        NeedleHit best = null;
        for (String needle : needles) {
            if (needle.isBlank()) {
                continue;
            }
            int index = compactHaystack.indexOf(needle);
            if (index >= 0 && (best == null || needle.length() > best.needle().length())) {
                best = new NeedleHit(index, needle);
            }
        }
        return Optional.ofNullable(best);
    }

    /** Desplazamiento hacia arriba para que el sello PAdES cubra el marcador de texto. */
    private static final float TAG_SIGNATURE_Y_NUDGE_PT = 28f;

    private static TagPosition toTagPosition(PDPage page, PageMatch match) {
        float pageHeight = page.getMediaBox().getHeight();
        List<TextPosition> positions = match.index().positions();
        int originalStart = match.index().compactToOriginalIndex(match.compactStart());
        int originalEnd =
                match.index().compactToOriginalIndex(
                        match.compactStart() + match.matchedNeedle().length() - 1);
        float[] bounds = boundsForMatchedText(positions, originalStart, originalEnd);
        int posX = Math.max(0, Math.round(bounds[0]));
        float topFromPageTop = tagTopFromPageTop(bounds, pageHeight, positions);
        int posY = Math.max(0, Math.round(topFromPageTop - TAG_SIGNATURE_Y_NUDGE_PT));
        return new TagPosition(match.pageIndex() + 1, posX, posY);
    }

    /** Borde superior del tag en coordenadas del motor (origen arriba-izquierda). */
    private static float tagTopFromPageTop(
            float[] bounds, float pageHeight, List<TextPosition> positions) {
        if (usesTopDownTextCoords(positions, pageHeight)) {
            return bounds[1];
        }
        return pageHeight - bounds[3];
    }

    private static void coverMatchWithWhite(PDDocument document, PDPage page, PageMatch match) throws IOException {
        PageTextIndex index = match.index();
        float pageHeight = page.getMediaBox().getHeight();
        int compactEnd = match.compactStart() + match.matchedNeedle().length() - 1;
        int originalStart = index.compactToOriginalIndex(match.compactStart());
        int originalEnd = index.compactToOriginalIndex(compactEnd);
        float[] bounds = boundsForMatchedText(index.positions(), originalStart, originalEnd);
        float[] box = coverRectInPdfSpace(pageHeight, bounds, index.positions(), 1.5f);
        try (PDPageContentStream stream =
                new PDPageContentStream(
                        document,
                        page,
                        PDPageContentStream.AppendMode.APPEND,
                        true,
                        true)) {
            stream.setNonStrokingColor(Color.WHITE);
            stream.addRect(box[0], box[1], box[2], box[3]);
            stream.fill();
        }
        System.out.println(
                "[PdfTagPositionResolver] Borrado marcador: x="
                        + Math.round(box[0])
                        + " y="
                        + Math.round(box[1])
                        + " w="
                        + Math.round(box[2])
                        + " h="
                        + Math.round(box[3]));
    }

    /** Bounding box solo de los caracteres del match en la misma linea (ignora posiciones duplicadas de saltos). */
    private static float[] boundsForMatchedText(List<TextPosition> positions, int startIdx, int endIdx) {
        int from = Math.max(0, startIdx);
        int to = Math.min(positions.size() - 1, endIdx);
        if (from > to || positions.isEmpty()) {
            return new float[] {0f, 0f, 1f, 1f};
        }
        TextPosition anchor = positions.get(from);
        float anchorY = anchor.getYDirAdj();
        float lineSlack = Math.max(6f, anchor.getHeightDir() * 1.25f);
        float maxHeight = Math.max(16f, anchor.getHeightDir() * 1.5f);

        float minX = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        for (int i = from; i <= to; i++) {
            TextPosition tp = positions.get(i);
            if (Math.abs(tp.getYDirAdj() - anchorY) > lineSlack) {
                continue;
            }
            float x = tp.getXDirAdj();
            float width = Math.max(tp.getWidthDirAdj(), 0.5f);
            float y = tp.getYDirAdj();
            float height = Math.max(tp.getHeightDir(), 0.5f);
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x + width);
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y + height);
        }
        if (minX == Float.MAX_VALUE) {
            minX = anchor.getXDirAdj();
            maxX = minX + Math.max(anchor.getWidthDirAdj(), 1f);
            minY = anchorY;
            maxY = anchorY + Math.max(anchor.getHeightDir(), 1f);
        }
        if (maxY - minY > maxHeight) {
            maxY = minY + maxHeight;
        }
        return new float[] {minX, minY, maxX, maxY};
    }

    /** Rectangulo de borrado en coordenadas PDF (origen abajo-izquierda) para PDPageContentStream. */
    static float[] coverRectInPdfSpace(
            float pageHeight, float[] bounds, List<TextPosition> pagePositions, float pad) {
        float minX = bounds[0] - pad;
        float minY = bounds[1] - pad;
        float maxX = bounds[2] + pad;
        float maxY = bounds[3] + pad;
        float width = maxX - minX;
        float height = maxY - minY;
        if (usesTopDownTextCoords(pagePositions, pageHeight)) {
            float drawY = pageHeight - maxY;
            return new float[] {minX, drawY, width, height};
        }
        return new float[] {minX, minY, width, height};
    }

    static String normalizeForSearch(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFKC).toUpperCase(Locale.ROOT);
    }

    /**
     * Convierte la Y del marcador al sistema del motor PAdES (origen arriba-izquierda, Y hacia abajo).
     * PDF estandar: origen abajo-izquierda (texto superior con Y alta).
     * PDFs de Word: transformacion de pagina con Y baja para texto en la zona superior.
     */
    static float yFromPageTop(TextPosition anchor, float pageHeight, List<TextPosition> pagePositions) {
        if (anchor == null) {
            return 0f;
        }
        float yAdj = anchor.getYDirAdj();
        if (pagePositions == null || pagePositions.isEmpty()) {
            return pageHeight - yAdj;
        }
        if (usesTopDownTextCoords(pagePositions, pageHeight)) {
            return yAdj;
        }
        return pageHeight - yAdj;
    }

    static boolean usesTopDownTextCoords(List<TextPosition> positions, float pageHeight) {
        return !positions.isEmpty() && medianYDirAdj(positions) < pageHeight * 0.5f;
    }

    private static float medianYDirAdj(List<TextPosition> positions) {
        float[] values = new float[positions.size()];
        for (int i = 0; i < positions.size(); i++) {
            values[i] = positions.get(i).getYDirAdj();
        }
        Arrays.sort(values);
        int mid = values.length / 2;
        return values.length % 2 == 0 ? (values[mid - 1] + values[mid]) / 2f : values[mid];
    }

    private static final class PageTextIndex {
        private final String rawText;
        private final String compactText;
        private final int[] compactToOriginal;
        private final List<TextPosition> positions;

        private PageTextIndex(
                String rawText, String compactText, int[] compactToOriginal, List<TextPosition> positions) {
            this.rawText = rawText;
            this.compactText = compactText;
            this.compactToOriginal = compactToOriginal;
            this.positions = positions;
        }

        String compactText() {
            return compactText;
        }

        String rawText() {
            return rawText;
        }

        int compactToOriginalIndex(int compactIndex) {
            if (compactToOriginal.length == 0) {
                return 0;
            }
            if (compactIndex < 0) {
                return compactToOriginal[0];
            }
            if (compactIndex >= compactToOriginal.length) {
                return compactToOriginal[compactToOriginal.length - 1];
            }
            return compactToOriginal[compactIndex];
        }

        TextPosition positionAt(int charIndex) {
            int idx = Math.min(Math.max(charIndex, 0), Math.max(0, positions.size() - 1));
            return positions.get(idx);
        }

        List<TextPosition> positions() {
            return positions;
        }

        String snippetAround(int charIndex, int radius) {
            if (rawText.isEmpty()) {
                return "";
            }
            int start = Math.max(0, charIndex - radius);
            int end = Math.min(rawText.length(), charIndex + radius);
            return rawText.substring(start, end).replace('\n', ' ').replace('\r', ' ');
        }
    }

    private static final class PageCollector extends PDFTextStripper {

        private final StringBuilder rawText = new StringBuilder();
        private final StringBuilder searchText = new StringBuilder();
        private final List<TextPosition> positions = new ArrayList<>();

        private PageCollector() throws IOException {}

        PageTextIndex index() {
            StringBuilder compact = new StringBuilder();
            List<Integer> map = new ArrayList<>();
            for (int i = 0; i < searchText.length(); i++) {
                char c = searchText.charAt(i);
                if (!Character.isWhitespace(c)) {
                    compact.append(c);
                    map.add(i);
                }
            }
            int[] compactToOriginal = map.stream().mapToInt(Integer::intValue).toArray();
            return new PageTextIndex(
                    rawText.toString(), compact.toString(), compactToOriginal, List.copyOf(positions));
        }

        @Override
        protected void writeString(String string, List<TextPosition> textPositions) throws IOException {
            if (string == null || string.isEmpty() || textPositions == null || textPositions.isEmpty()) {
                return;
            }
            for (int i = 0; i < string.length(); i++) {
                char ch = string.charAt(i);
                rawText.append(ch);
                searchText.append(normalizeChar(ch));
                int posIndex = textPositions.size() == 1 ? 0 : Math.min(i, textPositions.size() - 1);
                positions.add(textPositions.get(posIndex));
            }
        }

        @Override
        protected void writeLineSeparator() throws IOException {
            appendSeparator(' ');
        }

        @Override
        protected void writeWordSeparator() throws IOException {
            appendSeparator(' ');
        }

        private void appendSeparator(char ch) {
            rawText.append(ch);
            searchText.append(ch);
            if (!positions.isEmpty()) {
                positions.add(positions.getLast());
            }
        }

        private static char normalizeChar(char ch) {
            String single = Normalizer.normalize(String.valueOf(ch), Normalizer.Form.NFKC);
            if (single.isEmpty()) {
                return ch;
            }
            return Character.toUpperCase(single.charAt(0));
        }
    }
}
