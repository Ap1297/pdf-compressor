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

        // Compress the PDF
        compressPDFFile(inputPath.toString(), outputPath.toString(), quality);

        // Return the ID of the compressed file
        return fileId + "_compressed.pdf";
    }

    private void compressPDFFile(String inputPath, String outputPath, float quality) throws IOException {
        // Load the PDF document
        File inputFile = new File(inputPath);
        PDDocument document = PDDocument.load(inputFile);
        
        // Create a new document for the compressed output
        PDDocument compressedDocument = new PDDocument();
        
        // Create a renderer for the original document
        PDFRenderer pdfRenderer = new PDFRenderer(document);
        
        // Process each page
        for (int pageIndex = 0; pageIndex < document.getNumberOfPages(); pageIndex++) {
            // Render the page to an image
            BufferedImage image = pdfRenderer.renderImageWithDPI(
                pageIndex, 150, ImageType.RGB);
            
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
        
        // Close both documents
        document.close();
        compressedDocument.close();
    }

    public Path getCompressedFilePath(String fileName) {
        return outputDir.resolve(fileName);
    }

    public long getCompressedFileSize(String fileName) throws IOException {
        Path filePath = outputDir.resolve(fileName);
        return Files.size(filePath);
    }
}
