package com.pdfcompressor.controller;

import com.pdfcompressor.model.ConversionResponse;
import com.pdfcompressor.service.DocumentConversionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/convert")
@CrossOrigin(origins = "*") // In production, restrict this to your frontend URL
public class DocumentConversionController {

    private final DocumentConversionService documentConversionService;

    @Autowired
    public DocumentConversionController(DocumentConversionService documentConversionService) {
        this.documentConversionService = documentConversionService;
    }

    @PostMapping("/pdf-to-word")
    public ResponseEntity<ConversionResponse> convertPdfToWord(@RequestParam("file") MultipartFile file) {
        try {
            // Validate file
            if (file.isEmpty() || !file.getContentType().equals("application/pdf")) {
                return ResponseEntity.badRequest().body(
                    new ConversionResponse(false, null, "PDF", "DOCX", "Invalid file. Please upload a PDF file.")
                );
            }
            
            // Convert the PDF to Word
            String fileName = documentConversionService.convertPdfToWord(file);
            
            // Create response
            ConversionResponse response = new ConversionResponse(
                true,
                fileName,
                "PDF",
                "DOCX",
                "PDF converted to Word successfully"
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                .body(new ConversionResponse(false, null, "PDF", "DOCX", "Error: " + e.getMessage()));
        }
    }

    @PostMapping("/word-to-pdf")
    public ResponseEntity<ConversionResponse> convertWordToPdf(@RequestParam("file") MultipartFile file) {
        try {
            // Validate file
            if (file.isEmpty() || 
                (!file.getContentType().equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document") &&
                 !file.getContentType().equals("application/msword"))) {
                return ResponseEntity.badRequest().body(
                    new ConversionResponse(false, null, "DOCX", "PDF", "Invalid file. Please upload a Word document.")
                );
            }
            
            // Convert the Word to PDF
            String fileName = documentConversionService.convertWordToPdf(file);
            
            // Create response
            ConversionResponse response = new ConversionResponse(
                true,
                fileName,
                "DOCX",
                "PDF",
                "Word document converted to PDF successfully"
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                .body(new ConversionResponse(false, null, "DOCX", "PDF", "Error: " + e.getMessage()));
        }
    }

    @GetMapping("/download/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName) {
        try {
            Path filePath = documentConversionService.getConvertedFilePath(fileName);
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists()) {
                // Determine content type based on file extension
                String contentType;
                if (fileName.endsWith(".pdf")) {
                    contentType = "application/pdf";
                } else if (fileName.endsWith(".docx")) {
                    contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                } else {
                    contentType = "application/octet-stream";
                }
                
                return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
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
            boolean deleted = documentConversionService.deleteFiles(fileName);
            
            if (deleted) {
                response.put("success", true);
                response.put("message", "Files deleted successfully");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Failed to delete files");
                return ResponseEntity.status(500).body(response);
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}
