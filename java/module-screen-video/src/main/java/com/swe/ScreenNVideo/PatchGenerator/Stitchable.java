/**
 * Contributed by Devansh Manoj Kesan.
 */

package com.swe.ScreenNVideo.PatchGenerator;

/**
 * Interface for stitchable objects.
 */
public interface Stitchable {
    void applyOn(int[][] canvas);

    int getHeight();

    int getWidth();

    int getX();
    
    int getY();
}