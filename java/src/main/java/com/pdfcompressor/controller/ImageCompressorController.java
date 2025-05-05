package com.pdfcompressor.controller;

import com.pdfcompressor.model.CompressionResponse;
import com.pdfcompressor.service.ImageCompressorService;
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
@RequestMapping("/api/image")
@CrossOrigin(origins = "*") // In production, restrict this to your frontend URL
public class ImageCompressorController {

    private final ImageCompressorService imageCompressorService;

    @Autowired
    public ImageCompressorController(ImageCompressorService imageCompressorService) {
        this.imageCompressorService = imageCompressorService;
    }

    @PostMapping("/compress")
    public ResponseEntity<CompressionResponse> compressImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("compressionLevel") int compressionLevel) {
        
        try {
            // Validate file
            if (file.isEmpty() || !file.getContentType().startsWith("image/")) {
                return ResponseEntity.badRequest().body(new CompressionResponse(false, null, 0, 0, "Invalid file"));
            }

            // Convert compression level to quality (0-1)
            float quality = Math.max(0.1f, 1 - (compressionLevel / 100.0f));
            
            // Compress the image
            String fileName = imageCompressorService.compressImage(file, quality);
            
            // Get the size of the original and compressed files
            long originalSize = imageCompressorService.getOriginalFileSize(fileName);
            long compressedSize = imageCompressorService.getCompressedFileSize(fileName);
            
            // Create response
            CompressionResponse response = new CompressionResponse(
                true,
                fileName,
                originalSize,
                compressedSize,
                "Image compressed successfully"
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                .body(new CompressionResponse(false, null, 0, 0, "Error: " + e.getMessage()));
        }
    }

    @GetMapping("/download/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName) {
        try {
            Path filePath = imageCompressorService.getCompressedFilePath(fileName);
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists()) {
                // Determine the content type based on file extension
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
            boolean deleted = imageCompressorService.deleteFiles(fileName);
            
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
