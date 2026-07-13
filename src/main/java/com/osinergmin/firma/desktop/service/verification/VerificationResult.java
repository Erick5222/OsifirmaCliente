package com.osinergmin.firma.desktop.service.verification;

import pe.gob.osinergmin.firma.bean.VerificacionBean;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Resultado de verificar el PDF firmado de la sesion actual. */
public final class VerificationResult {

    private final boolean success;
    private final String message;
    private final String documentName;
    private final String estado;
    private final String integridad;
    private final int numeroFirmas;
    private final int firmasValidas;
    private final String fechaValidacion;
    private final List<SignatureVerificationDetail> firmas;

    private VerificationResult(
            boolean success,
            String message,
            String documentName,
            String estado,
            String integridad,
            int numeroFirmas,
            int firmasValidas,
            String fechaValidacion,
            List<SignatureVerificationDetail> firmas) {
        this.success = success;
        this.message = message == null ? "" : message;
        this.documentName = nullToDash(documentName);
        this.estado = nullToDash(estado);
        this.integridad = nullToDash(integridad);
        this.numeroFirmas = numeroFirmas;
        this.firmasValidas = firmasValidas;
        this.fechaValidacion = nullToDash(fechaValidacion);
        this.firmas = firmas == null ? List.of() : List.copyOf(firmas);
    }

    public static VerificationResult from(VerificacionBean bean) {
        if (bean == null) {
            return failure("El motor de verificacion no devolvio resultado.");
        }
        List<SignatureVerificationDetail> details = new ArrayList<>();
        if (bean.getFirmas() != null) {
            for (int i = 0; i < bean.getFirmas().size(); i++) {
                var firma = bean.getFirmas().get(i);
                details.add(
                        new SignatureVerificationDetail(
                                i + 1,
                                nullToDash(firma.getFirmadoPor()),
                                nullToDash(firma.getEstadoFirma()),
                                nullToDash(firma.getFechaFirma()),
                                nullToDash(firma.getFormatoFirma()),
                                nullToDash(firma.getMotivo()),
                                nullToDash(firma.getAlgoritmo()),
                                nullToDash(firma.getNotBefore()),
                                nullToDash(firma.getNotAfter())));
            }
        }
        int numeroFirmas = bean.getNumeroFirmas();
        int firmasValidas = bean.getFirmasValidas();
        boolean ok = isIntegrityOk(bean.getIntegridad()) && numeroFirmas > 0 && firmasValidas >= numeroFirmas;
        return new VerificationResult(
                ok,
                ok ? "Verificacion completada." : "Verificacion con observaciones.",
                bean.getNombreDocumento(),
                bean.getEstado(),
                formatIntegridad(bean.getIntegridad()),
                numeroFirmas,
                firmasValidas,
                bean.getFechaValidacion(),
                details);
    }

    public static VerificationResult failure(String message) {
        return new VerificationResult(
                false,
                message == null || message.isBlank() ? "No se pudo verificar la firma." : message.trim(),
                "",
                "",
                "",
                0,
                0,
                "",
                List.of());
    }

    public boolean success() {
        return success;
    }

    public String message() {
        return message;
    }

    public String documentName() {
        return documentName;
    }

    public String estadoLabel() {
        if (!success && numeroFirmas == 0) {
            return "Sin firmas detectadas";
        }
        if (success) {
            return "Valido · " + firmasValidas + "/" + numeroFirmas + " firmas";
        }
        return estado + " · " + firmasValidas + "/" + numeroFirmas + " firmas";
    }

    public String integridadLabel() {
        return integridad.isBlank() || "-".equals(integridad) ? "No disponible" : integridad;
    }

    public String firmasResumenLabel() {
        if (numeroFirmas <= 0) {
            return "Sin firmas en el documento";
        }
        String firmante =
                firmas.isEmpty() ? "" : firmas.get(0).firmadoPor();
        if (firmante.isBlank() || "-".equals(firmante)) {
            return firmasValidas + " de " + numeroFirmas + " firmas validas";
        }
        return firmasValidas + " de " + numeroFirmas + " validas · " + firmante;
    }

    public String fechaValidacionLabel() {
        return fechaValidacion;
    }

    public String successSubtitle() {
        if (success) {
            return "Documento firmado y verificado correctamente.";
        }
        return "Documento firmado. Revise el resultado de la verificacion.";
    }

    public String logLine() {
        return "Verificacion: "
                + estadoLabel()
                + " | Integridad: "
                + integridadLabel()
                + " | "
                + firmasResumenLabel();
    }

    public String technicalDetailText() {
        StringBuilder sb = new StringBuilder();
        sb.append("Documento: ").append(documentName).append('\n');
        sb.append("Estado: ").append(estadoLabel()).append('\n');
        sb.append("Integridad: ").append(integridadLabel()).append('\n');
        sb.append("Fecha verificacion: ").append(fechaValidacionLabel()).append('\n');
        sb.append("Firmas validas: ").append(firmasValidas).append('/').append(numeroFirmas).append('\n');
        if (firmas.isEmpty()) {
            sb.append('\n').append(message);
            return sb.toString().trim();
        }
        for (SignatureVerificationDetail firma : firmas) {
            sb.append('\n').append("--- Firma ").append(firma.index()).append(" ---\n");
            sb.append("Firmado por: ").append(firma.firmadoPor()).append('\n');
            sb.append("Estado: ").append(firma.estadoFirma()).append('\n');
            sb.append("Fecha firma: ").append(firma.fechaFirma()).append('\n');
            sb.append("Formato: ").append(firma.formatoFirma()).append('\n');
            sb.append("Motivo: ").append(firma.motivo()).append('\n');
            sb.append("Algoritmo: ").append(firma.algoritmo()).append('\n');
            sb.append("Certificado: ").append(firma.notBefore()).append(" - ").append(firma.notAfter());
        }
        return sb.toString().trim();
    }

    private static boolean isIntegrityOk(Object integridad) {
        if (integridad instanceof Boolean value) {
            return value;
        }
        if (integridad == null) {
            return false;
        }
        String text = integridad.toString().trim().toLowerCase(Locale.ROOT);
        return text.equals("true")
                || text.contains("ok")
                || text.contains("valid")
                || text.contains("integ")
                || text.contains("sin modific");
    }

    private static String formatIntegridad(Object integridad) {
        if (integridad instanceof Boolean value) {
            return value ? "Sin modificaciones" : "Documento alterado";
        }
        if (integridad == null || integridad.toString().isBlank()) {
            return "No disponible";
        }
        return integridad.toString().trim();
    }

    private static String nullToDash(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }
}
