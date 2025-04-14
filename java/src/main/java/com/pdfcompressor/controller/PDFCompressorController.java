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

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // In production, restrict this to your frontend URL
public class PDFCompressorController {

    private final PDFCompressorService pdfCompressorService;

    @Autowired
    public PDFCompressorController(PDFCompressorService pdfCompressorService) {
        this.pdfCompressorService = pdfCompressorService;
    }

    @PostMapping("/compress")
    public ResponseEntity<CompressionResponse> compressPDF(
            @RequestParam("file") MultipartFile file,
            @RequestParam("compressionLevel") int compressionLevel) {
        
        try {
            // Validate file
            if (file.isEmpty() || !file.getContentType().equals("application/pdf")) {
                return ResponseEntity.badRequest().body(new CompressionResponse(false, null, 0, "Invalid file"));
            }

            // Convert compression level to quality (0-1)
            float quality = Math.max(0.1f, 1 - (compressionLevel / 100.0f));
            
            // Compress the PDF
            String fileName = pdfCompressorService.compressPDF(file, quality);
            
            // Get the size of the compressed file
            long compressedSize = pdfCompressorService.getCompressedFileSize(fileName);
            
            // Create response
            CompressionResponse response = new CompressionResponse(
                true,
                fileName,
                compressedSize,
                "PDF compressed successfully"
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                .body(new CompressionResponse(false, null, 0, "Error: " + e.getMessage()));
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
}
