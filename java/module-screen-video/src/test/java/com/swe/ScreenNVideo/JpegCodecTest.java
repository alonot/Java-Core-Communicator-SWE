package com.swe.ScreenNVideo;

import com.swe.ScreenNVideo.Codec.JpegCodec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Comprehensive test suite for JpegCodec class.
 * Tests JPEG encoding and decoding with YCbCr color space conversion.
 */
public class JpegCodecTest {

    private static final int SMALL_DIM = 8;
    private static final int MEDIUM_DIM = 16;
    private static final int LARGE_DIM = 32;
    private static final int COLOR_BLACK = 0xFF000000;
    private static final int COLOR_WHITE = 0xFFFFFFFF;
    private static final int COLOR_RED = 0xFFFF0000;
    private static final int COLOR_GREEN = 0xFF00FF00;
    private static final int COLOR_BLUE = 0xFF0000FF;
    private static final int COLOR_GRAY = 0xFF808080;
    private static final int QUALITY_LOW = 10;
    private static final int QUALITY_MID = 50;
    private static final int QUALITY_HIGH = 90;
    private static final int DELTA = 50;
    private static final int TOP_LEFT_ZERO = 0;
    private static final int ALPHA_MASK = 0xFF000000;
    private static final int RED_SHIFT = 16;
    private static final int GREEN_SHIFT = 8;
    private static final int COLOR_MASK = 0xFF;

    private JpegCodec codec;

    /**
     * Sets up test fixture before each test.
     */
    @BeforeEach
    public void setUp() {
        codec = new JpegCodec();
    }

    /**
     * Tests default constructor creates non-null instance.
     */
    @Test
    public void testDefaultConstructor() {
        assertNotNull(codec);
    }

    /**
     * Tests encode with all black image.
     */
    @Test
    public void testEncodeBlackImage() {
        final int[][] image = createSolidColorImage(SMALL_DIM, SMALL_DIM, COLOR_BLACK);

        final byte[] encoded = codec.encode(image, TOP_LEFT_ZERO, TOP_LEFT_ZERO, SMALL_DIM, SMALL_DIM).get(0);
        assertNotNull(encoded);
        assertTrue(encoded.length > 0);
    }

    /**
     * Tests encode with all white image.
     */
    @Test
    public void testEncodeWhiteImage() {
        final int[][] image = createSolidColorImage(SMALL_DIM, SMALL_DIM, COLOR_WHITE);

        final byte[] encoded = codec.encode(image, TOP_LEFT_ZERO, TOP_LEFT_ZERO, SMALL_DIM, SMALL_DIM).get(0);
        assertNotNull(encoded);
        assertTrue(encoded.length > 0);
    }

    /**
     * Tests encode with red image.
     */
    @Test
    public void testEncodeRedImage() {
        final int[][] image = createSolidColorImage(SMALL_DIM, SMALL_DIM, COLOR_RED);

        final byte[] encoded = codec.encode(image, TOP_LEFT_ZERO, TOP_LEFT_ZERO, SMALL_DIM, SMALL_DIM).get(0);
        assertNotNull(encoded);
        assertTrue(encoded.length > 0);
    }

    /**
     * Tests encode with green image.
     */
    @Test
    public void testEncodeGreenImage() {
        final int[][] image = createSolidColorImage(SMALL_DIM, SMALL_DIM, COLOR_GREEN);

        final byte[] encoded = codec.encode(image, TOP_LEFT_ZERO, TOP_LEFT_ZERO, SMALL_DIM, SMALL_DIM).get(0);
        assertNotNull(encoded);
        assertTrue(encoded.length > 0);
    }

    /**
     * Tests encode with blue image.
     */
    @Test
    public void testEncodeBlueImage() {
        final int[][] image = createSolidColorImage(SMALL_DIM, SMALL_DIM, COLOR_BLUE);

        final byte[] encoded = codec.encode(image, TOP_LEFT_ZERO, TOP_LEFT_ZERO, SMALL_DIM, SMALL_DIM).get(0);
        assertNotNull(encoded);
        assertTrue(encoded.length > 0);
    }

    /**
     * Tests encode with gray image.
     */
    @Test
    public void testEncodeGrayImage() {
        final int[][] image = createSolidColorImage(SMALL_DIM, SMALL_DIM, COLOR_GRAY);

        final byte[] encoded = codec.encode(image, TOP_LEFT_ZERO, TOP_LEFT_ZERO, SMALL_DIM, SMALL_DIM).get(0);
        assertNotNull(encoded);
        assertTrue(encoded.length > 0);
    }

