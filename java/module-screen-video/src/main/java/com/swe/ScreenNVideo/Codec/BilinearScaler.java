/**
 * Contributed by Chirag Varshney.
 */

package com.swe.ScreenNVideo.Codec;

/**
 * Provides an implementation of the {@link ImageScaler} interface 
 * using Bilinear Interpolation.
 *
 * <p>
 * Bilinear Interpolation computes the color of each pixel in the scaled 
 * image as a weighted average of the four nearest pixels in the original
 * image. This produces smoother results than nearest-neighbour scaling,
 * especially when upscalling.
 * </p>
 *
 * @see ImageScaler
 */
public class BilinearScaler implements ImageScaler {
    
    /** Offset for alpha component in the AARRGGBB value.*/
    private static final int A_OFFSET = 24;

    /** Offset for red color component in the AARRGGBB value.*/
    private static final int R_OFFSET = 16;
              
    /** Offset for green color component in the AARRGGBB value.*/
    private static final int G_OFFSET = 8;

    /** Maximum value for a color channel (8-bit).*/
    private static final int COLOR_MAX = 255;
                  
    /** Mask for extracting color component. */
    private static final int MASK = 0xFF;

    /** Scales the given image using bilinear interpolation to the specified
     * width and height.
     *
     * @param matrix the image to be scaled 
     * @param targetHeight the target height of the scaled image (must be positive)
     * @param targetWidth the target width of the scaled image (must be positive)
     * @return the scaled image
     * */
    public int[][] scale(final int[][] matrix, final int targetHeight, final int targetWidth) {

        final int inputHeight = matrix.length;
        final int inputWidth = matrix[0].length;

        final double scaleY = (double) inputHeight / targetHeight;
        final double scaleX = (double) inputWidth / targetWidth;

        final int[][] reqMatrix = new int[targetHeight][targetWidth];

        for (int yOut = 0; yOut < targetHeight; yOut++) {
            for (int xOut = 0; xOut < targetWidth; xOut++) {
       
                // Map it to input coordinates
                final double xIn = (xOut + 0.5) * scaleX - 0.5;
                final double yIn = (yOut + 0.5) * scaleY - 0.5;

                // Locate 4 surrounding neighbours
                final int x0 = Math.max((int) Math.floor(xIn), 0);
                final int x1 = Math.min(x0 + 1, inputWidth - 1);
                final int y0 = Math.max(0, (int) Math.floor(yIn));
                final int y1 = Math.min(y0 + 1, inputHeight - 1);

                final double dx = xIn - x0;
                final double dy = yIn - y0;

                // Extract channels
                final int r00 = (matrix[y0][x0] >> R_OFFSET) & MASK;
                final int g00 = (matrix[y0][x0] >> G_OFFSET) & MASK;
                final int b00 = matrix[y0][x0] & MASK;

                final int r01 = (matrix[y0][x1] >> R_OFFSET) & MASK;
                final int g01 = (matrix[y0][x1] >> G_OFFSET) & MASK;
                final int b01 = matrix[y0][x1] & MASK;

                final int r10 = (matrix[y1][x0] >> R_OFFSET) & MASK;
                final int g10 = (matrix[y1][x0] >> G_OFFSET) & MASK;
                final int b10 = matrix[y1][x0] & MASK;

                final int r11 = (matrix[y1][x1] >> R_OFFSET) & MASK;
                final int g11 = (matrix[y1][x1] >> G_OFFSET) & MASK;
                final int b11 = matrix[y1][x1] & MASK;
  
                // Interpolate each channel separately
                int r = (int) Math.round(
                      (1 - dx) * (1 - dy) * r00
                      + dx * (1 - dy) * r10
                      + (1 - dx) * dy * r01 
                      + dx * dy * r11
                    );

                int g = (int) Math.round(
                      (1 - dx) * (1 - dy) * g00
                      + dx * (1 - dy) * g10
                      + (1 - dx) * dy * g01 
                      + dx * dy * g11
                    );

                int b = (int) Math.round(
                      (1 - dx) * (1 - dy) * b00
                      + dx * (1 - dy) * b10
                      + (1 - dx) * dy * b01 
                      + dx * dy * b11
                    );

                // Clamp
                r = Math.min(COLOR_MAX, Math.max(0, r));
                g = Math.min(COLOR_MAX, Math.max(0, g));
                b = Math.min(COLOR_MAX, Math.max(0, b));

                // Combine channels
                final int rgb = (r << R_OFFSET) | (g << G_OFFSET) | b;
                reqMatrix[yOut][xOut] = rgb;
            }
        }
        return reqMatrix;
    }
}
