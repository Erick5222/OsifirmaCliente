package com.osinergmin.firma.desktop.service.signing;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.util.Matrix;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfTagPositionResolverTest {

    @TempDir
    Path tempDir;

    @Test
    void markersIncludePlainAndGuillemetVariants() {
        List<String> markers = PdfTagPositionResolver.markersFor("GYAURI");
        assertTrue(markers.contains("GYAURI"));
        assertTrue(markers.stream().anyMatch(m -> m.contains("«GYAURI»")));
    }

    @Test
    void findsTagSplitAcrossWordsInPdf() throws Exception {
        Path pdf = tempDir.resolve("tag-test.pdf");
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            PDFont font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(font, 12);
                cs.newLineAtOffset(100, 700);
                cs.showText("Firma aqui: ");
                cs.showText("«");
                cs.showText("GYAURI");
                cs.showText("»");
                cs.endText();
            }
            doc.save(pdf.toFile());
        }

        Optional<PdfTagPositionResolver.TagPosition> found = PdfTagPositionResolver.find(pdf, "GYAURI");
        assertTrue(found.isPresent(), "Debe localizar GYAURI aunque venga en varios showText");
        assertEquals(1, found.get().pageOneBased());
        assertTrue(found.get().posX() > 50);
        assertTrue(found.get().posY() > 50);
        assertTrue(found.get().posY() < 300, "PDF estandar: tag arriba debe tener posY bajo, no cerca del pie");
    }

    @Test
    void findsTagInWordStyleFlippedPdf() throws Exception {
        Path pdf = tempDir.resolve("tag-word-flip.pdf");
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            float pageHeight = page.getMediaBox().getHeight();
            PDFont font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.transform(new Matrix(1, 0, 0, -1, 0, pageHeight));
                cs.beginText();
                cs.setFont(font, 12);
                cs.newLineAtOffset(91, 125);
                cs.showText("Pruebas con tag");
                cs.endText();
                cs.beginText();
                cs.setFont(font, 12);
                cs.newLineAtOffset(91, 145);
                cs.showText("«GYAURI»");
                cs.endText();
            }
            doc.save(pdf.toFile());
        }

        Optional<PdfTagPositionResolver.TagPosition> found = PdfTagPositionResolver.find(pdf, "GYAURI");
        assertTrue(found.isPresent(), "Debe localizar GYAURI en PDF con eje Y invertido (Word)");
        assertEquals(1, found.get().pageOneBased());
        assertTrue(found.get().posX() > 50);
        assertTrue(
                found.get().posY() < 300,
                "PDF Word: posY debe estar en la zona superior, no cerca de 717 (pie de pagina)");
        assertTrue(found.get().posY() > 50);
        assertTrue(
                found.get().posY() < 130,
                "El sello debe subir respecto al tag para cubrirlo (posY ~117, no ~145)");
    }

    @Test
    void coverRectIsCompactForWordStyleTag() throws Exception {
        Path pdf = tempDir.resolve("tag-cover-compact.pdf");
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            float pageHeight = page.getMediaBox().getHeight();
            PDFont font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.transform(new Matrix(1, 0, 0, -1, 0, pageHeight));
                cs.beginText();
                cs.setFont(font, 12);
                cs.newLineAtOffset(91, 125);
                cs.showText("Pruebas con tag");
                cs.endText();
                cs.beginText();
                cs.setFont(font, 12);
                cs.newLineAtOffset(91, 145);
                cs.showText("«GYAURI»");
                cs.endText();
            }
            doc.save(pdf.toFile());
        }

        assertTrue(PdfTagPositionResolver.eraseMarker(pdf, "GYAURI"));
    }
}
