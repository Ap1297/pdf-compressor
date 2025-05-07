package com.pdfcompressor.service;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class VideoCompressorService {

    private final Path uploadDir = Paths.get("uploads");
    private final Path outputDir = Paths.get("outputs");

    public VideoCompressorService() {
        try {
            Files.createDirectories(uploadDir);
            Files.createDirectories(outputDir);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directories", e);
        }
    }

    public String compressVideo(MultipartFile file, int compressionLevel) throws IOException {
        // Generate unique file names
        String originalFileName = file.getOriginalFilename();
        String fileExtension = getFileExtension(originalFileName);
        String fileId = file.getOriginalFilename();
        
        Path inputPath = uploadDir.resolve(fileId + "." + fileExtension);
        Path outputPath = outputDir.resolve(fileId + "_compressed." + fileExtension);

        // Save the uploaded file
        Files.write(inputPath, file.getBytes());

        // Get original file size
        long originalSize = Files.size(inputPath);

        try {
            // Compress the video
            compressVideoFile(inputPath.toFile(), outputPath.toFile(), compressionLevel);

            // Check if compression actually reduced the file size
            long compressedSize = Files.size(outputPath);
            
            // If compression increased the file size, use the original file instead
            if (compressedSize > originalSize) {
                Files.copy(inputPath, outputPath, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Compression increased file size. Using original file instead.");
            }
        } catch (Exception e) {
            // If any error occurs during compression, use the original file
            Files.copy(inputPath, outputPath, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Error during compression: " + e.getMessage() + ". Using original file instead.");
        }

        // Return the ID of the compressed file
        return fileId + "_compressed." + fileExtension;
    }

    private void compressVideoFile(File inputFile, File outputFile, int compressionLevel) throws Exception {
        // Load the video
        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(inputFile);
        grabber.start();
        
        // Calculate target bitrate based on compression level (1-100)
        // Higher compression level means lower bitrate
        double qualityFactor = (100 - compressionLevel) / 100.0;
        
        // Get original video parameters
        int originalWidth = grabber.getImageWidth();
        int originalHeight = grabber.getImageHeight();
        double frameRate = grabber.getVideoFrameRate();
        int originalVideoBitrate = grabber.getVideoBitrate();
        int originalAudioBitrate = grabber.getAudioBitrate();
        
        // Calculate new parameters
        int newVideoBitrate = originalVideoBitrate > 0 ? 
            (int)(originalVideoBitrate * qualityFactor) : 
            calculateDefaultVideoBitrate(originalWidth, originalHeight, qualityFactor);
            
        int newAudioBitrate = originalAudioBitrate > 0 ? 
            (int)(originalAudioBitrate * qualityFactor) : 
            128000; // Default audio bitrate
        
        // Create a recorder with the calculated parameters
        FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(
            outputFile,
            originalWidth,
            originalHeight,
            grabber.getAudioChannels()
        );
        
        // Set video parameters
        recorder.setFormat(grabber.getFormat());
        recorder.setFrameRate(frameRate);
        recorder.setVideoBitrate(newVideoBitrate);
        
        // Set audio parameters if the video has audio
        if (grabber.getAudioChannels() > 0) {
            recorder.setAudioChannels(grabber.getAudioChannels());
            recorder.setAudioBitrate(newAudioBitrate);
            recorder.setSampleRate(grabber.getSampleRate());
        }
        
        // Use H.264 for video and AAC for audio (widely compatible)
        recorder.setVideoCodec(org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_H264);
        recorder.setAudioCodec(org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_AAC);
        
        // Add quality-related options
        recorder.setVideoOption("crf", String.valueOf(Math.min(51, Math.max(18, 18 + (compressionLevel / 3)))));
        recorder.setVideoOption("preset", getPresetForQuality(compressionLevel));
        
        // Start the recorder
        recorder.start();
        
        // Process each frame
        Frame frame;
        while ((frame = grabber.grab()) != null) {
            recorder.record(frame);
        }
        
        // Close resources
        recorder.stop();
        recorder.release();
        grabber.stop();
        grabber.release();
    }
    
    private int calculateDefaultVideoBitrate(int width, int height, double qualityFactor) {
        // Calculate a reasonable bitrate based on resolution
        int baseBitrate = (width * height * 30) / 8000; // bits per pixel * 30fps / 8000
        return (int)(baseBitrate * qualityFactor);
    }
    
    private String getPresetForQuality(int compressionLevel) {
        // Map compression level to FFmpeg presets
        if (compressionLevel < 20) return "veryslow"; // Best quality, slowest
        if (compressionLevel < 40) return "slower";
        if (compressionLevel < 60) return "medium";
        if (compressionLevel < 80) return "faster";
        return "veryfast"; // Lowest quality, fastest
    }

    private String getFileExtension(String filename) {
        if (filename == null) {
            return "mp4";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "mp4";
        }
        return filename.substring(lastDotIndex + 1).toLowerCase();
    }

    public Path getCompressedFilePath(String fileName) {
        return outputDir.resolve(fileName);
    }

    public long getCompressedFileSize(String fileName) throws IOException {
        Path filePath = outputDir.resolve(fileName);
        return Files.size(filePath);
    }
    
    public long getOriginalFileSize(String fileName) throws IOException {
        // Extract the original file ID and extension from the compressed file name
        String originalFileName = fileName.replace("_compressed.", ".");
        Path filePath = uploadDir.resolve(originalFileName);
        return Files.size(filePath);
    }
    
    public boolean deleteFiles(String fileName) {
        try {
            boolean allDeleted = true;
            
            // Delete the compressed file from the output directory
            Path compressedOutputPath = outputDir.resolve(fileName);
            boolean compressedOutputDeleted = Files.deleteIfExists(compressedOutputPath);
            allDeleted = allDeleted && compressedOutputDeleted;
            
            // Also check if the compressed file exists in the upload directory (just in case)
            Path compressedUploadPath = uploadDir.resolve(fileName);
            boolean compressedUploadDeleted = Files.deleteIfExists(compressedUploadPath);
            
            // Delete the original file from the upload directory
            String originalFileName = fileName.replace("_compressed.", ".");
            Path originalFilePath = uploadDir.resolve(originalFileName);
            boolean originalDeleted = Files.deleteIfExists(originalFilePath);
            allDeleted = allDeleted && originalDeleted;
            
            // Log deletion results
            System.out.println("File deletion results:");
            System.out.println("- Compressed file (output): " + (compressedOutputDeleted ? "Deleted" : "Not found"));
            System.out.println("- Compressed file (upload): " + (compressedUploadDeleted ? "Deleted" : "Not found"));
            System.out.println("- Original file (upload): " + (originalDeleted ? "Deleted" : "Not found"));
            
            return allDeleted;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
