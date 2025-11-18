package com.swe.ScreenNVideo.Codec;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify that zigZagRLE encoding/decoding is completely lossless.
 * This test bypasses RGB-YCbCr conversion and chroma subsampling to test
 * only the zigZagRLE functionality.
 */
public class ZigZagRLELosslessTest {

    private static final int TILE_SIZE = 64;
    private static final Random RANDOM = new Random(42); // Fixed seed for reproducibility
    private static final int NUM_ITERATIONS = 3;

    /**
     * Test that verifies zigZagRLE is lossless by:
     * 1. Creating random Y, Cr, Cb short matrices directly
     * 2. Encoding with zigZagRLE (without DCT/quantization)
     * 3. Decoding with revZigZagRLE
     * 4. Verifying decoded matrices exactly match originals
     */
    @Test
    public void testZigZagRLEIsLossless() {
        // Generate random dimensions (divisible by 2 and multiple of 8 for clean 8x8 blocks)
        final int width = generateRandomMultipleOf8(64, 320);
        final int height = generateRandomMultipleOf8(64, 240);

        System.out.println("Testing zigZagRLE lossless encoding with dimensions: " + height + "x" + width);

        final IRLE enDeRLE = EncodeDecodeRLEHuffman.getInstance();

        // Process multiple iterations
        for (int iteration = 1; iteration <= NUM_ITERATIONS; iteration++) {
            System.out.println("\n=== Iteration " + iteration + " ===");

            // Step 1: Create random Y, Cr, Cb matrices
            // Y matrix: full size
            final short[][] originalYMatrix = createRandomShortMatrix(height, width);

            // Cr and Cb matrices: half size (simulating 4:2:0 subsampling)
            final int chromaHeight = height / 2;
            final int chromaWidth = width / 2;
            final short[][] originalCrMatrix = createRandomShortMatrix(chromaHeight, chromaWidth);
            final short[][] originalCbMatrix = createRandomShortMatrix(chromaHeight, chromaWidth);

            System.out.println("Y matrix: " + height + "x" + width);
            System.out.println("Cr/Cb matrices: " + chromaHeight + "x" + chromaWidth);

            // Step 2: Encode using zigZagRLE (similar to JpegCodec.encode lines 265-293)
            final int maxLen = calculateMaxBufferSize(height, width);
            final ByteBuffer resRLEBuffer = ByteBuffer.allocate(maxLen);

            // Encode Y matrix
            enDeRLE.zigZagRLE(originalYMatrix, resRLEBuffer);
            final int yEncodedSize = resRLEBuffer.position();
            System.out.println("Y encoded size: " + yEncodedSize + " bytes");

            // Encode Cb matrix
            enDeRLE.zigZagRLE(originalCbMatrix, resRLEBuffer);
            final int cbEncodedSize = resRLEBuffer.position() - yEncodedSize;
            System.out.println("Cb encoded size: " + cbEncodedSize + " bytes");

            // Encode Cr matrix
            enDeRLE.zigZagRLE(originalCrMatrix, resRLEBuffer);
            final int crEncodedSize = resRLEBuffer.position() - yEncodedSize - cbEncodedSize;
            System.out.println("Cr encoded size: " + crEncodedSize + " bytes");

            final int totalEncodedSize = resRLEBuffer.position();
            System.out.println("Total encoded size: " + totalEncodedSize + " bytes");

            // Prepare buffer for decoding
            final byte[] encodedData = new byte[totalEncodedSize];
            resRLEBuffer.rewind();
            resRLEBuffer.get(encodedData);

            // Step 3: Decode using revZigZagRLE (similar to JpegCodec.decode lines 310-313)
            final ByteBuffer decodeBuffer = ByteBuffer.wrap(encodedData);

            final short[][] decodedYMatrix = enDeRLE.revZigZagRLE(decodeBuffer);
            final short[][] decodedCbMatrix = enDeRLE.revZigZagRLE(decodeBuffer);
            final short[][] decodedCrMatrix = enDeRLE.revZigZagRLE(decodeBuffer);

            assertNotNull(decodedYMatrix, "Decoded Y matrix is null");
            assertNotNull(decodedCbMatrix, "Decoded Cb matrix is null");
            assertNotNull(decodedCrMatrix, "Decoded Cr matrix is null");

            // Step 4: Verify decoded matrices exactly match originals
            System.out.println("\nVerifying Y matrix...");
            verifyMatricesExactlyEqual(originalYMatrix, decodedYMatrix, "Y", iteration);

            System.out.println("Verifying Cb matrix...");
            verifyMatricesExactlyEqual(originalCbMatrix, decodedCbMatrix, "Cb", iteration);

            System.out.println("Verifying Cr matrix...");
            verifyMatricesExactlyEqual(originalCrMatrix, decodedCrMatrix, "Cr", iteration);

            System.out.println("✓ Iteration " + iteration + " passed: All matrices decoded losslessly!");
        }

        System.out.println("\n=== All " + NUM_ITERATIONS + " iterations passed! zigZagRLE is lossless! ===");
    }

