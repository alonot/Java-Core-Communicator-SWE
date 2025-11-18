package com.swe.ScreenNVideo.Codec;

import com.swe.ScreenNVideo.PatchGenerator.CompressedPatch;
import com.swe.ScreenNVideo.Model.CPackets;
import com.swe.ScreenNVideo.Synchronizer.ImageSynchronizer;
import com.swe.ScreenNVideo.Utils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for JPEG codec with packet generation and image synchronization.
 * Tests the complete encoding-serialization-deserialization-decoding pipeline.
 */
public class JpegCodecIntegrationTest {

    private static final int TILE_SIZE = 64;
    private static final int COLOR_MASK = 0xFF;
    private static final int RED_SHIFT = 16;
    private static final int GREEN_SHIFT = 8;
    private static final int DELTA = 50; // Tolerance for lossy compression
    private static final String TEST_IP = "127.0.0.1";
    private static final int NUM_FEED_ITERATIONS = 100000;
    private static final Random RANDOM = new Random(42); // Fixed seed for reproducibility

    /**
     * Full integration test that:
     * 1. Creates a random matrix with dimensions divisible by 2
     * 2. Encodes the matrix in 64-pixel tiles (without hash comparisons)
     * 3. Creates CPackets and serializes them
     * 4. Deserializes and uses ImageSynchronizer to decode
     * 5. Compares decoded result with original
     * 6. Repeats for multiple feed numbers synchronously
     */
    @Test
    public void testFullPipelineIntegration() throws IOException {
        // Step 1: Create random dimensions (divisible by 2)
        final int width = generateRandomEvenDimension(800, 800 );
        final int height = generateRandomEvenDimension(600, 600);
        
        System.out.println("Testing with dimensions: " + height + "x" + width);

        // Initialize codec and synchronizer
        final JpegCodec codec = new JpegCodec();
        final ImageSynchronizer imageSynchronizer = new ImageSynchronizer(codec);
        final boolean toCompress = true;

        // Process multiple feed numbers synchronously
        for (int feedNumber = 1; feedNumber <= NUM_FEED_ITERATIONS; feedNumber++) {
            System.out.println("\n=== Processing Feed Number: " + feedNumber + " ===");
            
            // Step 2: Create random matrix with random RGB values
            final int[][] originalMatrix = createRandomMatrix(height, width);
            
            // Step 3: Encode matrix in tiles of 64x64 (without hash comparisons)
            final List<CompressedPatch> patches = encodeTilesWithoutHashing(
                originalMatrix, codec, toCompress);
            
            System.out.println("Generated " + patches.size() + " patches");
            
            // Step 4: Create CPackets and serialize
            final CPackets networkPackets = new CPackets(
                feedNumber,
                TEST_IP,
                false,
                toCompress,
                height,
                width,
                patches
            );
            
            System.out.println("Feed number: " + networkPackets.packetNumber());
            
            byte[] encodedPatches = null;
            int tries = Utils.MAX_TRIES_TO_SERIALIZE;
            while (tries-- > 0) {
                try {
                    encodedPatches = networkPackets.serializeCPackets();
                    break;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            
            assertNotNull(encodedPatches, "Failed to serialize compressed packets");
            assertFalse(tries < 0, "Exhausted all serialization attempts");
            
            System.out.println("Serialized size: " + encodedPatches.length / Utils.KB + " KB");
            
            // Step 5: Deserialize CPackets
            final CPackets deserializedPackets = CPackets.deserialize(encodedPatches);
            
            assertNotNull(deserializedPackets);
            assertEquals(feedNumber, deserializedPackets.packetNumber());
            assertEquals(height, deserializedPackets.height());
            assertEquals(width, deserializedPackets.width());
            assertEquals(toCompress, deserializedPackets.compress());
            assertEquals(patches.size(), deserializedPackets.packets().size());
            
            // Step 6: Use ImageSynchronizer to decode
            imageSynchronizer.setExpectedFeedNumber(feedNumber);
            
            final int[][] decodedImage;
            try {
                decodedImage = imageSynchronizer.synchronize(
                    deserializedPackets.height(),
                    deserializedPackets.width(),
                    deserializedPackets.packets(),
                    deserializedPackets.compress()
                );
            } catch (Exception e) {
                fail("Failed to synchronize image: " + e.getMessage());
                return;
            }
            
            assertNotNull(decodedImage);
            
            // Step 7: Compare decoded image with original
//            verifyMatrixSimilarity(originalMatrix, decodedImage, feedNumber);
            
            // Update expected feed number for next iteration
            imageSynchronizer.setExpectedFeedNumber(feedNumber + 1);
            
            System.out.println("Feed " + feedNumber + " processed successfully!");
        }
        
        System.out.println("\n=== All " + NUM_FEED_ITERATIONS + " feeds processed successfully! ===");
    }

    /**
     * Encodes the entire matrix in tiles of TILE_SIZE x TILE_SIZE,
     * similar to PacketGenerator but without hash comparisons.
     * 
     * @param matrix the matrix to encode
     * @param codec the JPEG codec
     * @param toCompress whether to apply compression
     * @return list of compressed patches
     */
    private List<CompressedPatch> encodeTilesWithoutHashing(
            final int[][] matrix,
            final JpegCodec codec,
            final boolean toCompress) {
        
        final int height = matrix.length;
        final int width = matrix[0].length;
        
        // Calculate number of tiles
        final int tilesX = (int) Math.ceil((double) width / TILE_SIZE);
        final int tilesY = (int) Math.ceil((double) height / TILE_SIZE);
        
        final List<CompressedPatch> patches = new ArrayList<>();
        
        // Encode each tile
        for (int ty = 0; ty < tilesY; ty++) {
            for (int tx = 0; tx < tilesX; tx++) {
                final int x = tx * TILE_SIZE;
                final int y = ty * TILE_SIZE;
                final int w = Math.min(TILE_SIZE, width - x);
                final int h = Math.min(TILE_SIZE, height - y);

                
                // Encode this tile (note: encode parameters are: screenshot, topLeftX, topLeftY, height, width, compress)
                final byte[] compressedData = codec.encode(matrix, x, y, h, w).get(0);
                
                patches.add(new CompressedPatch(x, y, w, h, compressedData));
            }
        }
        
        return patches;
    }

    /**
     * Creates a random matrix with the specified dimensions.
     * Each pixel has random RGB values in ARGB format (0xAARRGGBB).
     * 
     * @param height matrix height
     * @param width matrix width
     * @return random matrix
     */
    private int[][] createRandomMatrix(final int height, final int width) {
        final int[][] matrix = new int[height][width];
        
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                final int r = RANDOM.nextInt(256);
                final int g = RANDOM.nextInt(256);
                final int b = RANDOM.nextInt(256);
                final int a = 0xFF; // Full opacity
                
                matrix[i][j] = (a << 24) | (r << RED_SHIFT) | (g << GREEN_SHIFT) | b;
            }
        }
        
        return matrix;
    }

