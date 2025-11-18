package com.swe.ScreenNVideo.PatchGenerator;

import com.swe.ScreenNVideo.Codec.Codec;
import com.swe.ScreenNVideo.Model.FeedPatch;
import com.swe.ScreenNVideo.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates packets for image/patch transmission by dividing images into tiles.
 * Uses a compressor to encode patches and a hasher to detect changes between frames.
 * Maintains a cache of previous hashes for efficient patch generation.
 */
public class PacketGenerator {

    /** Default size of each tile in pixels. */
    private static final int TILE_SIZE = 64;

    /** Compressor used to encode image patches. */
    private final Codec compressor;

    /** Hasher used to compute hashes of image patches. */
    private final IHasher hasher;

    /** Stores previous hash values for each tile in the grid. */
    private long[][] prevHashes;


    public PacketGenerator(final Codec compressorArg, final IHasher hasherArg) {
        this.compressor = compressorArg;
        this.hasher = hasherArg;
    }

    /**
     * Generate a full image patch covering the entire frame.
     * @param curr is the image frame (int[width][height][3] RGB array)
     * @return list containing a single compressed patch for the full image
     */
    public FeedPatch generateFullImage(final int[][] curr) {
        final int height = curr.length;
        final int width = curr[0].length;

        final List<byte[]> compressedString = this.compressor.encode(curr, 0, 0, height, width);
        final List<CompressedPatch> compressedPatches = new ArrayList<>();
        final List<CompressedPatch> unCompressedPatches = new ArrayList<>();
        // add the compressed patch
        compressedPatches.add(new CompressedPatch(0, 0, width, height, compressedString.get(0)));
        // add uncompressed patch
        unCompressedPatches.add(new CompressedPatch(0, 0, width, height, compressedString.get(1)));
        return new FeedPatch(compressedPatches, unCompressedPatches);
    }

    /**
     * Split frames into tiles, compare hashes, compress dirty tiles.
     * @param curr is the image frame (int[width][height][3] RGB array)
     * @return list of compressed patches,
     */
    public FeedPatch generatePackets(final int[][] curr) {
        final int height = curr.length;
        final int width = curr[0].length;

        // Tile grid size
        final int tilesX = (int) Math.ceil((double) width / TILE_SIZE);
        final int tilesY = (int) Math.ceil((double) height / TILE_SIZE);

        // Initialize hash cache if first frame
        if (prevHashes == null) {
            prevHashes = new long[tilesX][tilesY];
        }

        if (tilesX != prevHashes.length || tilesY != prevHashes[0].length) {
            // Resize prevHashes array if needed
            final long[][] newPrevHashes = new long[tilesX][tilesY];
            Utils.copyMatrix(prevHashes, newPrevHashes);
            prevHashes = newPrevHashes;
        }

        final List<CompressedPatch> compressedPatches = new ArrayList<>();
        final List<CompressedPatch> unCompressedPatches = new ArrayList<>();

        for (int ty = 0; ty < tilesY; ty++) {
            for (int tx = 0; tx < tilesX; tx++) {
                final int x = tx * TILE_SIZE;
                final int y = ty * TILE_SIZE;
                final int w = Math.min(TILE_SIZE, width - x);
                final int h = Math.min(TILE_SIZE, height - y);

                final long currHash = hasher.hash(curr, x, y, w, h);
                if (currHash != prevHashes[tx][ty]) {
                    final List<byte[]> compressedString = this.compressor.encode(curr, x, y, h, w);
                    // add the compressed patch
                    compressedPatches.add(new CompressedPatch(x, y, w, h, compressedString.get(0)));
                    // add uncompressed patch
                    unCompressedPatches.add(new CompressedPatch(x, y, w, h, compressedString.get(1)));
                }
                prevHashes[tx][ty] = currHash;
            }
        }
        return new FeedPatch(compressedPatches, unCompressedPatches);
    }

}

