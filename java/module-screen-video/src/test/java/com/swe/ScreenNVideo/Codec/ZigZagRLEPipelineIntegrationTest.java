package com.swe.ScreenNVideo.Codec;

import com.swe.ScreenNVideo.PatchGenerator.CompressedPatch;
import com.swe.ScreenNVideo.Model.CPackets;
import com.swe.ScreenNVideo.Utils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test that combines tiling, CPackets serialization, and zigZagRLE decoding.
 * Tests the full pipeline without the lossy RGB-YCbCr conversion.
 */
public class ZigZagRLEPipelineIntegrationTest {

    private static final int TILE_SIZE = 64;
    private static final String TEST_IP = "127.0.0.1";
    private static final int NUM_FEED_ITERATIONS = 100000;
    private static final Random RANDOM = new Random(42); // Fixed seed for reproducibility

    /**
     * Full pipeline test that:
     * 1. Generates random short[][] matrices (Y, Cr, Cb)
     * 2. Tiles them into 64x64 blocks
     * 3. Encodes each tile with zigZagRLE
     * 4. Creates CompressedPatch objects
     * 5. Creates CPackets and serializes them
     * 6. Deserializes CPackets
     * 7. Decodes patches using revZigZagRLE (no ImageSynchronizer)
     * 8. Manually stitches decoded tiles back
     * 9. Verifies lossless reconstruction
     */
    @Test
    public void testFullPipelineWithZigZagRLE() throws IOException {
        // Generate random dimensions (divisible by 2 and multiple of 8)
        final int width = generateRandomMultipleOf8(128, 256);
        final int height = generateRandomMultipleOf8(128, 256);

        System.out.println("Testing full pipeline with dimensions: " + height + "x" + width);

        final IRLE enDeRLE = EncodeDecodeRLEHuffman.getInstance();
        final boolean toCompress = false;

        // Process multiple feed numbers synchronously
        for (int feedNumber = 1; feedNumber <= NUM_FEED_ITERATIONS; feedNumber++) {
            System.out.println("\n=== Processing Feed Number: " + feedNumber + " ===");

            // Step 1: Create random Y, Cr, Cb matrices
            final short[][] originalYMatrix = createRandomShortMatrix(height, width);
            final int chromaHeight = height / 2;
            final int chromaWidth = width / 2;
            final short[][] originalCrMatrix = createRandomShortMatrix(chromaHeight, chromaWidth);
            final short[][] originalCbMatrix = createRandomShortMatrix(chromaHeight, chromaWidth);

            System.out.println("Y matrix: " + height + "x" + width);
            System.out.println("Cr/Cb matrices: " + chromaHeight + "x" + chromaWidth);

            // Step 2 & 3: Tile and encode all matrices together (Y, Cr, Cb per tile)
            final List<CompressedPatch> patches = encodeMatrixInTiles(
                originalYMatrix, originalCrMatrix, originalCbMatrix, enDeRLE, toCompress);

            System.out.println("Generated " + patches.size() + " patches (each containing Y, Cr, Cb)");

            // Step 4: Create CPackets
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

            // Step 5: Serialize CPackets
            byte[] encodedPatches = serializeWithRetry(networkPackets);
            assertNotNull(encodedPatches, "Failed to serialize compressed packets");
            System.out.println("Serialized size: " + encodedPatches.length / Utils.KB + " KB");

            // Step 6: Deserialize CPackets
            final CPackets deserializedPackets = CPackets.deserialize(encodedPatches);
            assertNotNull(deserializedPackets);
            assertEquals(feedNumber, deserializedPackets.packetNumber());
            assertEquals(height, deserializedPackets.height());
            assertEquals(width, deserializedPackets.width());

            // Step 7: Decode patches and verify directly (no stitching)
            decodePatchesAndVerify(
                deserializedPackets.packets(),
                originalYMatrix, originalCrMatrix, originalCbMatrix,
                enDeRLE, feedNumber);

            System.out.println("\n✓ Feed " + feedNumber + " processed successfully!");
        }

        System.out.println("\n=== All " + NUM_FEED_ITERATIONS + " feeds processed successfully! ===");
    }

