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
                generateFlowingCurves(g2d, width, height, rand);
                break;
            case 1:
                generateGeometricShapes(g2d, width, height, rand);
                break;
            case 2:
                generateParticleField(g2d, width, height, rand);
                break;
            case 3:
                generateMondrianStyle(g2d, width, height, rand);
                break;
            case 4:
                generateColorBlocks(g2d, width, height, rand);
                break;
        }

        // Add centered text overlay with "winnow" and version
        addTextOverlay(g2d, width, height, version);

        g2d.dispose();
        ImageIO.write(image, "jpg", outputFile);
        return outputFile;
    }

    private static void addTextOverlay(final Graphics2D g2d, final int width, final int height, final String version) {
        final String appName = "winnow";
        final String versionText = "v" + version;

        // Calculate text dimensions
        final Font nameFont = new Font("Arial", Font.BOLD, 120);
        final Font versionFont = new Font("Arial", Font.PLAIN, 48);

        g2d.setFont(nameFont);
        final FontMetrics nameFm = g2d.getFontMetrics();
        final int nameWidth = nameFm.stringWidth(appName);
        final int nameHeight = nameFm.getHeight();

        g2d.setFont(versionFont);
        final FontMetrics versionFm = g2d.getFontMetrics();
        final int versionWidth = versionFm.stringWidth(versionText);
        final int versionHeight = versionFm.getHeight();

        // Calculate overlay dimensions
        final int overlayWidth = Math.max(nameWidth, versionWidth) + 80;
        final int overlayHeight = nameHeight + versionHeight + 60;
        final int overlayX = (width - overlayWidth) / 2;
        final int overlayY = (height - overlayHeight) / 2;

        // Draw semi-transparent background rectangle
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRoundRect(overlayX, overlayY, overlayWidth, overlayHeight, 20, 20);

        // Draw border
        g2d.setColor(new Color(255, 255, 255, 100));
        g2d.setStroke(new BasicStroke(3));
        g2d.drawRoundRect(overlayX, overlayY, overlayWidth, overlayHeight, 20, 20);

        // Draw app name
        g2d.setFont(nameFont);
        g2d.setColor(new Color(255, 255, 255, 255));
        final int nameX = (width - nameWidth) / 2;
        final int nameY = overlayY + (overlayHeight - versionHeight) / 2;
        g2d.drawString(appName, nameX, nameY);

        // Draw version
        g2d.setFont(versionFont);
        g2d.setColor(new Color(220, 220, 220, 230));
        final int versionX = (width - versionWidth) / 2;
        final int versionY = nameY + nameHeight - 10;
        g2d.drawString(versionText, versionX, versionY);
    }

    private static void generateFlowingCurves(final Graphics2D g2d, final int width, final int height, final Random rand) {
        // Background gradient
        final Color c1 = new Color(rand.nextInt(100) + 20, rand.nextInt(100) + 20, rand.nextInt(100) + 80);
        final Color c2 = new Color(rand.nextInt(100) + 80, rand.nextInt(100) + 20, rand.nextInt(100) + 20);
        g2d.setPaint(new GradientPaint(0, 0, c1, width, height, c2));
        g2d.fillRect(0, 0, width, height);

        // Flowing curves with transparency
        for (int i = 0; i < 30; i++) {
            final int r = rand.nextInt(200) + 55;
            final int g = rand.nextInt(200) + 55;
            final int b = rand.nextInt(200) + 55;
            final int alpha = rand.nextInt(100) + 50;
            g2d.setColor(new Color(r, g, b, alpha));
            g2d.setStroke(new BasicStroke(rand.nextInt(20) + 5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            final int points = rand.nextInt(5) + 3;
            final int[] xPoints = new int[points];
            final int[] yPoints = new int[points];
            for (int j = 0; j < points; j++) {
                xPoints[j] = rand.nextInt(width);
                yPoints[j] = rand.nextInt(height);
            }

            for (int j = 0; j < points - 1; j++) {
                g2d.drawLine(xPoints[j], yPoints[j], xPoints[j + 1], yPoints[j + 1]);
            }
        }
    }

    private static void generateGeometricShapes(final Graphics2D g2d, final int width, final int height, final Random rand) {
        // Background
        final Color bg = new Color(rand.nextInt(50) + 10, rand.nextInt(50) + 10, rand.nextInt(50) + 10);
        g2d.setColor(bg);
        g2d.fillRect(0, 0, width, height);

        // Random geometric shapes
        for (int i = 0; i < 20; i++) {
            final int r = rand.nextInt(200) + 55;
            final int g = rand.nextInt(200) + 55;
            final int b = rand.nextInt(200) + 55;
            final int alpha = rand.nextInt(150) + 105;
            g2d.setColor(new Color(r, g, b, alpha));

            final int x = rand.nextInt(width);
            final int y = rand.nextInt(height);
            final int size = rand.nextInt(200) + 50;

            final int shapeType = rand.nextInt(3);
            switch (shapeType) {
                case 0: // Circle
                    g2d.fillOval(x, y, size, size);
                    break;
                case 1: // Rectangle
                    g2d.fillRect(x, y, size, size * rand.nextInt(3) + 1);
                    break;
                case 2: // Triangle
                    final int[] xTri = {x, x + size, x + size / 2};
                    final int[] yTri = {y + size, y + size, y};
                    g2d.fillPolygon(xTri, yTri, 3);
                    break;
            }
        }
    }

    private static void generateParticleField(final Graphics2D g2d, final int width, final int height, final Random rand) {
        // Gradient background
        final GradientPaint gradient = new GradientPaint(
            0, 0, new Color(10, 10, 30),
            width, height, new Color(30, 10, 50)
        );
        g2d.setPaint(gradient);
        g2d.fillRect(0, 0, width, height);

        // Particles with connections
        final int particleCount = 150;
        final int[][] particles = new int[particleCount][2];

        for (int i = 0; i < particleCount; i++) {
            particles[i][0] = rand.nextInt(width);
            particles[i][1] = rand.nextInt(height);
        }

        // Draw connections
        g2d.setStroke(new BasicStroke(1));
        for (int i = 0; i < particleCount; i++) {
            for (int j = i + 1; j < particleCount; j++) {
                final int dx = particles[i][0] - particles[j][0];
                final int dy = particles[i][1] - particles[j][1];
                final double dist = Math.sqrt(dx * dx + dy * dy);

                if (dist < 150) {
                    final int alpha = (int) (150 - dist);
                    g2d.setColor(new Color(100, 150, 255, alpha));
                    g2d.drawLine(particles[i][0], particles[i][1], particles[j][0], particles[j][1]);
                }
            }
        }

        // Draw particles
        for (int i = 0; i < particleCount; i++) {
            final int size = rand.nextInt(8) + 2;
            g2d.setColor(new Color(150, 200, 255, 200));
            g2d.fillOval(particles[i][0] - size / 2, particles[i][1] - size / 2, size, size);
        }
    }

    private static void generateMondrianStyle(final Graphics2D g2d, final int width, final int height, final Random rand) {
        // White background
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, width, height);

        final Color[] mondrianColors = {
            new Color(220, 20, 20),   // Red
            new Color(20, 20, 220),   // Blue
            new Color(240, 220, 20),  // Yellow
            Color.WHITE,
            Color.WHITE,
            Color.WHITE
        };

        // Recursive subdivision
        subdivideRectangle(g2d, 0, 0, width, height, rand, mondrianColors, 0);

        // Draw black grid lines
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(8));
        drawGrid(g2d, 0, 0, width, height, rand, 0);
    }

    private static void subdivideRectangle(final Graphics2D g2d, final int x, final int y, final int w, final int h,
                                          final Random rand, final Color[] colors, final int depth) {
        if (depth > 4 || w < 100 || h < 100) {
            g2d.setColor(colors[rand.nextInt(colors.length)]);
            g2d.fillRect(x, y, w, h);
            return;
        }

        if (rand.nextBoolean() && w > 200) {
            final int splitX = x + w / 3 + rand.nextInt(w / 3);
            subdivideRectangle(g2d, x, y, splitX - x, h, rand, colors, depth + 1);
            subdivideRectangle(g2d, splitX, y, w - (splitX - x), h, rand, colors, depth + 1);
        } else if (h > 200) {
            final int splitY = y + h / 3 + rand.nextInt(h / 3);
            subdivideRectangle(g2d, x, y, w, splitY - y, rand, colors, depth + 1);
            subdivideRectangle(g2d, x, splitY, w, h - (splitY - y), rand, colors, depth + 1);
        } else {
            g2d.setColor(colors[rand.nextInt(colors.length)]);
            g2d.fillRect(x, y, w, h);
        }
    }

    private static void drawGrid(final Graphics2D g2d, final int x, final int y, final int w, final int h,
                                 final Random rand, final int depth) {
        if (depth > 4 || w < 100 || h < 100) {
            return;
        }

        if (rand.nextBoolean() && w > 200) {
            final int splitX = x + w / 3 + rand.nextInt(w / 3);
            g2d.drawLine(splitX, y, splitX, y + h);
            drawGrid(g2d, x, y, splitX - x, h, rand, depth + 1);
            drawGrid(g2d, splitX, y, w - (splitX - x), h, rand, depth + 1);
        } else if (h > 200) {
            final int splitY = y + h / 3 + rand.nextInt(h / 3);
            g2d.drawLine(x, splitY, x + w, splitY);
            drawGrid(g2d, x, y, w, splitY - y, rand, depth + 1);
            drawGrid(g2d, x, splitY, w, h - (splitY - y), rand, depth + 1);
        }
    }

    private static void generateColorBlocks(final Graphics2D g2d, final int width, final int height, final Random rand) {
        // Background
        g2d.setColor(new Color(rand.nextInt(30) + 10, rand.nextInt(30) + 10, rand.nextInt(30) + 10));
        g2d.fillRect(0, 0, width, height);

        // Generate color palette
        final int baseHue = rand.nextInt(360);
        final Color[] palette = new Color[5];
        for (int i = 0; i < 5; i++) {
            final int hue = (baseHue + i * 72) % 360;
            palette[i] = Color.getHSBColor(hue / 360f, 0.6f + rand.nextFloat() * 0.3f, 0.7f + rand.nextFloat() * 0.2f);
        }

        // Draw color blocks
        final int blockSize = 100;
        for (int x = 0; x < width; x += blockSize) {
            for (int y = 0; y < height; y += blockSize) {
                if (rand.nextFloat() > 0.3) {
                    final Color c = palette[rand.nextInt(palette.length)];
                    g2d.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), rand.nextInt(150) + 105));
                    final int offset = rand.nextInt(20);
                    final int size = blockSize + rand.nextInt(40) - 20;
                    g2d.fillRect(x + offset, y + offset, size, size);
                }
            }
        }
    }
}
