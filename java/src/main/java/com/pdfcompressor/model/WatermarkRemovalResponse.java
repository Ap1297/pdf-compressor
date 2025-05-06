package com.pdfcompressor.model;

public class WatermarkRemovalResponse {
    private boolean success;
    private String fileName;
    private String fileType;
    private String message;

    public WatermarkRemovalResponse(boolean success, String fileName, String fileType, String message) {
        this.success = success;
        this.fileName = fileName;
        this.fileType = fileType;
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
    
    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
