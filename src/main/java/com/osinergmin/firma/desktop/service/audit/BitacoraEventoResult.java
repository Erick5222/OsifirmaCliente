package com.osinergmin.firma.desktop.service.audit;

public record BitacoraEventoResult(boolean success, String message, int httpStatus) {

    public static BitacoraEventoResult ok(String message, int httpStatus) {
        return new BitacoraEventoResult(true, message, httpStatus);
    }

    public static BitacoraEventoResult fail(String message, int httpStatus) {
        return new BitacoraEventoResult(false, message, httpStatus);
    }
}
