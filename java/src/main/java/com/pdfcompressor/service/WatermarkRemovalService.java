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
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
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
        String fileId = file.getOriginalFilename();
        
        Path inputPath = uploadDir.resolve(fileId + "." + fileExtension);
        Path outputPath = outputDir.resolve(fileId + "_nowatermark." + fileExtension);

        // Save the uploaded file
        Files.write(inputPath, file.getBytes());

        try {
            // Process the image to remove watermark
            BufferedImage originalImage = ImageIO.read(inputPath.toFile());
            
            // Use a different approach based on the threshold parameter
            BufferedImage processedImage;
            if (threshold > 150) {
                // For lighter watermarks, use color filtering approach
                processedImage = removeWatermarkByColorFiltering(originalImage, threshold, tolerance);
            } else {
                // For darker watermarks, use edge detection and reconstruction
                processedImage = removeWatermarkByEdgeReconstruction(originalImage, threshold, tolerance);
            }
            
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
                BufferedImage processedImage;
                if (threshold > 150) {
                    // For lighter watermarks, use color filtering approach
                    processedImage = removeWatermarkByColorFiltering(pageImage, threshold, tolerance);
                } else {
                    // For darker watermarks, use edge detection and reconstruction
                    processedImage = removeWatermarkByEdgeReconstruction(pageImage, threshold, tolerance);
                }
                
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

    private BufferedImage removeWatermarkByColorFiltering(BufferedImage image, int threshold, int tolerance) {
        int width = image.getWidth();
        int height = image.getHeight();
        
        // Create a new image for the result
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        
        // Step 1: Analyze the image to find dominant colors (potential watermark colors)
        int[] colorHistogram = new int[256]; // Simplified grayscale histogram
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color pixelColor = new Color(image.getRGB(x, y));
                int brightness = (pixelColor.getRed() + pixelColor.getGreen() + pixelColor.getBlue()) / 3;
                colorHistogram[brightness]++;
            }
        }
        
        // Find peaks in the histogram (potential watermark colors)
        boolean[] isPotentialWatermarkColor = new boolean[256];
        for (int i = 1; i < 255; i++) {
            // A peak is where the value is higher than its neighbors
            if (colorHistogram[i] > colorHistogram[i-1] && colorHistogram[i] > colorHistogram[i+1]) {
                // Only consider peaks in the upper brightness range for light watermarks
                if (i > threshold - 30 && i < threshold + 30) {
                    isPotentialWatermarkColor[i] = true;
                }
            }
        }
        
        // Step 2: Create a mask for potential watermark areas
        BufferedImage watermarkMask = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color pixelColor = new Color(image.getRGB(x, y));
                int brightness = (pixelColor.getRed() + pixelColor.getGreen() + pixelColor.getBlue()) / 3;
                
                // Check if this pixel has a color similar to a potential watermark color
                boolean isWatermarkPixel = false;
                for (int i = Math.max(0, brightness - tolerance); i <= Math.min(255, brightness + tolerance); i++) {
                    if (isPotentialWatermarkColor[i]) {
                        isWatermarkPixel = true;
                        break;
                    }
                }
                
                // Also check for semi-transparent characteristics
                boolean isSemiTransparent = isSemiTransparentPixel(pixelColor, tolerance);
                
                if (isWatermarkPixel || isSemiTransparent) {
                    watermarkMask.setRGB(x, y, Color.WHITE.getRGB());
                } else {
                    watermarkMask.setRGB(x, y, Color.BLACK.getRGB());
                }
            }
        }
        
        // Step 3: Apply morphological operations to improve the mask
        watermarkMask = applyMorphologicalOperations(watermarkMask);
        
        // Step 4: Remove the watermark by replacing watermark pixels
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if ((watermarkMask.getRGB(x, y) & 0xFF) > 200) {
                    // This is a watermark pixel - replace it
                    Color replacementColor = getReplacementColor(image, watermarkMask, x, y);
                    result.setRGB(x, y, replacementColor.getRGB());
                } else {
                    // This is not a watermark pixel - keep the original
                    result.setRGB(x, y, image.getRGB(x, y));
                }
            }
        }
        
        // Step 5: Apply post-processing to blend the result
        result = applyPostProcessing(result);
        
        return result;
    }
    
    private boolean isSemiTransparentPixel(Color color, int tolerance) {
        // Check for characteristics of semi-transparent pixels
        // Semi-transparent pixels often have a specific color cast or brightness pattern
        
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();
        
        // Check if the color channels are very close to each other (indicating gray/white with transparency)
        boolean isNearlyGray = Math.abs(r - g) < tolerance && Math.abs(r - b) < tolerance && Math.abs(g - b) < tolerance;
        
        // Check if the color has a specific cast that might indicate a watermark
        // For example, many watermarks have a slight blue or gray cast
        boolean hasColorCast = false;
        
        // Example: check for bluish cast
        if (b > r + tolerance && b > g + tolerance) {
            hasColorCast = true;
        }
        
        // Example: check for reddish cast
        if (r > b + tolerance && r > g + tolerance) {
            hasColorCast = true;
        }
        
        return isNearlyGray || hasColorCast;
    }
    
    private BufferedImage removeWatermarkByEdgeReconstruction(BufferedImage image, int threshold, int tolerance) {
        int width = image.getWidth();
        int height = image.getHeight();
        
        // Create a new image for the result
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        
        // Step 1: Convert to grayscale for edge detection
        BufferedImage grayscale = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color pixelColor = new Color(image.getRGB(x, y));
                int gray = (pixelColor.getRed() + pixelColor.getGreen() + pixelColor.getBlue()) / 3;
                grayscale.setRGB(x, y, new Color(gray, gray, gray).getRGB());
            }
        }
        
        // Step 2: Apply edge detection
        BufferedImage edges = applyEdgeDetection(grayscale);
        
        // Step 3: Identify potential watermark regions (areas with fewer edges)
        BufferedImage watermarkMask = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // Check the local edge density
                int edgeDensity = calculateLocalEdgeDensity(edges, x, y, 5);
                
                // Areas with low edge density but moderate brightness might be watermarks
                Color originalColor = new Color(image.getRGB(x, y));
                int brightness = (originalColor.getRed() + originalColor.getGreen() + originalColor.getBlue()) / 3;
                
                // Adjust these thresholds based on the watermark characteristics
                boolean isPotentialWatermark = edgeDensity < tolerance && 
                                              brightness > threshold - 50 && 
                                              brightness < threshold + 50;
                
                if (isPotentialWatermark) {
                    watermarkMask.setRGB(x, y, Color.WHITE.getRGB());
                } else {
                    watermarkMask.setRGB(x, y, Color.BLACK.getRGB());
                }
            }
        }
        
        // Step 4: Apply morphological operations to improve the mask
        watermarkMask = applyMorphologicalOperations(watermarkMask);
        
        // Step 5: Remove the watermark by texture-aware inpainting
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if ((watermarkMask.getRGB(x, y) & 0xFF) > 200) {
                    // This is a watermark pixel - replace it with texture-aware inpainting
                    Color replacementColor = getTextureAwareReplacement(image, watermarkMask, x, y);
                    result.setRGB(x, y, replacementColor.getRGB());
                } else {
                    // This is not a watermark pixel - keep the original
                    result.setRGB(x, y, image.getRGB(x, y));
                }
            }
        }
        
        // Step 6: Apply post-processing to blend the result
        result = applyPostProcessing(result);
        
        return result;
    }
    
    private BufferedImage applyEdgeDetection(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        
        // Sobel operator for edge detection
        float[] sobelX = {
            -1, 0, 1,
            -2, 0, 2,
            -1, 0, 1
        };
        
        float[] sobelY = {
            -1, -2, -1,
             0,  0,  0,
             1,  2,  1
        };
        
        // Apply Sobel operators
        ConvolveOp sobelXOp = new ConvolveOp(new Kernel(3, 3, sobelX), ConvolveOp.EDGE_NO_OP, null);
        ConvolveOp sobelYOp = new ConvolveOp(new Kernel(3, 3, sobelY), ConvolveOp.EDGE_NO_OP, null);
        
        BufferedImage gradientX = sobelXOp.filter(image, null);
        BufferedImage gradientY = sobelYOp.filter(image, null);
        
        // Combine the gradients
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int gx = gradientX.getRGB(x, y) & 0xFF;
                int gy = gradientY.getRGB(x, y) & 0xFF;
                
                // Calculate gradient magnitude
                int magnitude = (int)Math.sqrt(gx*gx + gy*gy);
                magnitude = Math.min(255, magnitude);
                
                result.setRGB(x, y, new Color(magnitude, magnitude, magnitude).getRGB());
            }
        }
        
        return result;
    }
    
    private int calculateLocalEdgeDensity(BufferedImage edges, int x, int y, int radius) {
        int width = edges.getWidth();
        int height = edges.getHeight();
        
        int totalEdgeStrength = 0;
        int pixelCount = 0;
        
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int nx = x + dx;
                int ny = y + dy;
                
                if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                    totalEdgeStrength += edges.getRGB(nx, ny) & 0xFF;
                    pixelCount++;
                }
            }
        }
        
        return pixelCount > 0 ? totalEdgeStrength / pixelCount : 0;
    }
    
    private BufferedImage applyMorphologicalOperations(BufferedImage mask) {
        int width = mask.getWidth();
        int height = mask.getHeight();
        
        // Apply dilation to connect nearby watermark pixels
        BufferedImage dilated = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // Check if any pixel in the 3x3 neighborhood is white
                boolean hasWhiteNeighbor = false;
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        int nx = x + dx;
                        int ny = y + dy;
                        
                        if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                            if ((mask.getRGB(nx, ny) & 0xFF) > 200) {
                                hasWhiteNeighbor = true;
                                break;
                            }
                        }
                    }
                    if (hasWhiteNeighbor) break;
                }
                
                if (hasWhiteNeighbor) {
                    dilated.setRGB(x, y, Color.WHITE.getRGB());
                } else {
                    dilated.setRGB(x, y, Color.BLACK.getRGB());
                }
            }
        }
        
        // Apply erosion to remove small isolated areas
        BufferedImage eroded = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // Check if all pixels in the 3x3 neighborhood are white
                boolean allWhiteNeighbors = true;
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        int nx = x + dx;
                        int ny = y + dy;
                        
                        if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                            if ((dilated.getRGB(nx, ny) & 0xFF) <= 200) {
                                allWhiteNeighbors = false;
                                break;
                            }
                        }
                    }
                    if (!allWhiteNeighbors) break;
                }
                
                if (allWhiteNeighbors) {
                    eroded.setRGB(x, y, Color.WHITE.getRGB());
                } else {
                    eroded.setRGB(x, y, Color.BLACK.getRGB());
                }
            }
        }
        
        return eroded;
    }
    
    private Color getReplacementColor(BufferedImage image, BufferedImage mask, int x, int y) {
        int width = image.getWidth();
        int height = image.getHeight();
        
        // Find the nearest non-watermark pixels in each direction
        Color[] nearestColors = new Color[8]; // 8 directions
        int[] distances = new int[8];
        for (int i = 0; i < 8; i++) {
            distances[i] = Integer.MAX_VALUE;
        }
        
        // Define the 8 directions (N, NE, E, SE, S, SW, W, NW)
        int[] dx = {0, 1, 1, 1, 0, -1, -1, -1};
        int[] dy = {-1, -1, 0, 1, 1, 1, 0, -1};
        
        // Search for the nearest non-watermark pixel in each direction
        for (int dir = 0; dir < 8; dir++) {
            for (int dist = 1; dist < 50; dist++) { // Limit search distance
                int nx = x + dx[dir] * dist;
                int ny = y + dy[dir] * dist;
                
                if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                    if ((mask.getRGB(nx, ny) & 0xFF) <= 200) {
                        // Found a non-watermark pixel
                        nearestColors[dir] = new Color(image.getRGB(nx, ny));
                        distances[dir] = dist;
                        break;
                    }
                } else {
                    // Out of bounds
                    break;
                }
            }
        }
        
        // Calculate the weighted average of the nearest colors
        int totalWeight = 0;
        int weightedR = 0, weightedG = 0, weightedB = 0;
        
        for (int dir = 0; dir < 8; dir++) {
            if (distances[dir] < Integer.MAX_VALUE) {
                int weight = 100 / distances[dir]; // Weight inversely proportional to distance
                totalWeight += weight;
                
                weightedR += nearestColors[dir].getRed() * weight;
                weightedG += nearestColors[dir].getGreen() * weight;
                weightedB += nearestColors[dir].getBlue() * weight;
            }
        }
        
        if (totalWeight > 0) {
            int avgR = weightedR / totalWeight;
            int avgG = weightedG / totalWeight;
            int avgB = weightedB / totalWeight;
            
            return new Color(avgR, avgG, avgB);
        } else {
            // Fallback: use the original color
            return new Color(image.getRGB(x, y));
        }
    }
    
    private Color getTextureAwareReplacement(BufferedImage image, BufferedImage mask, int x, int y) {
        int width = image.getWidth();
        int height = image.getHeight();
        
        // Find the best matching texture patch from non-watermark areas
        int patchSize = 5; // Size of the texture patch
        int bestMatchX = -1, bestMatchY = -1;
        double bestMatchScore = Double.MAX_VALUE;
        
        // Get the context around the current pixel
        int[] contextPattern = getContextPattern(image, mask, x, y, patchSize);
        
        // Search for the best matching texture patch
        for (int sy = patchSize; sy < height - patchSize; sy += 3) { // Skip some pixels for efficiency
            for (int sx = patchSize; sx < width - patchSize; sx += 3) {
                // Only consider non-watermark areas
                if ((mask.getRGB(sx, sy) & 0xFF) <= 200) {
                    // Get the pattern at this location
                    int[] candidatePattern = getFullPattern(image, sx, sy, patchSize);
                    
                    // Calculate the match score
                    double matchScore = calculatePatternMatchScore(contextPattern, candidatePattern);
                    
                    if (matchScore < bestMatchScore) {
                        bestMatchScore = matchScore;
                        bestMatchX = sx;
                        bestMatchY = sy;
                    }
                }
            }
        }
        
        if (bestMatchX >= 0) {
            // Use the center pixel of the best matching patch
            return new Color(image.getRGB(bestMatchX, bestMatchY));
        } else {
            // Fallback: use a simple color-based replacement
            return getReplacementColor(image, mask, x, y);
        }
    }
    
    private int[] getContextPattern(BufferedImage image, BufferedImage mask, int x, int y, int patchSize) {
        int width = image.getWidth();
        int height = image.getHeight();
        int radius = patchSize / 2;
        
        // Create a pattern that includes only the non-watermark pixels in the context
        int[] pattern = new int[patchSize * patchSize * 3]; // RGB values
        Arrays.fill(pattern, -1); // -1 indicates unknown (watermark) pixels
        
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int nx = x + dx;
                int ny = y + dy;
                
                if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                    if ((mask.getRGB(nx, ny) & 0xFF) <= 200) {
                        // This is a non-watermark pixel - include it in the pattern
                        Color pixelColor = new Color(image.getRGB(nx, ny));
                        int patternIndex = ((dy + radius) * patchSize + (dx + radius)) * 3;
                        
                        pattern[patternIndex] = pixelColor.getRed();
                        pattern[patternIndex + 1] = pixelColor.getGreen();
                        pattern[patternIndex + 2] = pixelColor.getBlue();
                    }
                }
            }
        }
        
        return pattern;
    }
    
    private int[] getFullPattern(BufferedImage image, int x, int y, int patchSize) {
        int width = image.getWidth();
        int height = image.getHeight();
        int radius = patchSize / 2;
        
        // Create a pattern that includes all pixels in the patch
        int[] pattern = new int[patchSize * patchSize * 3]; // RGB values
        
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                int nx = x + dx;
                int ny = y + dy;
                
                if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                    Color pixelColor = new Color(image.getRGB(nx, ny));
                    int patternIndex = ((dy + radius) * patchSize + (dx + radius)) * 3;
                    
                    pattern[patternIndex] = pixelColor.getRed();
                    pattern[patternIndex + 1] = pixelColor.getGreen();
                    pattern[patternIndex + 2] = pixelColor.getBlue();
                } else {
                    // Out of bounds - use -1 to indicate unknown
                    int patternIndex = ((dy + radius) * patchSize + (dx + radius)) * 3;
                    pattern[patternIndex] = -1;
                    pattern[patternIndex + 1] = -1;
                    pattern[patternIndex + 2] = -1;
                }
            }
        }
        
        return pattern;
    }
    
    private double calculatePatternMatchScore(int[] contextPattern, int[] candidatePattern) {
        double totalDifference = 0;
        int validPixels = 0;
        
        for (int i = 0; i < contextPattern.length; i += 3) {
            if (contextPattern[i] >= 0 && candidatePattern[i] >= 0) {
                // Both patterns have valid values for this pixel
                int rDiff = contextPattern[i] - candidatePattern[i];
                int gDiff = contextPattern[i+1] - candidatePattern[i+1];
                int bDiff = contextPattern[i+2] - candidatePattern[i+2];
                
                totalDifference += Math.sqrt(rDiff*rDiff + gDiff*gDiff + bDiff*bDiff);
                validPixels++;
            }
        }
        
        return validPixels > 0 ? totalDifference / validPixels : Double.MAX_VALUE;
    }
    
    private BufferedImage applyPostProcessing(BufferedImage image) {
        // Apply a slight blur to blend the inpainted areas
        float[] blurKernel = {
            1/16f, 1/8f, 1/16f,
            1/8f,  1/4f, 1/8f,
            1/16f, 1/8f, 1/16f
        };
        
        ConvolveOp blurOp = new ConvolveOp(new Kernel(3, 3, blurKernel), ConvolveOp.EDGE_NO_OP, null);
        return blurOp.filter(image, null);
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