    /**
     * Encodes Y, Cr, Cb matrices in tiles (similar to PacketGenerator.generatePackets).
     * Each tile contains Y, Cr, Cb encoded with zigZagRLE.
     */
    private List<CompressedPatch> encodeMatrixInTiles(
            final short[][] yMatrix,
            final short[][] crMatrix,
            final short[][] cbMatrix,
            final IRLE enDeRLE,
            final boolean toCompress) {

        final int height = yMatrix.length;
        final int width = yMatrix[0].length;

        // Calculate number of tiles
        final int tilesX = (int) Math.ceil((double) width / TILE_SIZE);
        final int tilesY = (int) Math.ceil((double) height / TILE_SIZE);

        final List<CompressedPatch> patches = new ArrayList<>();

        // Encode each tile (similar to JpegCodecIntegrationTest lines 159-171)
        for (int ty = 0; ty < tilesY; ty++) {
            for (int tx = 0; tx < tilesX; tx++) {
                final int x = tx * TILE_SIZE;
                final int y = ty * TILE_SIZE;
                final int w = Math.min(TILE_SIZE, width - x);
                final int h = Math.min(TILE_SIZE, height - y);

                // Extract Y tile from matrix
                final short[][] yTile = extractTile(yMatrix, x, y, w, h);

                // Extract Cr and Cb tiles (half size for chroma)
                final int chromaX = x / 2;
                final int chromaY = y / 2;
                final int chromaW = w / 2;
                final int chromaH = h / 2;
                final short[][] crTile = extractTile(crMatrix, chromaX, chromaY, chromaW, chromaH);
                final short[][] cbTile = extractTile(cbMatrix, chromaX, chromaY, chromaW, chromaH);

                // Encode Y, Cr, Cb with zigZagRLE into same buffer
                final int maxLen = calculateMaxBufferSize(h, w) * 3; // 3 channels
                final ByteBuffer resRLEBuffer = ByteBuffer.allocate(maxLen);
                
                // Encode Y matrix
                enDeRLE.zigZagRLE(yTile, resRLEBuffer);
                // Encode Cb matrix
                enDeRLE.zigZagRLE(cbTile, resRLEBuffer);
                // Encode Cr matrix
                enDeRLE.zigZagRLE(crTile, resRLEBuffer);

                // Get encoded bytes
                final byte[] compressedData = new byte[resRLEBuffer.position()];
                resRLEBuffer.rewind();
                resRLEBuffer.get(compressedData);

                patches.add(new CompressedPatch(x, y, w, h, compressedData));
            }
        }

        return patches;
    }

    /**
     * Decodes patches and verifies them directly against original matrices.
     * Similar to ImageSynchronizer lines 89-94 but without stitching.
     * Each patch is decoded to Y, Cr, Cb and compared with corresponding tile from originals.
     */
    private void decodePatchesAndVerify(
            final List<CompressedPatch> patches,
            final short[][] originalYMatrix,
            final short[][] originalCrMatrix,
            final short[][] originalCbMatrix,
            final IRLE enDeRLE,
            final int feedNumber) {

        int patchCount = 0;
        
        // Decode each patch (similar to ImageSynchronizer.synchronize)
        for (CompressedPatch compressedPatch : patches) {
            patchCount++;
            
            final int x = compressedPatch.x();
            final int y = compressedPatch.y();
            final int w = compressedPatch.width();
            final int h = compressedPatch.height();
            
            System.out.println("\nVerifying patch " + patchCount + "/" + patches.size() + 
                " at position (" + x + "," + y + ") size " + w + "x" + h);
            
            // Decode using revZigZagRLE (decodes Y, Cb, Cr in order)
            final ByteBuffer decodeBuffer = ByteBuffer.wrap(compressedPatch.data());
            final short[][] decodedYTile = enDeRLE.revZigZagRLE(decodeBuffer);
            final short[][] decodedCbTile = enDeRLE.revZigZagRLE(decodeBuffer);
            final short[][] decodedCrTile = enDeRLE.revZigZagRLE(decodeBuffer);

            // Extract corresponding tiles from original matrices
            final short[][] originalYTile = extractTile(originalYMatrix, x, y, w, h);
            
            final int chromaX = x / 2;
            final int chromaY = y / 2;
            final int chromaW = w / 2;
            final int chromaH = h / 2;
            final short[][] originalCrTile = extractTile(originalCrMatrix, chromaX, chromaY, chromaW, chromaH);
            final short[][] originalCbTile = extractTile(originalCbMatrix, chromaX, chromaY, chromaW, chromaH);

            // Verify Y tile
            verifyMatricesExactlyEqual(originalYTile, decodedYTile, 
                "Y tile[" + patchCount + "]", feedNumber);
            
            // Verify Cb tile
            verifyMatricesExactlyEqual(originalCbTile, decodedCbTile, 
                "Cb tile[" + patchCount + "]", feedNumber);
            
            // Verify Cr tile
            verifyMatricesExactlyEqual(originalCrTile, decodedCrTile, 
                "Cr tile[" + patchCount + "]", feedNumber);
            
            System.out.println("✓ Patch " + patchCount + " verified successfully!");
        }
        
        System.out.println("\n✓✓ All " + patchCount + " patches verified losslessly!");
    }