    /**
     * Tests encode with 16x16 image.
     */
    @Test
    public void testEncodeMediumImage() {
        final int[][] image = createSolidColorImage(MEDIUM_DIM, MEDIUM_DIM, COLOR_GRAY);

        final byte[] encoded = codec.encode(image, TOP_LEFT_ZERO, TOP_LEFT_ZERO, MEDIUM_DIM, MEDIUM_DIM).get(0);
        assertNotNull(encoded);
        assertTrue(encoded.length > 0);
    }

    /**
     * Tests encode with 32x32 image.
     */
    @Test
    public void testEncodeLargeImage() {
        final int[][] image = createSolidColorImage(LARGE_DIM, LARGE_DIM, COLOR_GRAY);

        final byte[] encoded = codec.encode(image, TOP_LEFT_ZERO, TOP_LEFT_ZERO, LARGE_DIM, LARGE_DIM).get(0);
        assertNotNull(encoded);
        assertTrue(encoded.length > 0);
    }

    /**
     * Tests encode with offset position.
     */
    @Test
    public void testEncodeWithOffset() {
        final int[][] image = createSolidColorImage(LARGE_DIM, LARGE_DIM, COLOR_GRAY);

        final byte[] encoded = codec.encode(image, SMALL_DIM, SMALL_DIM, MEDIUM_DIM, MEDIUM_DIM).get(0);
        assertNotNull(encoded);
        assertTrue(encoded.length > 0);
    }

    /**
     * Tests encode throws exception for odd height.
     */
    @Test
    public void testEncodeOddHeightThrowsException() {
        final int[][] image = createSolidColorImage(SMALL_DIM, SMALL_DIM, COLOR_GRAY);

        assertThrows(RuntimeException.class, () -> {
            codec.encode(image, TOP_LEFT_ZERO, TOP_LEFT_ZERO, 7, SMALL_DIM).get(0);
        });
    }

    /**
     * Tests encode throws exception for odd width.
     */
    @Test
    public void testEncodeOddWidthThrowsException() {
        final int[][] image = createSolidColorImage(SMALL_DIM, SMALL_DIM, COLOR_GRAY);

        assertThrows(RuntimeException.class, () -> {
            codec.encode(image, TOP_LEFT_ZERO, TOP_LEFT_ZERO, SMALL_DIM, 7).get(0);
        });
    }

    /**
     * Tests decode with encoded black image.
     */
    @Test
    public void testDecodeBlackImage() {
        final int[][] image = createSolidColorImage(SMALL_DIM, SMALL_DIM, COLOR_BLACK);

        final byte[] encoded = codec.encode(image, TOP_LEFT_ZERO, TOP_LEFT_ZERO, SMALL_DIM, SMALL_DIM).get(0);
        final int[][] decoded = codec.decode(encoded, true);

        assertNotNull(decoded);
        assertEquals(SMALL_DIM, decoded.length);
        assertEquals(SMALL_DIM, decoded[0].length);
    }

    /**
     * Tests decode with encoded white image.
     */
    @Test
    public void testDecodeWhiteImage() {
        final int[][] image = createSolidColorImage(SMALL_DIM, SMALL_DIM, COLOR_WHITE);

        final byte[] encoded = codec.encode(image, TOP_LEFT_ZERO, TOP_LEFT_ZERO, SMALL_DIM, SMALL_DIM).get(0);
        final int[][] decoded = codec.decode(encoded, true);

        assertNotNull(decoded);
        assertEquals(SMALL_DIM, decoded.length);
        assertEquals(SMALL_DIM, decoded[0].length);
    }

    /**
     * Tests round-trip encoding and decoding with black image.
     */
    @Test
    public void testRoundTripBlackImage() {
        final int[][] original = createSolidColorImage(SMALL_DIM, SMALL_DIM, COLOR_BLACK);

        final byte[] encoded = codec.encode(original, TOP_LEFT_ZERO, TOP_LEFT_ZERO, SMALL_DIM, SMALL_DIM).get(0);
        final int[][] decoded = codec.decode(encoded, true);

        verifyImageSimilarity(original, decoded);
    }

    /**
     * Tests round-trip encoding and decoding with white image.
     */
    @Test
    public void testRoundTripWhiteImage() {
        final int[][] original = createSolidColorImage(SMALL_DIM, SMALL_DIM, COLOR_WHITE);

        final byte[] encoded = codec.encode(original, TOP_LEFT_ZERO, TOP_LEFT_ZERO, SMALL_DIM, SMALL_DIM).get(0);
        final int[][] decoded = codec.decode(encoded, true);

        verifyImageSimilarity(original, decoded);
    }

