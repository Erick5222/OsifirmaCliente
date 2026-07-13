package com.osinergmin.firma.desktop.service.auth.dto;

import com.google.gson.annotations.SerializedName;

public final class TokenDataDto {

    @SerializedName("access_token")
    private String accessToken;

    @SerializedName("refresh_token")
    private String refreshToken;

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }
}
