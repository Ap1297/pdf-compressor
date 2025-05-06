package com.pdfcompressor.model;

public class ConversionResponse {
    private boolean success;
    private String fileName;
    private String sourceFormat;
    private String targetFormat;
    private String message;

    public ConversionResponse(boolean success, String fileName, String sourceFormat, String targetFormat, String message) {
        this.success = success;
        this.fileName = fileName;
        this.sourceFormat = sourceFormat;
        this.targetFormat = targetFormat;
        this.message = message;
    }

    // Getters and setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }
    
    public String getSourceFormat() {
        return sourceFormat;
    }

    public void setSourceFormat(String sourceFormat) {
        this.sourceFormat = sourceFormat;
    }

    public String getTargetFormat() {
        return targetFormat;
    }

    public void setTargetFormat(String targetFormat) {
        this.targetFormat = targetFormat;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