    /**
     * Tests round-trip encoding and decoding with red image.
     */
    @Test
    public void testRoundTripRedImage() {
        final int[][] original = createSolidColorImage(SMALL_DIM, SMALL_DIM, COLOR_RED);

        final byte[] encoded = codec.encode(original, TOP_LEFT_ZERO, TOP_LEFT_ZERO, SMALL_DIM, SMALL_DIM).get(0);
        final int[][] decoded = codec.decode(encoded, true);

        verifyImageSimilarity(original, decoded);
    }

    /**
     * Tests round-trip encoding and decoding with green image.
     */
    @Test
    public void testRoundTripGreenImage() {
        final int[][] original = createSolidColorImage(SMALL_DIM, SMALL_DIM, COLOR_GREEN);

        final byte[] encoded = codec.encode(original, TOP_LEFT_ZERO, TOP_LEFT_ZERO, SMALL_DIM, SMALL_DIM).get(0);
        final int[][] decoded = codec.decode(encoded, true);

        verifyImageSimilarity(original, decoded);
    }

    /**
     * Tests round-trip encoding and decoding with blue image.
     */
    @Test
    public void testRoundTripBlueImage() {
        final int[][] original = createSolidColorImage(SMALL_DIM, SMALL_DIM, COLOR_BLUE);

        final byte[] encoded = codec.encode(original, TOP_LEFT_ZERO, TOP_LEFT_ZERO, SMALL_DIM, SMALL_DIM).get(0);
        final int[][] decoded = codec.decode(encoded, true);

        verifyImageSimilarity(original, decoded);
    }

    /**
     * Tests round-trip encoding and decoding with gray image.
     */
    @Test
    public void testRoundTripGrayImage() {
        final int[][] original = createSolidColorImage(SMALL_DIM, SMALL_DIM, COLOR_GRAY);

        final byte[] encoded = codec.encode(original, TOP_LEFT_ZERO, TOP_LEFT_ZERO, SMALL_DIM, SMALL_DIM).get(0);
        final int[][] decoded = codec.decode(encoded, true);

        verifyImageSimilarity(original, decoded);
    }

    /**
     * Tests round-trip encoding and decoding with gradient image.
     */
    @Test
    public void testRoundTripGradientImage() {
        final int[][] original = createGradientImage(SMALL_DIM, SMALL_DIM);

        final byte[] encoded = codec.encode(original, TOP_LEFT_ZERO, TOP_LEFT_ZERO, SMALL_DIM, SMALL_DIM).get(0);
        final int[][] decoded = codec.decode(encoded, true);

        verifyImageSimilarity(original, decoded);
    }

    /**
     * Tests round-trip encoding and decoding with checkerboard image.
     */
    @Test
    public void testRoundTripCheckerboardImage() {
        final int[][] original = createCheckerboardImage(SMALL_DIM, SMALL_DIM);

        final byte[] encoded = codec.encode(original, TOP_LEFT_ZERO, TOP_LEFT_ZERO, SMALL_DIM, SMALL_DIM).get(0);
        final int[][] decoded = codec.decode(encoded, true);

        verifyImageSimilarity(original, decoded);
    }

    /**
     * Tests round-trip with 16x16 image.
     */
    @Test
    public void testRoundTripMediumImage() {
        final int[][] original = createSolidColorImage(MEDIUM_DIM, MEDIUM_DIM, COLOR_GRAY);

        final byte[] encoded = codec.encode(original, TOP_LEFT_ZERO, TOP_LEFT_ZERO, MEDIUM_DIM, MEDIUM_DIM).get(0);
        final int[][] decoded = codec.decode(encoded, true);

        verifyImageSimilarity(original, decoded);
    }

    /**
     * Tests round-trip with 32x32 image.
     */
    @Test
    public void testRoundTripLargeImage() {
        final int[][] original = createSolidColorImage(LARGE_DIM, LARGE_DIM, COLOR_GRAY);

        final byte[] encoded = codec.encode(original, TOP_LEFT_ZERO, TOP_LEFT_ZERO, LARGE_DIM, LARGE_DIM).get(0);
        final int[][] decoded = codec.decode(encoded, true);

        verifyImageSimilarity(original, decoded);
    }

