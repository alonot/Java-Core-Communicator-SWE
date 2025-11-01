/**
 * Contributed by Chirag Varshney.
 */

package com.swe.ScreenNVideo.Codec;

/**
 * Interface for scaling images up or down.
 */
public interface ImageScaler {

    /**
     * Scales the given image by the specified height and width factors.
     *
     * <p>
     * Scaling should preserve the visual quality of the original as much 
     * as possible.
     * </p>
     *
     * @param matrix image that needs to be scaled 
     * @param targetHeight the target height of the scaled image 
     * @param targetWidth the target width of the scaled image
     * @return a scaled image
     */
    int[][] scale(int[][] matrix, int targetHeight, int targetWidth);
}
