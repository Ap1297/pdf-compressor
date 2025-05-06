package com.pdfcompressor.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class WatermarkRemovalService {

    private final Path uploadDir = Paths.get("uploads");
    private final Path outputDir = Paths.get("outputs");

    public WatermarkRemovalService() {
        try {
            Files.createDirectories(uploadDir);
            Files.createDirectories(outputDir);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directories", e);
        }
    }

    public String removeWatermarkFromImage(MultipartFile file, int threshold, int tolerance) throws IOException {
        // Generate unique file names
        String originalFileName = file.getOriginalFilename();
        String fileExtension = getFileExtension(originalFileName);
        String fileId = UUID.randomUUID().toString();
        
        Path inputPath = uploadDir.resolve(fileId + "." + fileExtension);
        Path outputPath = outputDir.resolve(fileId + "_nowatermark." + fileExtension);

        // Save the uploaded file
        Files.write(inputPath, file.getBytes());

        try {
            // Process the image to remove watermark
            BufferedImage originalImage = ImageIO.read(inputPath.toFile());
            BufferedImage processedImage = processImageForWatermarkRemoval(originalImage, threshold, tolerance);
            
            // Save the processed image
            ImageIO.write(processedImage, fileExtension, outputPath.toFile());
            
            return fileId + "_nowatermark." + fileExtension;
        } catch (Exception e) {
            e.printStackTrace();
            // If any error occurs, use the original file
            Files.copy(inputPath, outputPath, StandardCopyOption.REPLACE_EXISTING);
            return fileId + "_nowatermark." + fileExtension;
        }
    }

    public String removeWatermarkFromPDF(MultipartFile file, int threshold, int tolerance) throws IOException {
        // Generate unique file names
        String fileId = file.getOriginalFilename();
        Path inputPath = uploadDir.resolve(fileId + ".pdf");
        Path outputPath = outputDir.resolve(fileId + "_nowatermark.pdf");

        // Save the uploaded file
        Files.write(inputPath, file.getBytes());

        try {
            // Load the PDF document
            PDDocument document = PDDocument.load(inputPath.toFile());
            PDDocument processedDocument = new PDDocument();
            
            // Create a renderer for the original document
            PDFRenderer pdfRenderer = new PDFRenderer(document);
            
            // Process each page
            for (int pageIndex = 0; pageIndex < document.getNumberOfPages(); pageIndex++) {
                // Render the page to an image
                BufferedImage pageImage = pdfRenderer.renderImageWithDPI(
                    pageIndex, 300, ImageType.RGB);
                
                // Process the image to remove watermark
                BufferedImage processedImage = processImageForWatermarkRemoval(pageImage, threshold, tolerance);
                
                // Create a new page with the same dimensions
                PDPage originalPage = document.getPage(pageIndex);
                PDRectangle mediaBox = originalPage.getMediaBox();
                PDPage newPage = new PDPage(new PDRectangle(mediaBox.getWidth(), mediaBox.getHeight()));
                processedDocument.addPage(newPage);
                
                // Convert the processed image back to PDF
                PDImageXObject pdImage = JPEGFactory.createFromImage(
                    processedDocument, processedImage, 0.9f);
                
                // Draw the processed image on the new page
                PDPageContentStream contentStream = new PDPageContentStream(processedDocument, newPage);
                contentStream.drawImage(pdImage, 0, 0, mediaBox.getWidth(), mediaBox.getHeight());
                contentStream.close();
            }
            
            // Save the processed document
            processedDocument.save(outputPath.toFile());
            
            // Close both documents
            document.close();
            processedDocument.close();
            
            return fileId + "_nowatermark.pdf";
        } catch (Exception e) {
            e.printStackTrace();
            // If any error occurs, use the original file
            Files.copy(inputPath, outputPath, StandardCopyOption.REPLACE_EXISTING);
            return fileId + "_nowatermark.pdf";
        }
    }

    private BufferedImage processImageForWatermarkRemoval(BufferedImage image, int threshold, int tolerance) {
        int width = image.getWidth();
        int height = image.getHeight();
        
        // Create a new image for the result
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        
        // Process each pixel
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color pixelColor = new Color(image.getRGB(x, y));
                
                // Check if this pixel might be part of a watermark
                if (isLikelyWatermark(pixelColor, threshold, tolerance)) {
                    // Replace with background color (estimated from surroundings)
                    Color replacementColor = estimateBackgroundColor(image, x, y);
                    result.setRGB(x, y, replacementColor.getRGB());
                } else {
                    // Keep the original pixel
                    result.setRGB(x, y, image.getRGB(x, y));
                }
            }
        }
        
        return result;
    }
    
    private boolean isLikelyWatermark(Color color, int threshold, int tolerance) {
        // This is a simplified approach - watermarks are often semi-transparent
        // or have specific color characteristics
        
        // Check if the color is close to a typical watermark color (light gray/white with transparency)
        int brightness = (color.getRed() + color.getGreen() + color.getBlue()) / 3;
        
        // Check if the color has high brightness (typical for light watermarks)
        boolean isLight = brightness > threshold;
        
        // Check if the color channels are close to each other (typical for gray watermarks)
        boolean isGrayish = Math.abs(color.getRed() - color.getGreen()) < tolerance &&
                           Math.abs(color.getRed() - color.getBlue()) < tolerance &&
                           Math.abs(color.getGreen() - color.getBlue()) < tolerance;
        
        return isLight && isGrayish;
    }
    
    private Color estimateBackgroundColor(BufferedImage image, int x, int y) {
        // This is a simplified approach - we take the average color from surrounding pixels
        // A more sophisticated approach would use inpainting algorithms
        
        int width = image.getWidth();
        int height = image.getHeight();
        
        int sampleSize = 5; // Sample size for surrounding pixels
        int totalSamples = 0;
        int redSum = 0, greenSum = 0, blueSum = 0;
        
        // Sample surrounding pixels
        for (int dy = -sampleSize; dy <= sampleSize; dy++) {
            for (int dx = -sampleSize; dx <= sampleSize; dx++) {
                // Skip the center pixel
                if (dx == 0 && dy == 0) continue;
                
                // Check if the sample pixel is within bounds
                int sx = x + dx;
                int sy = y + dy;
                if (sx >= 0 && sx < width && sy >= 0 && sy < height) {
                    Color sampleColor = new Color(image.getRGB(sx, sy));
                    
                    // Only use samples that are not likely watermarks themselves
                    if (!isLikelyWatermark(sampleColor, 200, 30)) {
                        redSum += sampleColor.getRed();
                        greenSum += sampleColor.getGreen();
                        blueSum += sampleColor.getBlue();
                        totalSamples++;
                    }
                }
            }
        }
        
        // If we couldn't find any suitable samples, return the original color
        if (totalSamples == 0) {
            return new Color(image.getRGB(x, y));
        }
        
        // Calculate the average color
        int avgRed = redSum / totalSamples;
        int avgGreen = greenSum / totalSamples;
        int avgBlue = blueSum / totalSamples;
        
        return new Color(avgRed, avgGreen, avgBlue);
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

    public Path getProcessedFilePath(String fileName) {
        return outputDir.resolve(fileName);
    }
    
    public boolean deleteFiles(String fileName) {
        try {
            boolean allDeleted = true;
            
            // Delete the processed file from the output directory
            Path processedOutputPath = outputDir.resolve(fileName);
            boolean processedOutputDeleted = Files.deleteIfExists(processedOutputPath);
            allDeleted = allDeleted && processedOutputDeleted;
            
            // Delete the original file from the upload directory
            String originalFileName = fileName.replace("_nowatermark.", ".");
            Path originalFilePath = uploadDir.resolve(originalFileName);
            boolean originalDeleted = Files.deleteIfExists(originalFilePath);
            allDeleted = allDeleted && originalDeleted;
            
            // Log deletion results
            System.out.println("File deletion results:");
            System.out.println("- Processed file (output): " + (processedOutputDeleted ? "Deleted" : "Not found"));
            System.out.println("- Original file (upload): " + (originalDeleted ? "Deleted" : "Not found"));
            
            return allDeleted;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