    /**
     * Test with tile-based encoding (similar to PacketGenerator).
     * Encodes matrix in TILE_SIZE x TILE_SIZE blocks.
     */
    @Test
    public void testZigZagRLEWithTiledEncoding() {
        final int width = 128;
        final int height = 128;

        System.out.println("Testing tiled zigZagRLE encoding with dimensions: " + height + "x" + width);

        final IRLE enDeRLE = EncodeDecodeRLEHuffman.getInstance();

        // Create a large random matrix
        final short[][] originalMatrix = createRandomShortMatrix(height, width);

        // Calculate number of tiles
        final int tilesX = (int) Math.ceil((double) width / TILE_SIZE);
        final int tilesY = (int) Math.ceil((double) height / TILE_SIZE);

        System.out.println("Number of tiles: " + tilesX + " x " + tilesY + " = " + (tilesX * tilesY));

        // Encode each tile separately (similar to PacketGenerator lines 78-92)
        for (int ty = 0; ty < tilesY; ty++) {
            for (int tx = 0; tx < tilesX; tx++) {
                final int x = tx * TILE_SIZE;
                final int y = ty * TILE_SIZE;
                final int w = Math.min(TILE_SIZE, width - x);
                final int h = Math.min(TILE_SIZE, height - y);

                System.out.println("\nTile [" + tx + "," + ty + "]: " +
                    "position=(" + x + "," + y + ") size=" + w + "x" + h);

                // Extract tile from original matrix
                final short[][] tile = extractTile(originalMatrix, x, y, w, h);

                // Encode tile
                final int maxLen = calculateMaxBufferSize(h, w);
                final ByteBuffer resRLEBuffer = ByteBuffer.allocate(maxLen);
                enDeRLE.zigZagRLE(tile, resRLEBuffer);

                final int encodedSize = resRLEBuffer.position();
                System.out.println("Encoded size: " + encodedSize + " bytes");

                // Decode tile
                final byte[] encodedData = new byte[encodedSize];
                resRLEBuffer.rewind();
                resRLEBuffer.get(encodedData);

                final ByteBuffer decodeBuffer = ByteBuffer.wrap(encodedData);
                final short[][] decodedTile = enDeRLE.revZigZagRLE(decodeBuffer);

                // Verify tile matches
                verifyMatricesExactlyEqual(tile, decodedTile, "Tile[" + tx + "," + ty + "]", 1);
                System.out.println("✓ Tile decoded losslessly");
            }
        }

        System.out.println("\n=== All tiles encoded and decoded losslessly! ===");
    }

    /**
     * Test with edge cases: very small and very large values.
     */
    @Test
    public void testZigZagRLEWithEdgeCases() {
        final int width = 64;
        final int height = 64;

        final IRLE enDeRLE = EncodeDecodeRLEHuffman.getInstance();

        // Test case 1: All zeros
        System.out.println("\n=== Test Case 1: All Zeros ===");
        short[][] allZeros = new short[height][width];
        testMatrixEncodeDecode(allZeros, enDeRLE, "AllZeros");

        // Test case 2: All maximum values
        System.out.println("\n=== Test Case 2: All Max Values ===");
        short[][] allMax = createConstantMatrix(height, width, (short) 127);
        testMatrixEncodeDecode(allMax, enDeRLE, "AllMax");

        // Test case 3: All minimum values
        System.out.println("\n=== Test Case 3: All Min Values ===");
        short[][] allMin = createConstantMatrix(height, width, (short) -128);
        testMatrixEncodeDecode(allMin, enDeRLE, "AllMin");

        // Test case 4: Checkerboard pattern
        System.out.println("\n=== Test Case 4: Checkerboard Pattern ===");
        short[][] checkerboard = createCheckerboardMatrix(height, width);
        testMatrixEncodeDecode(checkerboard, enDeRLE, "Checkerboard");

        // Test case 5: Gradient pattern
        System.out.println("\n=== Test Case 5: Gradient Pattern ===");
        short[][] gradient = createGradientMatrix(height, width);
        testMatrixEncodeDecode(gradient, enDeRLE, "Gradient");

        System.out.println("\n=== All edge cases passed! ===");
    }

    /**
     * Helper method to test a single matrix encode/decode cycle.
     */
    private void testMatrixEncodeDecode(short[][] matrix, IRLE enDeRLE, String testName) {
        final int height = matrix.length;
        final int width = matrix[0].length;

        final int maxLen = calculateMaxBufferSize(height, width);
        final ByteBuffer resRLEBuffer = ByteBuffer.allocate(maxLen);

        enDeRLE.zigZagRLE(matrix, resRLEBuffer);
        final int encodedSize = resRLEBuffer.position();
        System.out.println("Encoded size: " + encodedSize + " bytes");

        final byte[] encodedData = new byte[encodedSize];
        resRLEBuffer.rewind();
        resRLEBuffer.get(encodedData);

        final ByteBuffer decodeBuffer = ByteBuffer.wrap(encodedData);
        final short[][] decodedMatrix = enDeRLE.revZigZagRLE(decodeBuffer);

        verifyMatricesExactlyEqual(matrix, decodedMatrix, testName, 1);
        System.out.println("✓ " + testName + " passed");
    }

