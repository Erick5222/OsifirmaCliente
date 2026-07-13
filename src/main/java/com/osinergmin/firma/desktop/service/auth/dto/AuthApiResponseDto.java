package com.osinergmin.firma.desktop.service.auth.dto;

import com.google.gson.JsonElement;

public final class AuthApiResponseDto {

    private String message;
    private JsonElement data;
    private Integer statusCode;

    public String getMessage() {
        return message;
    }

    public JsonElement getData() {
        return data;
    }

    public Integer getStatusCode() {
        return statusCode;
    }
}
