package com.swe.ScreenNVideo.PatchGenerator;

import com.swe.ScreenNVideo.Utils;

import java.util.List;

/**
 * Handles stitching multiple patches together into a single canvas.
 */
public class ImageStitcher {

    /**
     * The target canvas for image stitching.
     */
    private int[][] canvas;

    /**
     * Height of current canvas.
     */
    private int currentHeight;

    /**
     * Width of current Canvas.
     */
    private int currentWidth;

    /**
     * Assigns new canvas.
     *
     * @param height of target canvas
     * @param width of target canvas
     */
    public void setCanvas(final int height, final int width) {
        this.canvas = new int[height][width];
        this.currentHeight = height;
        this.currentWidth = width;
    }

    /**
     * Assigns new canvas.
     *
     * @param initialCanvas to be assigned to canvas
     */
    public void setCanvas(final int[][] initialCanvas) {
        this.canvas = initialCanvas;
        this.currentHeight = initialCanvas.length;
        this.currentWidth = initialCanvas[0].length;
        System.out.println("Setting canvas " + currentHeight + " " + currentWidth);
    }

    /**
     * Set Canvas Dimensions
     * @param newHeight height of the updated canvas
     * @param newWidth width of the updated canvas
     */
    public void setCanvasDimensions(final int newHeight, final int newWidth) {
        if (this.currentHeight != newHeight && this.currentWidth != newWidth) {
            resize(newHeight, newWidth, true);
        }
    }

    /**
     * Resets the canvas to a empty canvas.
     */
    public void resetCanvas() {
        this.canvas = new int[0][0];
        this.currentHeight = 0;
        this.currentWidth = 0;
        System.out.println("Resetting canvas " + currentHeight + " " + currentWidth);
    }

    /**
     * Stitches the provided patches list onto the canvas.
     * Stretches the canvas if necessary.
     * @param patches the list of patches to apply
     */
    public void stitch(final List<Stitchable> patches) {
        for (Stitchable patch : patches) {
            stitch(patch);
        }
    }

    /**
     * Stitches the provided patch onto the canvas.
     * Stretches the canvas if necessary.
     * @param patch Compressed Patch to apply on the Canvas
     */
    public void stitch(final Stitchable patch) {
        verifyDimensions(patch);
        patch.applyOn(canvas);
    }

    private void verifyDimensions(final Stitchable patch) {
        final int maxHeightWithPatch = Math.max(patch.getY() + patch.getHeight(), currentHeight);
        final int maxWidthWithPatch = Math.max(patch.getX() + patch.getWidth(), currentWidth);

        if (maxHeightWithPatch > currentHeight || maxWidthWithPatch > currentWidth) {
            resize(maxHeightWithPatch, maxWidthWithPatch, true);
        }
    }

    /**
     * Resize the canvas to the provided dimensions.
     * @param fill Copies the first contents of the canvas to the new canvas.
     * @param height of new canvas
     * @param width of new canvas
     */
    private void resize(final int height, final int width, final boolean fill) {
        final int[][] newCanvas = new int[height][width];
        System.out.println("Resizing from" + currentHeight + " " + currentWidth +  " to " + height + " " + width);

        if (fill) {
            Utils.copyMatrix(this.canvas, newCanvas);
        }

        this.canvas = newCanvas;
        this.currentHeight = height;
        this.currentWidth = width;
    }

    /**
     * Returns the final stitched canvas.
     *
     * @return the canvas
     */
    public int[][] getCanvas() {
        return canvas;
    }
}