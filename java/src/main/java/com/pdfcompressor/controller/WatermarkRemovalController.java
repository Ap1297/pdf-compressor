package com.pdfcompressor.controller;

import com.pdfcompressor.model.WatermarkRemovalResponse;
import com.pdfcompressor.service.WatermarkRemovalService;
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
@RequestMapping("/api/watermark")
@CrossOrigin(origins = "*") // In production, restrict this to your frontend URL
public class WatermarkRemovalController {

    private final WatermarkRemovalService watermarkRemovalService;

    @Autowired
    public WatermarkRemovalController(WatermarkRemovalService watermarkRemovalService) {
        this.watermarkRemovalService = watermarkRemovalService;
    }

    @PostMapping("/remove/image")
    public ResponseEntity<WatermarkRemovalResponse> removeWatermarkFromImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "threshold", defaultValue = "200") int threshold,
            @RequestParam(value = "tolerance", defaultValue = "30") int tolerance) {
        
        try {
            // Validate file
            if (file.isEmpty() || !file.getContentType().startsWith("image/")) {
                return ResponseEntity.badRequest().body(
                    new WatermarkRemovalResponse(false, null, "image", "Invalid file. Please upload an image file.")
                );
            }
            
            // Process the image to remove watermark
            String fileName = watermarkRemovalService.removeWatermarkFromImage(file, threshold, tolerance);
            
            // Create response
            WatermarkRemovalResponse response = new WatermarkRemovalResponse(
                true,
                fileName,
                "image",
                "Watermark removed from image successfully"
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                .body(new WatermarkRemovalResponse(false, null, "image", "Error: " + e.getMessage()));
        }
    }

    @PostMapping("/remove/pdf")
    public ResponseEntity<WatermarkRemovalResponse> removeWatermarkFromPDF(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "threshold", defaultValue = "200") int threshold,
            @RequestParam(value = "tolerance", defaultValue = "30") int tolerance) {
        
        try {
            // Validate file
            if (file.isEmpty() || !file.getContentType().equals("application/pdf")) {
                return ResponseEntity.badRequest().body(
                    new WatermarkRemovalResponse(false, null, "pdf", "Invalid file. Please upload a PDF file.")
                );
            }
            
            // Process the PDF to remove watermark
            String fileName = watermarkRemovalService.removeWatermarkFromPDF(file, threshold, tolerance);
            
            // Create response
            WatermarkRemovalResponse response = new WatermarkRemovalResponse(
                true,
                fileName,
                "pdf",
                "Watermark removed from PDF successfully"
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                .body(new WatermarkRemovalResponse(false, null, "pdf", "Error: " + e.getMessage()));
        }
    }

    @GetMapping("/download/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName) {
        try {
            Path filePath = watermarkRemovalService.getProcessedFilePath(fileName);
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists()) {
                // Determine content type based on file extension
                String contentType = determineContentType(fileName);
                
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
            boolean deleted = watermarkRemovalService.deleteFiles(fileName);
            
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
    
    private String determineContentType(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        switch (extension) {
            case "pdf":
                return "application/pdf";
            case "png":
                return "image/png";
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "gif":
                return "image/gif";
            case "bmp":
                return "image/bmp";
            case "webp":
                return "image/webp";
            case "tiff":
            case "tif":
                return "image/tiff";
            default:
                return "application/octet-stream";
        }
    }
}
