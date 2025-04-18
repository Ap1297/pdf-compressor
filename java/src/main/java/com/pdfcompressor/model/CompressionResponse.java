package com.pdfcompressor.model;

public class CompressionResponse {
    private boolean success;
    private String fileName;
    private long originalSize;
    private long compressedSize;
    private String message;

    public CompressionResponse(boolean success, String fileName, long originalSize, long compressedSize, String message) {
        this.success = success;
        this.fileName = fileName;
        this.originalSize = originalSize;
        this.compressedSize = compressedSize;
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
    
    public long getOriginalSize() {
        return originalSize;
    }

    public void setOriginalSize(long originalSize) {
        this.originalSize = originalSize;
    }

    public long getCompressedSize() {
        return compressedSize;
    }

    public void setCompressedSize(long compressedSize) {
        this.compressedSize = compressedSize;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