    /**
     * Extracts a tile from a matrix with proper alignment.
     */
    private short[][] extractTile(
            final short[][] matrix,
            final int x,
            final int y,
            final int width,
            final int height) {

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
     * Serializes CPackets with retry logic.
     */
    private byte[] serializeWithRetry(final CPackets networkPackets) throws IOException {
        byte[] encodedPatches = null;
        int tries = Utils.MAX_TRIES_TO_SERIALIZE;
        while (tries-- > 0) {
            try {
                encodedPatches = networkPackets.serializeCPackets();
                break;
            } catch (IOException e) {
                if (tries == 0) {
                    throw e;
                }
                e.printStackTrace();
            }
        }
        return encodedPatches;
    }

    /**
     * Creates a random short matrix with values in range [-128, 127].
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
     */
    private int calculateMaxBufferSize(final int height, final int width) {
        return (int) ((height * width * 1.5 * 4) + (4 * 3) + 0.5);
    }

    /**
     * Verifies that two short matrices are exactly equal (no tolerance).
     */
    private void verifyMatricesExactlyEqual(
            final short[][] original,
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
            "%s matrix: %d pixels verified, %d mismatches (100.00%% match)",
            matrixName, totalPixels, mismatches
        ));

        assertEquals(0, mismatches,
            String.format("%s matrix has %d mismatches out of %d pixels",
                matrixName, mismatches, totalPixels));
    }

    /**
     * Test with specific small dimensions.
     */
    @Test
    public void testPipelineWithSmallDimensions() throws IOException {
        final int width = 64;
        final int height = 64;

        System.out.println("Testing pipeline with small dimensions: " + height + "x" + width);

        final IRLE enDeRLE = EncodeDecodeRLEHuffman.getInstance();
        final boolean toCompress = false;

        final short[][] originalYMatrix = createRandomShortMatrix(height, width);
        final int chromaHeight = height / 2;
        final int chromaWidth = width / 2;
        final short[][] originalCrMatrix = createRandomShortMatrix(chromaHeight, chromaWidth);
        final short[][] originalCbMatrix = createRandomShortMatrix(chromaHeight, chromaWidth);

        final List<CompressedPatch> patches = encodeMatrixInTiles(
            originalYMatrix, originalCrMatrix, originalCbMatrix, enDeRLE, toCompress);

        assertEquals(1, patches.size(), "Should have exactly 1 patch for 64x64 matrix");

        final CPackets networkPackets = new CPackets(
            1, TEST_IP, false, toCompress, height, width, patches);

        byte[] encodedPatches = serializeWithRetry(networkPackets);
        assertNotNull(encodedPatches);

        final CPackets deserializedPackets = CPackets.deserialize(encodedPatches);
        
        decodePatchesAndVerify(
            deserializedPackets.packets(),
            originalYMatrix, originalCrMatrix, originalCbMatrix,
            enDeRLE, 1);

        System.out.println("✓ Small dimensions test passed!");
    }

    /**
     * Test with non-aligned dimensions.
     */
    @Test
    public void testPipelineWithNonAlignedDimensions() throws IOException {
        final int width = 130; // Not a multiple of 64
        final int height = 98;  // Not a multiple of 64, but aligned to 8

        System.out.println("Testing pipeline with non-aligned dimensions: " + height + "x" + width);

        final IRLE enDeRLE = EncodeDecodeRLEHuffman.getInstance();
        final boolean toCompress = false;

        final short[][] originalYMatrix = createRandomShortMatrix(height, width);
        final int chromaHeight = height / 2;
        final int chromaWidth = width / 2;
        final short[][] originalCrMatrix = createRandomShortMatrix(chromaHeight, chromaWidth);
        final short[][] originalCbMatrix = createRandomShortMatrix(chromaHeight, chromaWidth);

        final List<CompressedPatch> patches = encodeMatrixInTiles(
            originalYMatrix, originalCrMatrix, originalCbMatrix, enDeRLE, toCompress);

        System.out.println("Generated " + patches.size() + " patches");

        final CPackets networkPackets = new CPackets(
            1, TEST_IP, false, toCompress, height, width, patches);

        byte[] encodedPatches = serializeWithRetry(networkPackets);
        assertNotNull(encodedPatches);

        final CPackets deserializedPackets = CPackets.deserialize(encodedPatches);
        
        decodePatchesAndVerify(
            deserializedPackets.packets(),
            originalYMatrix, originalCrMatrix, originalCbMatrix,
            enDeRLE, 1);

        System.out.println("✓ Non-aligned dimensions test passed!");
    }
}

