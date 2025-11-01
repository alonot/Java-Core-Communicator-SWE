/**
 * Contributed by Aman Rahman Biswas.
 */

package com.swe.ScreenNVideo.PatchGenerator;

/**
 * Interface for computing a hash value of an image patch or tile.
 */
public interface IHasher {
    /**
     * Computes a hash for an image patch/tile.
     * @param img -> pixel data (int[][][]); (x, y)->location of the first corner; (w, h)->height and width
     * @param x -> x-coordinate of first corner
     * @param y -> y-coordinate of first corner
     * @param h -> height
     * @param w -> width
     * @return hash as a long
     */
    long hash(int[][] img, int x, int y, int w, int h);
}
