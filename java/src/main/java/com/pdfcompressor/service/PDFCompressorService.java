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

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class PDFCompressorService {

    private final Path uploadDir = Paths.get("uploads");
    private final Path outputDir = Paths.get("outputs");

    public PDFCompressorService() {
        try {
            Files.createDirectories(uploadDir);
            Files.createDirectories(outputDir);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directories", e);
        }
    }

    public String compressPDF(MultipartFile file, float quality) throws IOException {
        // Generate unique file names
        String fileId = UUID.randomUUID().toString();
        Path inputPath = uploadDir.resolve(fileId + ".pdf");
        Path outputPath = outputDir.resolve(fileId + "_compressed.pdf");

        // Save the uploaded file
        Files.write(inputPath, file.getBytes());

        // Get original file size
        long originalSize = Files.size(inputPath);

        try {
            // Compress the PDF
            compressPDFFile(inputPath.toString(), outputPath.toString(), quality);

            // Check if compression actually reduced the file size
            long compressedSize = Files.size(outputPath);
            
            // If compression increased the file size, use the original file instead
            if (compressedSize > originalSize) {
                // Use REPLACE_EXISTING to handle the case where the file already exists
                Files.copy(inputPath, outputPath, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("Compression increased file size. Using original file instead.");
            }
        } catch (Exception e) {
            // If any error occurs during compression, use the original file
            Files.copy(inputPath, outputPath, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Error during compression: " + e.getMessage() + ". Using original file instead.");
        }

        // Return the ID of the compressed file
        return fileId + "_compressed.pdf";
    }

    private void compressPDFFile(String inputPath, String outputPath, float quality) throws IOException {
        // Load the PDF document
        File inputFile = new File(inputPath);
        PDDocument document = PDDocument.load(inputFile);
        
        try {
            // Try different compression strategies based on the quality parameter
            if (quality < 0.5f) {
                // For higher compression (lower quality), use image-based compression
                compressWithImageConversion(document, outputPath, quality);
            } else {
                // For higher quality, use PDF/A optimization which preserves quality better
                compressWithPDFOptimization(document, outputPath);
            }
        } finally {
            // Close the document
            document.close();
        }
    }
    
    private void compressWithImageConversion(PDDocument document, String outputPath, float quality) throws IOException {
        // Create a new document for the compressed output
        PDDocument compressedDocument = new PDDocument();
        
        try {
            // Create a renderer for the original document
            PDFRenderer pdfRenderer = new PDFRenderer(document);
            
            // Calculate appropriate DPI based on quality
            // Lower quality = lower DPI = smaller file
            int dpi = Math.max(72, Math.min(150, (int)(72 + (quality * 78))));
            
            // Process each page
            for (int pageIndex = 0; pageIndex < document.getNumberOfPages(); pageIndex++) {
                // Render the page to an image
                BufferedImage image = pdfRenderer.renderImageWithDPI(
                    pageIndex, dpi, ImageType.RGB);
                
                // Create a JPEG from the image with the specified quality
                PDImageXObject pdImage = JPEGFactory.createFromImage(
                    compressedDocument, image, quality);
                
                // Get the original page dimensions
                PDPage originalPage = document.getPage(pageIndex);
                PDRectangle mediaBox = originalPage.getMediaBox();
                
                // Create a new page with the same dimensions
                PDPage newPage = new PDPage(new PDRectangle(mediaBox.getWidth(), mediaBox.getHeight()));
                compressedDocument.addPage(newPage);
                
                // Draw the compressed image on the new page
                PDPageContentStream contentStream = new PDPageContentStream(compressedDocument, newPage);
                contentStream.drawImage(pdImage, 0, 0, mediaBox.getWidth(), mediaBox.getHeight());
                contentStream.close();
            }
            
            // Save the compressed document
            compressedDocument.save(outputPath);
        } finally {
            // Close the compressed document
            compressedDocument.close();
        }
    }
    
    private void compressWithPDFOptimization(PDDocument document, String outputPath) throws IOException {
        // This method preserves the original PDF structure but applies some optimizations
        
        // Set the version to PDF 1.5 which has better compression
        document.setVersion(1.5f);
        
        // Remove metadata to reduce size
        document.setDocumentInformation(new org.apache.pdfbox.pdmodel.PDDocumentInformation());
        
        // Save with compression enabled
        document.save(outputPath);
    }

    public Path getCompressedFilePath(String fileName) {
        return outputDir.resolve(fileName);
    }

    public long getCompressedFileSize(String fileName) throws IOException {
        Path filePath = outputDir.resolve(fileName);
        return Files.size(filePath);
    }
    
    public long getOriginalFileSize(String fileName) throws IOException {
        // Extract the original file ID from the compressed file name
        String originalFileName = fileName.replace("_compressed.pdf", ".pdf");
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
            String originalFileName = fileName.replace("_compressed.pdf", ".pdf");
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