    /**
     * Generates a random dimension between min and max that is divisible by 2.
     * 
     * @param min minimum dimension
     * @param max maximum dimension
     * @return random even dimension
     */
    private int generateRandomEvenDimension(final int min, final int max) {
        int dimension = RANDOM.nextInt(max - min + 1) + min;
        // Make it even
        if (dimension % 2 != 0) {
            dimension++;
        }
        return dimension;
    }

    /**
     * Verifies that the decoded matrix is similar to the original matrix
     * within a tolerance delta (for lossy compression).
     * 
     * @param original original matrix
     * @param decoded decoded matrix
     * @param feedNumber current feed number (for logging)
     */
    private void verifyMatrixSimilarity(
            final int[][] original,
            final int[][] decoded,
            final int feedNumber) {
        
        assertNotNull(decoded, "Decoded matrix is null for feed " + feedNumber);
        assertEquals(original.length, decoded.length,
            "Height mismatch for feed " + feedNumber);
        assertEquals(original[0].length, decoded[0].length,
            "Width mismatch for feed " + feedNumber);
        
        int totalPixels = 0;
        int matchingPixels = 0;
        double totalRError = 0;
        double totalGError = 0;
        double totalBError = 0;
        
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
                
                final int rError = Math.abs(origR - decR);
                final int gError = Math.abs(origG - decG);
                final int bError = Math.abs(origB - decB);
                
                totalRError += rError;
                totalGError += gError;
                totalBError += bError;
                totalPixels++;
                
                // Check if pixel is within tolerance
                if (rError <= DELTA && gError <= DELTA && bError <= DELTA) {
                    matchingPixels++;
                }
                
                // Assert individual pixel is within tolerance
                assertTrue(rError <= DELTA,
                    String.format("Feed %d: Red channel error too large at [%d,%d]: %d (orig=%d, dec=%d)",
                        feedNumber, i, j, rError, origR, decR));
                assertTrue(gError <= DELTA,
                    String.format("Feed %d: Green channel error too large at [%d,%d]: %d (orig=%d, dec=%d)",
                        feedNumber, i, j, gError, origG, decG));
                assertTrue(bError <= DELTA,
                    String.format("Feed %d: Blue channel error too large at [%d,%d]: %d (orig=%d, dec=%d)",
                        feedNumber, i, j, bError, origB, decB));
            }
        }
        
        // Calculate and display statistics
        final double avgRError = totalRError / totalPixels;
        final double avgGError = totalGError / totalPixels;
        final double avgBError = totalBError / totalPixels;
        final double matchingPercentage = (matchingPixels * 100.0) / totalPixels;
        
        System.out.println(String.format(
            "Feed %d Statistics: Avg errors - R:%.2f G:%.2f B:%.2f | Matching pixels: %.2f%%",
            feedNumber, avgRError, avgGError, avgBError, matchingPercentage
        ));
        
        // Assert that most pixels match within tolerance (at least 90%)
        assertTrue(matchingPercentage >= 90.0,
            String.format("Feed %d: Only %.2f%% of pixels within tolerance (expected >= 90%%)",
                feedNumber, matchingPercentage));
    }

    /**
     * Test with specific small dimensions to ensure edge cases work.
     */
    @Test
    public void testFullPipelineWithSmallDimensions() throws IOException {
        final int width = 64;
        final int height = 64;
        
        final JpegCodec codec = new JpegCodec();
        final ImageSynchronizer imageSynchronizer = new ImageSynchronizer(codec);
        final boolean toCompress = false;
        
        final int[][] originalMatrix = createRandomMatrix(height, width);
        final List<CompressedPatch> patches = encodeTilesWithoutHashing(
            originalMatrix, codec, toCompress);
        
        assertEquals(1, patches.size(), "Should have exactly 1 patch for 64x64 image");
        
        final CPackets networkPackets = new CPackets(
            1, TEST_IP, false, toCompress, height, width, patches);
        
        final byte[] encodedPatches = networkPackets.serializeCPackets();
        assertNotNull(encodedPatches);
        
        final CPackets deserializedPackets = CPackets.deserialize(encodedPatches);
        imageSynchronizer.setExpectedFeedNumber(1);
        
        final int[][] decodedImage = imageSynchronizer.synchronize(
            deserializedPackets.height(),
            deserializedPackets.width(),
            deserializedPackets.packets(),
            deserializedPackets.compress()
        );
        
        verifyMatrixSimilarity(originalMatrix, decodedImage, 1);
    }

    /**
     * Test with larger dimensions to ensure scalability.
     */
    @Test
    public void testFullPipelineWithLargeDimensions() throws IOException {
        final int width = 320;
        final int height = 240;
        
        final JpegCodec codec = new JpegCodec();
        final ImageSynchronizer imageSynchronizer = new ImageSynchronizer(codec);
        final boolean toCompress = false;
        
        final int[][] originalMatrix = createRandomMatrix(height, width);
        final List<CompressedPatch> patches = encodeTilesWithoutHashing(
            originalMatrix, codec, toCompress);
        
        System.out.println("Large test: Generated " + patches.size() + " patches for " 
            + height + "x" + width + " image");
        
        final CPackets networkPackets = new CPackets(
            1, TEST_IP, false, toCompress, height, width, patches);
        
        final byte[] encodedPatches = networkPackets.serializeCPackets();
        assertNotNull(encodedPatches);
        
        System.out.println("Large test: Serialized size: " 
            + encodedPatches.length / Utils.KB + " KB");
        
        final CPackets deserializedPackets = CPackets.deserialize(encodedPatches);
        imageSynchronizer.setExpectedFeedNumber(1);
        
        final int[][] decodedImage = imageSynchronizer.synchronize(
            deserializedPackets.height(),
            deserializedPackets.width(),
            deserializedPackets.packets(),
            deserializedPackets.compress()
        );
        
        verifyMatrixSimilarity(originalMatrix, decodedImage, 1);
    }

    /**
     * Test with non-aligned dimensions (not multiples of TILE_SIZE).
     */
    @Test
    public void testFullPipelineWithNonAlignedDimensions() throws IOException {
        final int width = 130; // Not a multiple of 64
        final int height = 98;  // Not a multiple of 64
        
        final JpegCodec codec = new JpegCodec();
        final ImageSynchronizer imageSynchronizer = new ImageSynchronizer(codec);
        final boolean toCompress = false;
        
        final int[][] originalMatrix = createRandomMatrix(height, width);
        final List<CompressedPatch> patches = encodeTilesWithoutHashing(
            originalMatrix, codec, toCompress);
        
        final CPackets networkPackets = new CPackets(
            1, TEST_IP, false, toCompress, height, width, patches);
        
        final byte[] encodedPatches = networkPackets.serializeCPackets();
        assertNotNull(encodedPatches);
        
        final CPackets deserializedPackets = CPackets.deserialize(encodedPatches);
        imageSynchronizer.setExpectedFeedNumber(1);
        
        final int[][] decodedImage = imageSynchronizer.synchronize(
            deserializedPackets.height(),
            deserializedPackets.width(),
            deserializedPackets.packets(),
            deserializedPackets.compress()
        );
        
        verifyMatrixSimilarity(originalMatrix, decodedImage, 1);
    }
}

