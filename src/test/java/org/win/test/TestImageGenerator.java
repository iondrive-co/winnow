package org.win.test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;

/**
 * Utility class for generating synthetic test images of various sizes.
 */
public final class TestImageGenerator {

    private TestImageGenerator() {
        // Utility class
    }
    private static final int[][] IMAGE_SIZES = {
        {400, 300},   // Image 0
        {600, 400},   // Image 1
        {800, 600},   // Image 2
        {1024, 768},  // Image 3
        {1280, 720},  // Image 4
        {1920, 1080}, // Image 5
        {500, 500},   // Image 6 (square)
        {300, 600},   // Image 7 (portrait)
        {640, 480},   // Image 8
        {1600, 900}   // Image 9
    };

    /**
     * Generates a synthetic test image with custom dimensions.
     *
     * @param imageIndex Index for color pattern (0-9)
     * @param outputFile Target file to save the image
     * @param width Image width in pixels
     * @param height Image height in pixels
     * @return The created file
     * @throws IOException If image creation fails
     */
    public static File generateTestImage(final int imageIndex, final File outputFile, final int width, final int height) throws IOException {
        final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        final Graphics2D g2d = image.createGraphics();

        // Set rendering hints for quality
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Generate unique color based on index
        final Color backgroundColor = generateColor(imageIndex);
        g2d.setColor(backgroundColor);
        g2d.fillRect(0, 0, width, height);

        // Add some pattern/content
        drawPattern(g2d, imageIndex, width, height);

        // Add text overlay with image info
        g2d.setColor(Color.WHITE);
        final int fontSize = Math.max(20, Math.min(width, height) / 20);
        g2d.setFont(new Font("Arial", Font.BOLD, fontSize));
        final String text = "Test Image " + imageIndex;
        final String sizeText = width + "x" + height;

        final FontMetrics fm = g2d.getFontMetrics();
        final int textWidth = fm.stringWidth(text);
        final int textHeight = fm.getHeight();

        g2d.drawString(text, (width - textWidth) / 2, height / 2 - textHeight / 2);
        g2d.drawString(sizeText, (width - fm.stringWidth(sizeText)) / 2, height / 2 + textHeight / 2);

        g2d.dispose();

        // Save as JPEG
        ImageIO.write(image, "jpg", outputFile);
        return outputFile;
    }

    /**
     * Generates a synthetic test image with the specified index (0-9).
     * Each image has a unique size and color pattern.
     *
     * @param imageIndex Index of the image (0-9)
     * @param outputFile Target file to save the image
     * @return The created file
     * @throws IOException If image creation fails
     */
    public static File generateTestImage(int imageIndex, File outputFile) throws IOException {
        if (imageIndex < 0 || imageIndex >= IMAGE_SIZES.length) {
            throw new IllegalArgumentException("Image index must be between 0 and " + (IMAGE_SIZES.length - 1));
        }

        int width = IMAGE_SIZES[imageIndex][0];
        int height = IMAGE_SIZES[imageIndex][1];

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        // Set rendering hints for quality
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Generate unique color based on index
        Color backgroundColor = generateColor(imageIndex);
        g2d.setColor(backgroundColor);
        g2d.fillRect(0, 0, width, height);

        // Add some pattern/content
        drawPattern(g2d, imageIndex, width, height);

        // Add text overlay with image info
        g2d.setColor(Color.WHITE);
        int fontSize = Math.max(20, Math.min(width, height) / 20);
        g2d.setFont(new Font("Arial", Font.BOLD, fontSize));
        String text = "Test Image " + imageIndex;
        String sizeText = width + "x" + height;

        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getHeight();

        g2d.drawString(text, (width - textWidth) / 2, height / 2 - textHeight / 2);
        g2d.drawString(sizeText, (width - fm.stringWidth(sizeText)) / 2, height / 2 + textHeight / 2);

        g2d.dispose();

        // Save as JPEG
        ImageIO.write(image, "jpg", outputFile);
        return outputFile;
    }

    /**
     * Generates a unique color for each image index.
     */
    private static Color generateColor(int index) {
        // Predefined colors for consistency
        Color[] colors = {
            new Color(102, 153, 204), // Blue
            new Color(204, 102, 153), // Pink
            new Color(153, 204, 102), // Green
            new Color(204, 153, 102), // Orange
            new Color(153, 102, 204), // Purple
            new Color(102, 204, 204), // Cyan
            new Color(204, 204, 102), // Yellow
            new Color(204, 102, 102), // Red
            new Color(102, 204, 153), // Teal
            new Color(153, 153, 204)  // Lavender
        };
        return colors[index % colors.length];
    }

