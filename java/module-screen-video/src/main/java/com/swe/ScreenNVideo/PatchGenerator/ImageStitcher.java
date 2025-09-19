package com.swe.ScreenNVideo.PatchGenerator;

import java.util.List;

/**
 *Represents a patch of pixels that can be applied to a canvas.
 */
class Patch implements Stitchable {

    /** Pixel data for this patch. */
    private final int[][][] pixels;

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
    Patch(final int[][][] patchPixels, final int posX, final int posY) {
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
    public void applyOn(final int[][][] canvas) {
        for (int i = 0; i < pixels.length; i++) {
            for (int j = 0; j < pixels[0].length; j++) {
                final int targetX = x + i;
                final int targetY = y + j;


                if (targetX >= 0
                    && targetX < canvas.length
                    && targetY >= 0
                    && targetY < canvas[0].length) {

                    System.arraycopy(pixels[i][j], 0, canvas[targetX][targetY], 0, pixels[i][j].length);
                }
            }
        }
    }
}

/**
 * Handles stitching multiple patches together into a single canvas.
 */
public class ImageStitcher {

    /** The target canvas for image stitching. */
    private final int[][][] canvas;

    /**
     * Creates a new {@code ImageStitcher}.
     *
     * @param initialCanvas is the target canvas
     */
    public ImageStitcher(final int[][][] initialCanvas) {
        this.canvas = initialCanvas;
    }

    /**
     * Stitches the provided patches list onto the canvas.
     *
     * @param patches the list of patches to apply
     */
    public void stitch(final List<Stitchable> patches) {
        for (Stitchable patch : patches) {
            patch.applyOn(canvas);
        }
    }

    /**
     * Returns the final stitched canvas.
     *
     * @return the canvas
     */
    public int[][][] getCanvas() {
        return canvas;
    }
}
