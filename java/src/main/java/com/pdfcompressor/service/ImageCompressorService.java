package com.pdfcompressor.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.UUID;

@Service
public class ImageCompressorService {

    private final Path uploadDir = Paths.get("uploads");
    private final Path outputDir = Paths.get("outputs");

    public ImageCompressorService() {
        try {
            Files.createDirectories(uploadDir);
            Files.createDirectories(outputDir);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directories", e);
        }
    }

    public String compressImage(MultipartFile file, float quality) throws IOException {
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
            // Compress the image
            compressImageFile(inputPath.toString(), outputPath.toString(), fileExtension, quality);

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

    private void compressImageFile(String inputPath, String outputPath, String formatName, float quality) throws IOException {
        // Read the image
        BufferedImage image = ImageIO.read(new java.io.File(inputPath));
        
        // For PNG files with transparency, we need special handling
        if (formatName.equalsIgnoreCase("png")) {
            compressPNG(image, outputPath, quality);
            return;
        }
        
        // For other formats, use standard compression
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(formatName);
        
        if (!writers.hasNext()) {
            throw new IOException("No writer found for format: " + formatName);
        }
        
        ImageWriter writer = writers.next();
        ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(outputStream);
        writer.setOutput(imageOutputStream);
        
        ImageWriteParam param = writer.getDefaultWriteParam();
        
        // Not all formats support compression
        if (param.canWriteCompressed()) {
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality);
        }
        
        writer.write(null, new IIOImage(image, null, null), param);
        writer.dispose();
        imageOutputStream.close();
        
        // Write the compressed image to file
        byte[] compressedImageData = outputStream.toByteArray();
        Files.write(Paths.get(outputPath), compressedImageData);
    }
    
    private void compressPNG(BufferedImage image, String outputPath, float quality) throws IOException {
        // For PNG, we don't use compression quality directly
        // Instead, we can reduce the color depth or use a different compression method
        
        // Convert to a format that can be compressed, then back to PNG
        ByteArrayOutputStream jpgOutput = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", jpgOutput);
        
        // Read the JPG back in (this loses transparency)
        BufferedImage jpgImage = ImageIO.read(new ByteArrayInputStream(jpgOutput.toByteArray()));
        
        // Write as PNG
        ImageIO.write(jpgImage, "png", new java.io.File(outputPath));
    }
    
    private String getFileExtension(String filename) {
        if (filename == null) {
            return "jpg";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "jpg";
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