    /**
     * Draws a unique pattern on each image.
     */
    private static void drawPattern(Graphics2D g2d, int index, int width, int height) {
        g2d.setStroke(new BasicStroke(2));

        // Different pattern for each image index
        switch (index % 5) {
            case 0: // Diagonal lines
                g2d.setColor(new Color(255, 255, 255, 50));
                for (int i = 0; i < width + height; i += 40) {
                    g2d.drawLine(i, 0, 0, i);
                }
                break;
            case 1: // Grid
                g2d.setColor(new Color(255, 255, 255, 50));
                for (int i = 0; i < width; i += 50) {
                    g2d.drawLine(i, 0, i, height);
                }
                for (int i = 0; i < height; i += 50) {
                    g2d.drawLine(0, i, width, i);
                }
                break;
            case 2: // Circles
                g2d.setColor(new Color(255, 255, 255, 50));
                for (int i = 0; i < 5; i++) {
                    int radius = (Math.min(width, height) / 6) * (i + 1);
                    g2d.drawOval((width - radius) / 2, (height - radius) / 2, radius, radius);
                }
                break;
            case 3: // Dots
                g2d.setColor(new Color(255, 255, 255, 80));
                Random rand = new Random(index); // Use index as seed for consistency
                for (int i = 0; i < 50; i++) {
                    int x = rand.nextInt(width);
                    int y = rand.nextInt(height);
                    g2d.fillOval(x, y, 10, 10);
                }
                break;
            case 4: // Rectangles
                g2d.setColor(new Color(255, 255, 255, 50));
                for (int i = 0; i < 3; i++) {
                    int rectWidth = width / 4 + i * width / 8;
                    int rectHeight = height / 4 + i * height / 8;
                    g2d.drawRect((width - rectWidth) / 2, (height - rectHeight) / 2, rectWidth, rectHeight);
                }
                break;
        }
    }

    /**
     * Generates a visually rich demo image suitable for screenshots.
     * Creates an image with interesting patterns, gradients, and shapes.
     *
     * @param outputFile Target file to save the image
     * @return The created file
     * @throws IOException If image creation fails
     */
    public static File generateDemoImage(final File outputFile) throws IOException {
        final int width = 1200;
        final int height = 800;

        final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        final Graphics2D g2d = image.createGraphics();

        // Set rendering hints for quality
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // Create gradient background
        final GradientPaint gradient = new GradientPaint(
            0, 0, new Color(45, 52, 54),
            width, height, new Color(99, 110, 114)
        );
        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, width, height);

        // Draw geometric patterns
        drawGeometricPattern(g2d, width, height);

        // Draw sample photo area with frame
        drawPhotoFrame(g2d, width / 4, height / 4, width / 2, height / 2);

        // Add title
        g2d.setColor(new Color(255, 255, 255, 200));
        final Font titleFont = new Font("Arial", Font.BOLD, 48);
        g2d.setFont(titleFont);
        final String title = "Winnow Demo";
        final FontMetrics fm = g2d.getFontMetrics();
        final int titleWidth = fm.stringWidth(title);
        g2d.drawString(title, (width - titleWidth) / 2, 80);

        // Add subtitle
        g2d.setColor(new Color(255, 255, 255, 150));
        final Font subtitleFont = new Font("Arial", Font.PLAIN, 24);
        g2d.setFont(subtitleFont);
        final String subtitle = "Bulk Image Editor with Zoom & Crop";
        final FontMetrics fm2 = g2d.getFontMetrics();
        final int subtitleWidth = fm2.stringWidth(subtitle);
        g2d.drawString(subtitle, (width - subtitleWidth) / 2, 120);

        g2d.dispose();

