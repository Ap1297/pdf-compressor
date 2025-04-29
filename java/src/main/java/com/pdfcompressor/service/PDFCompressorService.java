package com.pdfcompressor.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class PDFCompressorService {

    private final String TEMP_DIR = System.getProperty("java.io.tmpdir");

    public File compressPDF(MultipartFile file, int qualityLevel) throws IOException {
        // Save uploaded file to temp location
        File inputFile = saveToTempFile(file);
        File outputFile = new File(inputFile.getParent(), "compressed-" + UUID.randomUUID() + ".pdf");

        // Perform compression using Ghostscript
        compressWithGhostscript(inputFile.getAbsolutePath(), outputFile.getAbsolutePath(), qualityLevel);

        // Clean up input file (optional)
        inputFile.delete();

        return outputFile;
    }

    private File saveToTempFile(MultipartFile file) throws IOException {
        File tempFile = Files.createTempFile("input-", ".pdf").toFile();
        try (InputStream in = file.getInputStream();
             FileOutputStream out = new FileOutputStream(tempFile)) {
            in.transferTo(out);
        }
        return tempFile;
    }

    private void compressWithGhostscript(String inputPath, String outputPath, int qualityLevel) throws IOException {
        String quality = switch (qualityLevel) {
            case 0 -> "/screen";   // low quality, smallest size
            case 1 -> "/ebook";    // medium quality
            case 2 -> "/printer";  // good quality
            default -> "/default"; // fallback
        };

        // Determine the Ghostscript executable based on OS
        String os = System.getProperty("os.name").toLowerCase();
        String gsCommand = os.contains("win")
        	    ? "C:\\Program Files\\gs\\gs10.05.0\\bin\\gswin64c.exe"
        	    : "gs";


        ProcessBuilder pb = new ProcessBuilder(
            gsCommand,
            "-sDEVICE=pdfwrite",
            "-dCompatibilityLevel=1.4",
            "-dPDFSETTINGS=" + quality,
            "-dNOPAUSE",
            "-dQUIET",
            "-dBATCH",
            "-sOutputFile=" + outputPath,
            inputPath
        );

        Process process = pb.start();
        try {
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("Ghostscript failed with exit code " + exitCode);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Ghostscript process interrupted", e);
        }
    }

    public Path getCompressedFilePath(String fileName) {
        return Paths.get(TEMP_DIR, fileName);
    }

    public boolean deleteFiles(String fileName) {
        File file = new File(TEMP_DIR, fileName);
        return file.exists() && file.delete();
    }

    public long getOriginalFileSize(String fileName) {
        File file = new File(TEMP_DIR, fileName);
        return file.exists() ? file.length() : 0;
    }

    public long getCompressedFileSize(String fileName) {
        return getOriginalFileSize(fileName);
    }
}
