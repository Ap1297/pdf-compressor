package com.pdfcompressor;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class PDFCompressor {

    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: java PDFCompressor <inputPath> <outputPath> <quality>");
            System.exit(1);
        }

        String inputPath = args[0];
        String outputPath = args[1];
        float quality = Float.parseFloat(args[2]);

        try {
            compressPDF(inputPath, outputPath, quality);
            System.out.println("PDF compressed successfully!");
        } catch (IOException e) {
            System.err.println("Error compressing PDF: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void compressPDF(String inputPath, String outputPath, float quality) throws IOException {
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
            
            // Print progress
            System.out.println("Processed page " + (pageIndex + 1) + " of " + document.getNumberOfPages());
        }
        
        // Save the compressed document
        compressedDocument.save(outputPath);
        
        // Close both documents
        document.close();
        compressedDocument.close();
    }
}