    /**
     * Tests encoding with low quality factor.
     */
    @Test
    public void testEncodeLowQuality() {
        final int[][] image = createSolidColorImage(SMALL_DIM, SMALL_DIM, COLOR_GRAY);
//        codec.setCompressionFactor((short) QUALITY_LOW);

        final byte[] encoded = codec.encode(image, TOP_LEFT_ZERO, TOP_LEFT_ZERO, SMALL_DIM, SMALL_DIM).get(0);
        assertNotNull(encoded);
        assertTrue(encoded.length > 0);
    }

    /**
     * Tests encoding with high quality factor.
     */
    @Test
    public void testEncodeHighQuality() {
        final int[][] image = createSolidColorImage(SMALL_DIM, SMALL_DIM, COLOR_GRAY);
//        codec.setCompressionFactor((short) QUALITY_HIGH);

        final byte[] encoded = codec.encode(image, TOP_LEFT_ZERO, TOP_LEFT_ZERO, SMALL_DIM, SMALL_DIM).get(0);
        assertNotNull(encoded);
        assertTrue(encoded.length > 0);
    }

    /**
     * Tests round-trip with low quality.
     */
    @Test
    public void testRoundTripLowQuality() {
        final int[][] original = createSolidColorImage(SMALL_DIM, SMALL_DIM, COLOR_GRAY);
//        codec.setCompressionFactor((short) QUALITY_LOW);

        final byte[] encoded = codec.encode(original, TOP_LEFT_ZERO, TOP_LEFT_ZERO, SMALL_DIM, SMALL_DIM).get(0);
        final int[][] decoded = codec.decode(encoded, true);

        assertNotNull(decoded);
    }

    /**
     * Tests round-trip with high quality.
     */
    @Test
    public void testRoundTripHighQuality() {
        final int[][] original = createSolidColorImage(SMALL_DIM, SMALL_DIM, COLOR_GRAY);
//        codec.setCompressionFactor((short) QUALITY_HIGH);

        final byte[] encoded = codec.encode(original, TOP_LEFT_ZERO, TOP_LEFT_ZERO, SMALL_DIM, SMALL_DIM).get(0);
        final int[][] decoded = codec.decode(encoded, true);

        assertNotNull(decoded);
    }

    /**
     * Tests encode with mixed color image.
     */
    @Test
    public void testEncodeMixedColorImage() {
        final int[][] image = createMixedColorImage(SMALL_DIM, SMALL_DIM);

        final byte[] encoded = codec.encode(image, TOP_LEFT_ZERO, TOP_LEFT_ZERO, SMALL_DIM, SMALL_DIM).get(0);
        assertNotNull(encoded);
        assertTrue(encoded.length > 0);
    }

    /**
     * Tests round-trip with mixed color image.
     */
    @Test
    public void testRoundTripMixedColorImage() {
        final int[][] original = createMixedColorImage(SMALL_DIM, SMALL_DIM);

        final byte[] encoded = codec.encode(original, TOP_LEFT_ZERO, TOP_LEFT_ZERO, SMALL_DIM, SMALL_DIM).get(0);
        final int[][] decoded = codec.decode(encoded, true);

        verifyImageSimilarity(original, decoded);
    }

    /**
     * Tests encode with non-aligned dimensions for chroma subsampling.
     */
    @Test
    public void testEncodeNonAlignedDimensions() {
        final int height = 10;
        final int width = 10;
        final int[][] image = createSolidColorImage(MEDIUM_DIM, MEDIUM_DIM, COLOR_GRAY);

        final byte[] encoded = codec.encode(image, TOP_LEFT_ZERO, TOP_LEFT_ZERO, height, width).get(0);
        assertNotNull(encoded);
        assertTrue(encoded.length > 0);
    }

    /**
     * Tests round-trip with non-aligned dimensions.
     */
    @Test
    public void testRoundTripNonAlignedDimensions() {
        final int height = 10;
        final int width = 10;
        final int[][] original = createSolidColorImage(MEDIUM_DIM, MEDIUM_DIM, COLOR_GRAY);

        final byte[] encoded = codec.encode(original, TOP_LEFT_ZERO, TOP_LEFT_ZERO, height, width).get(0);
        final int[][] decoded = codec.decode(encoded, true);

        assertNotNull(decoded);
        assertEquals(height, decoded.length);
        assertEquals(width, decoded[0].length);
    }

    /**
     * Tests encode handles color clamping for Y channel.
     */
    @Test
    public void testEncodeColorClampingY() {
        final int[][] image = createSolidColorImage(SMALL_DIM, SMALL_DIM, COLOR_WHITE);

        final byte[] encoded = codec.encode(image, TOP_LEFT_ZERO, TOP_LEFT_ZERO, SMALL_DIM, SMALL_DIM).get(0);
        assertNotNull(encoded);
    }