        // Save as JPEG with high quality
        ImageIO.write(image, "jpg", outputFile);
        return outputFile;
    }

    private static void drawGeometricPattern(final Graphics2D g2d, final int width, final int height) {
        // Draw decorative circles
        g2d.setStroke(new BasicStroke(2));
        g2d.setColor(new Color(255, 255, 255, 30));

        final Random rand = new Random(42); // Fixed seed for consistency
        for (int i = 0; i < 20; i++) {
            final int x = rand.nextInt(width);
            final int y = rand.nextInt(height);
            final int size = rand.nextInt(100) + 50;
            g2d.drawOval(x - size / 2, y - size / 2, size, size);
        }

        // Draw decorative lines
        g2d.setStroke(new BasicStroke(1));
        for (int i = 0; i < width; i += 100) {
            g2d.drawLine(i, 0, i + 50, height);
        }
    }

    private static void drawPhotoFrame(final Graphics2D g2d, final int x, final int y, final int w, final int h) {
        // Draw photo area with gradient
        final GradientPaint photoGradient = new GradientPaint(
            x, y, new Color(240, 147, 43),
            x + w, y + h, new Color(235, 77, 75)
        );
        g2d.setPaint(photoGradient);
        g2d.fillRect(x, y, w, h);

        // Draw frame
        g2d.setColor(new Color(255, 255, 255, 200));
        g2d.setStroke(new BasicStroke(8));
        g2d.drawRect(x, y, w, h);

        // Draw inner decorative elements
        g2d.setColor(new Color(255, 255, 255, 100));
        g2d.setStroke(new BasicStroke(2));
        final int margin = 30;
        g2d.drawRect(x + margin, y + margin, w - 2 * margin, h - 2 * margin);

        // Add "Sample Photo" text
        g2d.setColor(new Color(255, 255, 255, 180));
        final Font font = new Font("Arial", Font.BOLD, 36);
        g2d.setFont(font);
        final String text = "Sample Photo";
        final FontMetrics fm = g2d.getFontMetrics();
        final int textWidth = fm.stringWidth(text);
        g2d.drawString(text, x + (w - textWidth) / 2, y + h / 2);
    }

    /**
     * Generates a procedurally created abstract art image with unique patterns.
     *
     * @param seed Seed for random generation (for reproducibility)
     * @param outputFile Target file to save the image
     * @param version Version string to display (e.g., "0.2.0")
     * @return The created file
     * @throws IOException If image creation fails
     */
    public static File generateAbstractArt(final int seed, final File outputFile, final String version) throws IOException {
        final int width = 1200;
        final int height = 800;

        final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        final Graphics2D g2d = image.createGraphics();

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        final Random rand = new Random(seed);
        final int artStyle = rand.nextInt(5);

        switch (artStyle) {
            case 0:
                generateFlowingCurves(g2d, width, height, rand, version);
                break;
            case 1:
                generateGeometricShapes(g2d, width, height, rand, version);
                break;
            case 2:
                generateParticleField(g2d, width, height, rand, version);
                break;
            case 3:
                generateMondrianStyle(g2d, width, height, rand, version);
                break;
            case 4:
                generateColorBlocks(g2d, width, height, rand, version);
                break;
        }

        g2d.dispose();
        ImageIO.write(image, "jpg", outputFile);
        return outputFile;
    }


    private static void generateFlowingCurves(final Graphics2D g2d, final int width, final int height,
                                              final Random rand, final String version) {
        // Black background
        g2d.setColor(new Color(15, 15, 20));
        g2d.fillRect(0, 0, width, height);

        // Gold rays emanating from a point (like sunlight)
        final int originX = width / 4 + rand.nextInt(width / 2);
        final int originY = height / 4 + rand.nextInt(height / 2);
        final int rayCount = 6 + rand.nextInt(3);

        for (int i = 0; i < rayCount; i++) {
            final int goldVariation = rand.nextInt(30);
            g2d.setColor(new Color(200 + goldVariation, 170 + goldVariation, 60 + goldVariation, 80));
            g2d.setStroke(new BasicStroke(40 + rand.nextInt(30), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            final double angle = (Math.PI * 2 * i) / rayCount + rand.nextDouble() * 0.3;
            final int endX = originX + (int) (Math.cos(angle) * width * 2);
            final int endY = originY + (int) (Math.sin(angle) * height * 2);
            g2d.drawLine(originX, originY, endX, endY);
        }

        // Large, bold gold text
        g2d.setFont(new Font("Comic Sans MS", Font.BOLD, 80));
        g2d.setColor(new Color(210, 180, 70));
        final FontMetrics fm = g2d.getFontMetrics();
        final String text = "winnow v" + version;
        final int textWidth = fm.stringWidth(text);
        g2d.drawString(text, (width - textWidth) / 2, (int)(height * 0.65) + fm.getHeight() / 3);
    }

    private static void generateGeometricShapes(final Graphics2D g2d, final int width, final int height,
                                                final Random rand, final String version) {
        // Black background
        g2d.setColor(new Color(10, 10, 15));
        g2d.fillRect(0, 0, width, height);

        // Gold rays emanating from center point
        final int originX = width / 3 + rand.nextInt(width / 3);
        final int originY = height / 3 + rand.nextInt(height / 3);
        final int rayCount = 5 + rand.nextInt(3);

        for (int i = 0; i < rayCount; i++) {
            final int goldVariation = rand.nextInt(40);
            g2d.setColor(new Color(180 + goldVariation, 150 + goldVariation, 50 + goldVariation, 90));
            g2d.setStroke(new BasicStroke(50 + rand.nextInt(40), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            final double angle = (Math.PI * 2 * i) / rayCount + rand.nextDouble() * 0.4;
            final int endX = originX + (int) (Math.cos(angle) * width * 2);
            final int endY = originY + (int) (Math.sin(angle) * height * 2);
            g2d.drawLine(originX, originY, endX, endY);
        }

        // Large gold text
        g2d.setFont(new Font("Comic Sans MS", Font.BOLD, 80));
        g2d.setColor(new Color(210, 180, 70));
        final FontMetrics fm = g2d.getFontMetrics();
        final String text = "winnow v" + version;
        final int textWidth = fm.stringWidth(text);
        g2d.drawString(text, (width - textWidth) / 2, (int)(height * 0.65) + fm.getHeight() / 3);
    }

    private static void generateParticleField(final Graphics2D g2d, final int width, final int height,
                                              final Random rand, final String version) {
        // Black background
        g2d.setColor(new Color(5, 5, 10));
        g2d.fillRect(0, 0, width, height);

        // Gold rays emanating from a point
        final int originX = width / 3 + rand.nextInt(width / 3);
        final int originY = height / 3 + rand.nextInt(height / 3);
        final int rayCount = 8 + rand.nextInt(4);

        for (int i = 0; i < rayCount; i++) {
            final int goldVariation = rand.nextInt(35);
            g2d.setColor(new Color(190 + goldVariation, 160 + goldVariation, 55 + goldVariation, 100));
            g2d.setStroke(new BasicStroke(20 + rand.nextInt(20), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            final double angle = (Math.PI * 2 * i) / rayCount + rand.nextDouble() * 0.2;
            final int endX = originX + (int) (Math.cos(angle) * width * 2);
            final int endY = originY + (int) (Math.sin(angle) * height * 2);
            g2d.drawLine(originX, originY, endX, endY);
        }

        // Large gold text
        g2d.setFont(new Font("Comic Sans MS", Font.BOLD, 80));
        g2d.setColor(new Color(210, 180, 70));
        final FontMetrics fm = g2d.getFontMetrics();
        final String text = "winnow v" + version;
        final int textWidth = fm.stringWidth(text);
        g2d.drawString(text, (width - textWidth) / 2, (int)(height * 0.65) + fm.getHeight() / 3);
    }

    private static void generateMondrianStyle(final Graphics2D g2d, final int width, final int height,
                                              final Random rand, final String version) {
        // Black background
        g2d.setColor(new Color(12, 12, 18));
        g2d.fillRect(0, 0, width, height);

        // Gold rays from corner
        final int originX = rand.nextInt(width / 3);
        final int originY = rand.nextInt(height / 3);
        final int rayCount = 5 + rand.nextInt(3);

        for (int i = 0; i < rayCount; i++) {
            final int goldVariation = rand.nextInt(45);
            g2d.setColor(new Color(200 + goldVariation, 165 + goldVariation, 50 + goldVariation, 85));
            g2d.setStroke(new BasicStroke(60 + rand.nextInt(40), BasicStroke.CAP_SQUARE, BasicStroke.JOIN_MITER));

            final double angle = (Math.PI * 2 * i) / rayCount + rand.nextDouble() * 0.5;
            final int endX = originX + (int) (Math.cos(angle) * width * 2);
            final int endY = originY + (int) (Math.sin(angle) * height * 2);
            g2d.drawLine(originX, originY, endX, endY);
        }

        // Bold gold text
        g2d.setFont(new Font("Comic Sans MS", Font.BOLD, 80));
        g2d.setColor(new Color(210, 180, 70));
        final FontMetrics fm = g2d.getFontMetrics();
        final String text = "winnow v" + version;
        final int textWidth = fm.stringWidth(text);
        g2d.drawString(text, (width - textWidth) / 2, (int)(height * 0.65) + fm.getHeight() / 3);
    }


    private static void generateColorBlocks(final Graphics2D g2d, final int width, final int height,
                                            final Random rand, final String version) {
        // Black background
        g2d.setColor(new Color(8, 8, 12));
        g2d.fillRect(0, 0, width, height);

        // Gold rays from a central point
        final int originX = width / 4 + rand.nextInt(width / 2);
        final int originY = height / 4 + rand.nextInt(height / 2);
        final int rayCount = 6 + rand.nextInt(3);

        for (int i = 0; i < rayCount; i++) {
            final int goldVariation = rand.nextInt(50);
            g2d.setColor(new Color(185 + goldVariation, 155 + goldVariation, 45 + goldVariation, 95));
            g2d.setStroke(new BasicStroke(50 + rand.nextInt(40), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            final double angle = (Math.PI * 2 * i) / rayCount + rand.nextDouble() * 0.3;
            final int endX = originX + (int) (Math.cos(angle) * width * 2);
            final int endY = originY + (int) (Math.sin(angle) * height * 2);
            g2d.drawLine(originX, originY, endX, endY);
        }

        // Clean gold text
        g2d.setFont(new Font("Comic Sans MS", Font.BOLD, 80));
        g2d.setColor(new Color(210, 180, 70));
        final FontMetrics fm = g2d.getFontMetrics();
        final String text = "winnow v" + version;
        final int textWidth = fm.stringWidth(text);
        g2d.drawString(text, (width - textWidth) / 2, (int)(height * 0.65) + fm.getHeight() / 3);
    }
}
