/**
 * Contributed by Devan Manoj Kesan.
 */

package com.swe.ScreenNVideo.PatchGenerator;

/**
 *Represents a patch of pixels that can be applied to a canvas.
 */
public class Patch implements Stitchable {

    /** Pixel data for this patch. */
    private final int[][] pixels;

    /** X-coordinate to place patch. */
    private final int x;

    /** Y-coordinate to place patch. */
    private final int y;

    /**
     * Creates a new {@code Patch}.
     *
     * @param patchPixels the pixel data of the patch
     * @param posX the x-coordinate
     * @param posY the y-coordinate
     */
    public Patch(final int[][] patchPixels, final int posX, final int posY) {
        this.pixels = patchPixels;
        this.x = posX;
        this.y = posY;
    }


    /**
     * Applies this patch onto the provided canvas.
     *
     * @param canvas is the target canvas
     */
    @Override
    public void applyOn(final int[][] canvas) {
        for (int i = 0; i < pixels.length; i++) {
            for (int j = 0; j < pixels[0].length; j++) {
                final int targetX = x + j;
                final int targetY = y + i;


                if (targetY >= 0
                    && targetY < canvas.length
                    && targetX >= 0
                    && targetX < canvas[0].length) {
                    canvas[targetY][targetX] = pixels[i][j];
                }
            }
        }
    }

    @Override
    public int getHeight() {
        return pixels.length;
    }

    @Override
    public int getWidth() {
        return pixels[0].length;
    }

    @Override
    public int getX() {
        return x;
    }

    @Override
    public int getY() {
        return y;
    }
}