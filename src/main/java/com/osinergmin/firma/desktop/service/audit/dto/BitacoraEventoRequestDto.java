package com.osinergmin.firma.desktop.service.audit.dto;

/** Payload para POST /api/bitacora/evento (firma-digital-service). */
public final class BitacoraEventoRequestDto {

    public String coTransaccion;
    public String coTipoEvento;
    public String coEstado;
    public String coUsuario;
    public String coDniFirmante;
    public String noFirmante;
    public String coCertificado;
    public String coOrigenDocumento;
    public String coNodeAlfresco;
    public String noArchivo;
    public Long nuTamanio;
    public String coHashOriginal;
    public String coHashFirmado;
    public String coFormatoFirma;
    public String coNivelFirma;
    public String coComponente;
    public String coVersion;
    public String coIpOrigen;
    public String deMensaje;
    public String deError;
}
