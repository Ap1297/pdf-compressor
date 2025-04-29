package com.pdfcompressor.controller;

import com.pdfcompressor.model.CompressionResponse;
import com.pdfcompressor.service.PDFCompressorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // Restrict in production
public class PDFCompressorController {

    private final PDFCompressorService pdfCompressorService;

    @Autowired
    public PDFCompressorController(PDFCompressorService pdfCompressorService) {
        this.pdfCompressorService = pdfCompressorService;
    }

    @PostMapping("/compress")
    public ResponseEntity<CompressionResponse> compressPDF(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "compressionLevel", defaultValue = "1") int compressionLevel) {

        try {
            if (file.isEmpty() || !file.getContentType().equals("application/pdf")) {
                return ResponseEntity.badRequest()
                        .body(new CompressionResponse(false, null, 0, 0, "Invalid file. Only PDFs are allowed."));
            }

            // Compress PDF using Ghostscript (compressionLevel: 0 = low, 1 = med, 2 = high)
            File compressedFile = pdfCompressorService.compressPDF(file, compressionLevel);

            // Prepare metadata for response
            long originalSize = file.getSize();
            long compressedSize = compressedFile.length();
            String fileName = compressedFile.getName();

            CompressionResponse response = new CompressionResponse(
                true,
                fileName,
                originalSize,
                compressedSize,
                "PDF compressed successfully"
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(new CompressionResponse(false, null, 0, 0, "Compression failed: " + e.getMessage()));
        }
    }

    @GetMapping("/download/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName) {
        try {
            Path filePath = pdfCompressorService.getCompressedFilePath(fileName);
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_PDF)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/delete/{fileName:.+}")
    public ResponseEntity<Map<String, Object>> deleteFile(@PathVariable String fileName) {
        Map<String, Object> response = new HashMap<>();

        try {
            boolean deleted = pdfCompressorService.deleteFiles(fileName);

            if (deleted) {
                response.put("success", true);
                response.put("message", "Files deleted successfully");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "File deletion failed");
                return ResponseEntity.status(500).body(response);
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    
}
