package com.pdfcompressor.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import fr.opensagres.poi.xwpf.converter.pdf.PdfConverter;
import fr.opensagres.poi.xwpf.converter.pdf.PdfOptions;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class DocumentConversionService {

    private final Path uploadDir = Paths.get("uploads");
    private final Path outputDir = Paths.get("outputs");

    public DocumentConversionService() {
        try {
            Files.createDirectories(uploadDir);
            Files.createDirectories(outputDir);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directories", e);
        }
    }

    public String convertPdfToWord(MultipartFile file) throws IOException {
        // Generate unique file names
    	String fileId = file.getOriginalFilename();
        Path inputPath = uploadDir.resolve(fileId + ".pdf");
        Path outputPath = outputDir.resolve(fileId + ".docx");

        // Save the uploaded file
        Files.write(inputPath, file.getBytes());

        try {
            // Load the PDF document
            PDDocument document = PDDocument.load(inputPath.toFile());
            
            // Create a PDF text stripper
            PDFTextStripper stripper = new PDFTextStripper();
            
            // Extract text from the PDF
            String text = stripper.getText(document);
            
            // Close the PDF document
            document.close();
            
            // Create a new Word document
            XWPFDocument docx = new XWPFDocument();
            
            // Split the text by lines and add each line as a paragraph
            String[] lines = text.split("\\r?\\n");
            for (String line : lines) {
                XWPFParagraph paragraph = docx.createParagraph();
                XWPFRun run = paragraph.createRun();
                run.setText(line);
            }
            
            // Save the Word document
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                docx.write(out);
                Files.write(outputPath, out.toByteArray());
                docx.close();
            }
            
            return fileId + ".docx";
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException("Error converting PDF to Word: " + e.getMessage());
        }
    }

    public String convertWordToPdf(MultipartFile file) throws IOException {
        // Generate unique file names
        String fileId = UUID.randomUUID().toString();
        Path inputPath = uploadDir.resolve(fileId + ".docx");
        Path outputPath = outputDir.resolve(fileId + ".pdf");

        // Save the uploaded file
        Files.write(inputPath, file.getBytes());

        try {
            // Load the Word document
            XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(file.getBytes()));
            
            // Convert to PDF
            PdfOptions options = PdfOptions.create();
            ByteArrayOutputStream pdfStream = new ByteArrayOutputStream();
            PdfConverter.getInstance().convert(document, pdfStream, options);
            
            // Save the PDF
            Files.write(outputPath, pdfStream.toByteArray());
            
            // Close resources
            document.close();
            pdfStream.close();
            
            return fileId + ".pdf";
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException("Error converting Word to PDF: " + e.getMessage());
        }
    }

    public Path getConvertedFilePath(String fileName) {
        return outputDir.resolve(fileName);
    }
    
    public boolean deleteFiles(String fileName) {
        try {
            boolean allDeleted = true;
            
            // Delete the converted file from the output directory
            Path outputPath = outputDir.resolve(fileName);
            boolean outputDeleted = Files.deleteIfExists(outputPath);
            allDeleted = allDeleted && outputDeleted;
            
            // Delete the original file from the upload directory
            // Determine the original file name based on the extension
            String originalFileName;
            if (fileName.endsWith(".pdf")) {
                originalFileName = fileName.replace(".pdf", ".docx");
            } else {
                originalFileName = fileName.replace(".docx", ".pdf");
            }
            
            Path originalFilePath = uploadDir.resolve(originalFileName);
            boolean originalDeleted = Files.deleteIfExists(originalFilePath);
            allDeleted = allDeleted && originalDeleted;
            
            // Log deletion results
            System.out.println("File deletion results:");
            System.out.println("- Converted file: " + (outputDeleted ? "Deleted" : "Not found"));
            System.out.println("- Original file: " + (originalDeleted ? "Deleted" : "Not found"));
            
            return allDeleted;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