    /**
     * Creates a random short matrix with values in range [-128, 127].
     * This simulates the level-shifted YCbCr values (shifted by -128).
     */
    private short[][] createRandomShortMatrix(final int height, final int width) {
        final short[][] matrix = new short[height][width];

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                // Random values in range [-128, 127] to simulate level-shifted YCbCr
                matrix[i][j] = (short) (RANDOM.nextInt(256) - 128);
            }
        }

        return matrix;
    }

    /**
     * Creates a constant matrix filled with a specific value.
     */
    private short[][] createConstantMatrix(final int height, final int width, final short value) {
        final short[][] matrix = new short[height][width];

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                matrix[i][j] = value;
            }
        }

        return matrix;
    }

    /**
     * Creates a checkerboard pattern matrix.
     */
    private short[][] createCheckerboardMatrix(final int height, final int width) {
        final short[][] matrix = new short[height][width];

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                matrix[i][j] = (short) ((i + j) % 2 == 0 ? 100 : -100);
            }
        }

        return matrix;
    }

    /**
     * Creates a gradient pattern matrix.
     */
    private short[][] createGradientMatrix(final int height, final int width) {
        final short[][] matrix = new short[height][width];

        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                // Gradient from -128 to 127
                matrix[i][j] = (short) ((i * 255 / height) - 128);
            }
        }

        return matrix;
    }

    /**
     * Extracts a tile from a matrix.
     */
    private short[][] extractTile(final short[][] matrix, final int x, final int y,
                                   final int width, final int height) {
        // Make sure dimensions are multiples of 8 (required for zigZagRLE)
        final int alignedHeight = ((height + 7) / 8) * 8;
        final int alignedWidth = ((width + 7) / 8) * 8;

        final short[][] tile = new short[alignedHeight][alignedWidth];

        for (int i = 0; i < height && (y + i) < matrix.length; i++) {
            for (int j = 0; j < width && (x + j) < matrix[0].length; j++) {
                tile[i][j] = matrix[y + i][x + j];
            }
        }

        return tile;
    }

    /**
     * Generates a random dimension that is a multiple of 8.
     */
    private int generateRandomMultipleOf8(final int min, final int max) {
        int dimension = RANDOM.nextInt(max - min + 1) + min;
        // Round to nearest multiple of 8
        dimension = ((dimension + 7) / 8) * 8;
        return dimension;
    }

    /**
     * Calculates maximum buffer size needed for encoding.
     * Based on JpegCodec constructor calculation.
     */
    private int calculateMaxBufferSize(final int height, final int width) {
        // Conservative estimate: 3 channels * 1.5 factor * 4 bytes per pixel + metadata
        return (int) ((height * width * 1.5 * 4) + (4 * 3) + 0.5);
    }

    /**
     * Verifies that two short matrices are exactly equal (no tolerance).
     * zigZagRLE should be completely lossless.
     */
    private void verifyMatricesExactlyEqual(final short[][] original,
                                            final short[][] decoded,
                                            final String matrixName,
                                            final int iteration) {
        assertNotNull(decoded, matrixName + " decoded matrix is null for iteration " + iteration);

        assertEquals(original.length, decoded.length,
            matrixName + " height mismatch for iteration " + iteration);

        assertEquals(original[0].length, decoded[0].length,
            matrixName + " width mismatch for iteration " + iteration);

        int mismatches = 0;
        int totalPixels = 0;

        for (int i = 0; i < original.length; i++) {
            for (int j = 0; j < original[0].length; j++) {
                totalPixels++;

                if (original[i][j] != decoded[i][j]) {
                    mismatches++;

                    // Fail immediately on first mismatch with detailed info
                    fail(String.format(
                        "%s matrix mismatch at [%d,%d] for iteration %d: " +
                        "expected=%d, actual=%d (difference=%d)",
                        matrixName, i, j, iteration,
                        original[i][j], decoded[i][j],
                        Math.abs(original[i][j] - decoded[i][j])
                    ));
                }
            }
        }

        System.out.println(String.format(
            "%s matrix: %d pixels verified, %d mismatches (%.2f%% match)",
            matrixName, totalPixels, mismatches, 100.0 * (totalPixels - mismatches) / totalPixels
        ));

        assertEquals(0, mismatches,
            String.format("%s matrix has %d mismatches out of %d pixels",
                matrixName, mismatches, totalPixels));
    }
}


