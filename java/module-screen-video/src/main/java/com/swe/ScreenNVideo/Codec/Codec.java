/**
 * Contributed by Chirag Varshney.
 */

package com.swe.ScreenNVideo.Codec;

/**
 * Interface for encoding and decoding an image.
 *
 */
public interface Codec {
    /**
     * Set the screenshot for encoding or decoding. 
     *
     * @param screenshot matrix
     */
    void setScreenshot(int[][] screenshot); 

    /**
     * Encode and Compress the image.
     *
     * @param x topLeft postition along x axis of image matrix
     * @param y topLeft position along y axis of image matrix
     * @param height block's height
     * @param width block's width
     * @return an array bytes
     */
    byte[] encode(int x, int y, int height, int width);

    /**
     * Decode and Decompress the image.
     *
     * @param encodedImage image to be decoded 
     * @return decoded image matrix
     */
    int[][] decode(byte[] encodedImage);
}
