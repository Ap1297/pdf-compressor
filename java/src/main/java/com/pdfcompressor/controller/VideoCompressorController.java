package com.pdfcompressor.controller;


import com.pdfcompressor.model.CompressionResponse;
import com.pdfcompressor.service.VideoCompressorService;
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
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/video")
@CrossOrigin(origins = "*") // In production, restrict this to your frontend URL
public class VideoCompressorController {

    private final VideoCompressorService videoCompressorService;

    @Autowired
    public VideoCompressorController(VideoCompressorService videoCompressorService) {
        this.videoCompressorService = videoCompressorService;
    }

    @PostMapping("/compress")
    public ResponseEntity<CompressionResponse> compressVideo(
            @RequestParam("file") MultipartFile file,
            @RequestParam("compressionLevel") int compressionLevel) {
        
        try {
            // Validate file
            if (file.isEmpty() || !file.getContentType().startsWith("video/")) {
                return ResponseEntity.badRequest().body(new CompressionResponse(false, null, 0, 0, "Invalid file"));
            }

            // Compress the video
            String fileName = videoCompressorService.compressVideo(file, compressionLevel);
            
            // Get the size of the original and compressed files
            long originalSize = videoCompressorService.getOriginalFileSize(fileName);
            long compressedSize = videoCompressorService.getCompressedFileSize(fileName);
            
            // Create response
            CompressionResponse response = new CompressionResponse(
                true,
                fileName,
                originalSize,
                compressedSize,
                "Video compressed successfully"
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
            Path filePath = videoCompressorService.getCompressedFilePath(fileName);
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
            boolean deleted = videoCompressorService.deleteFiles(fileName);
            
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
            case "mp4":
                return "video/mp4";
            case "avi":
                return "video/x-msvideo";
            case "mov":
                return "video/quicktime";
            case "wmv":
                return "video/x-ms-wmv";
            case "flv":
                return "video/x-flv";
            case "mkv":
                return "video/x-matroska";
            case "webm":
                return "video/webm";
            default:
                return "application/octet-stream";
        }
    }
}

