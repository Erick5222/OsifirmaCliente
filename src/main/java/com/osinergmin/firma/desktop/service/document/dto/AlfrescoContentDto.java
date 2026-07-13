package com.osinergmin.firma.desktop.service.document.dto;

public final class AlfrescoContentDto {

    private String nodeId;
    private String fileName;
    private String contentType;
    private Long sizeInBytes;
    private String base64Content;

    public String getNodeId() {
        return nodeId;
    }

    public String getFileName() {
        return fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public Long getSizeInBytes() {
        return sizeInBytes;
    }

    public String getBase64Content() {
        return base64Content;
    }
}
