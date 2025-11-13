package com.swe.ScreenNVideo.Capture;

import java.awt.AWTException;
import java.awt.image.BufferedImage;

/**
 * Abstract base class for screen capture implementations.
 *
 * <p>Provides a method {@link #captureAsRGBMatrix()} to convert
 * a screenshot into a 3D RGB matrix representation.
 */
public abstract class ICapture {
    public abstract BufferedImage capture() throws AWTException;

    public abstract void reInit();

    /** Number of color channels in RGB. */
    private static final int CHANNELS = 3;

    /** Red channel shift bits. */
    private static final int RED_SHIFT = 16;

    /** Green channel shift bits. */
    private static final int GREEN_SHIFT = 8;

    /** Color channel mask (for extracting 8-bit values). */
    private static final int COLOR_MASK = 0xFF;

    /**
     * Captures the screen and returns a 3D RGB matrix [height][width][3].
     * Each pixel has {R, G, B} values (0–255).
     *
     * @return int[][][] RGB matrix of the screenshot
     * @throws AWTException if screen capture is not supported
     */
    @SuppressWarnings("checkstyle:FinalLocalVariable")
    public int[][][] captureAsRGBMatrix() throws AWTException {
        final BufferedImage image = capture();
        final int width = image.getWidth();
        final int height = image.getHeight();

        final int[][][] rgbMatrix = new int[height][width][CHANNELS];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                final int pixel = image.getRGB(x, y);
                rgbMatrix[y][x][0] = (pixel >> RED_SHIFT) & COLOR_MASK; // Red
                rgbMatrix[y][x][1] = (pixel >> GREEN_SHIFT) & COLOR_MASK;  // Green
                rgbMatrix[y][x][2] = pixel & COLOR_MASK;         // Blue
            }
        }

        return rgbMatrix;
    }
}