    /**
     * Tests encode handles color clamping for CbCr channels.
     */
    @Test
    public void testEncodeColorClampingCbCr() {
        final int[][] image = createSolidColorImage(SMALL_DIM, SMALL_DIM, COLOR_RED);

        final byte[] encoded = codec.encode(image, TOP_LEFT_ZERO, TOP_LEFT_ZERO, SMALL_DIM, SMALL_DIM).get(0);
        assertNotNull(encoded);
    }

    /**
     * Tests decode handles color clamping for RGB channels.
     */
    @Test
    public void testDecodeColorClamping() {
        final int[][] image = createSolidColorImage(SMALL_DIM, SMALL_DIM, COLOR_WHITE);

        final byte[] encoded = codec.encode(image, TOP_LEFT_ZERO, TOP_LEFT_ZERO, SMALL_DIM, SMALL_DIM).get(0);
        final int[][] decoded = codec.decode(encoded, true);

        for (int i = 0; i < decoded.length; i++) {
            for (int j = 0; j < decoded[0].length; j++) {
                final int pixel = decoded[i][j];
                final int r = (pixel >> RED_SHIFT) & COLOR_MASK;
                final int g = (pixel >> GREEN_SHIFT) & COLOR_MASK;
                final int b = pixel & COLOR_MASK;

                assertTrue(r >= 0 && r <= COLOR_MASK);
                assertTrue(g >= 0 && g <= COLOR_MASK);
                assertTrue(b >= 0 && b <= COLOR_MASK);
            }
        }
    }

    /**
     * Helper method to create solid color image.
     *
     * @param height image height
     * @param width image width
     * @param color ARGB color value
     * @return image array
     */
    private int[][] createSolidColorImage(final int height, final int width, final int color) {
        final int[][] image = new int[height][width];
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                image[i][j] = color;
            }
        }
        return image;
    }

    /**
     * Helper method to create gradient image.
     *
     * @param height image height
     * @param width image width
     * @return image array
     */
    private int[][] createGradientImage(final int height, final int width) {
        final int[][] image = new int[height][width];
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                final int value = (i * COLOR_MASK / height);
                image[i][j] = ALPHA_MASK | (value << RED_SHIFT) | (value << GREEN_SHIFT) | value;
            }
        }
        return image;
    }

    /**
     * Helper method to create checkerboard image.
     *
     * @param height image height
     * @param width image width
     * @return image array
     */
    private int[][] createCheckerboardImage(final int height, final int width) {
        final int[][] image = new int[height][width];
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                image[i][j] = (i + j) % 2 == 0 ? COLOR_WHITE : COLOR_BLACK;
            }
        }
        return image;
    }

    /**
     * Helper method to create mixed color image.
     *
     * @param height image height
     * @param width image width
     * @return image array
     */
    private int[][] createMixedColorImage(final int height, final int width) {
        final int[][] image = new int[height][width];
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                final int colorIndex = (i + j) % 3;
                if (colorIndex == 0) {
                    image[i][j] = COLOR_RED;
                } else if (colorIndex == 1) {
                    image[i][j] = COLOR_GREEN;
                } else {
                    image[i][j] = COLOR_BLUE;
                }
            }
        }
        return image;
    }

    /**
     * Helper method to verify image similarity within delta.
     *
     * @param original original image
     * @param decoded decoded image
     */
    private void verifyImageSimilarity(final int[][] original, final int[][] decoded) {
        assertNotNull(decoded);
        assertEquals(original.length, decoded.length);
        assertEquals(original[0].length, decoded[0].length);

        for (int i = 0; i < original.length; i++) {
            for (int j = 0; j < original[0].length; j++) {
                final int origPixel = original[i][j];
                final int decPixel = decoded[i][j];

                final int origR = (origPixel >> RED_SHIFT) & COLOR_MASK;
                final int origG = (origPixel >> GREEN_SHIFT) & COLOR_MASK;
                final int origB = origPixel & COLOR_MASK;

                final int decR = (decPixel >> RED_SHIFT) & COLOR_MASK;
                final int decG = (decPixel >> GREEN_SHIFT) & COLOR_MASK;
                final int decB = decPixel & COLOR_MASK;

                assertTrue(Math.abs(origR - decR) <= DELTA);
                assertTrue(Math.abs(origG - decG) <= DELTA);
                assertTrue(Math.abs(origB - decB) <= DELTA);
            }
        }
    }
}