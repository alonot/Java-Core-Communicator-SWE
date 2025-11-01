/**
 * Contributed by Aman Rahman Biswas.
 */

package com.swe.ScreenNVideo.PatchGenerator;

/**
 * Implementation of the IHasher interface.
 * Computes hashes for image patches using a configurable stride.
 */
public class Hasher implements IHasher {

    /**
     * Stride used for hashing the image patch.
     */
    private final int stride;

    /**
     * Offset for red color component in the AARRGGBB value.
     */
    private static final int R_OFFSET = 16;

    /**
     * Offset for green color component in the AARRGGBB value.
     */
    private static final int G_OFFSET = 8;

    /**
     * Bit shift for green channel when computing hash.
     */
    private static final int SHIFT1 = 20;

    /**
     * Bit shift for blue channel when computing hash.
     */
    private static final int SHIFT2 = 40;

    /**
     * Mask for extracting color componenet.
     */
    private static final int MASK = 0xFF;

    /**
     * Constructs a Hasher with a specified sampling stride.
     *
     * @param strideArg sampling step (e.g. 2 = every 2nd pixel, 4 = every 4th pixel)
     */
    public Hasher(final int strideArg) {
        this.stride = Math.max(1, strideArg); // avoid zero or negative
    }

    @Override
    public long hash(final int[][] img, final int x, final int y, final int w, final int h) {
        long hash = 0;
        for (int i = 0; i < w; i += stride) {
            for (int j = 0; j < h; j += stride) {
                final int pixel = img[y + j][x + i];
                final int r = (pixel >> R_OFFSET) & MASK;
                final int g = (pixel >> G_OFFSET) & MASK;
                final int b = pixel & MASK;
                hash += r;
                hash += (long) g << SHIFT1;
                hash += (long) b << SHIFT2;
            }
        }
        return hash;
    }
}
