package com.swe.ScreenNVideo.Codec;

import java.util.List;

/**
 * Interface for encoding and decoding an image.
 *
 */
public interface Codec {

    /**
     * Encode and Compress the image.
     *
     * @param screenShot image matrix to be encoded
     * @param x topLeft postition along x axis of image matrix
     * @param y topLeft position along y axis of image matrix
     * @param height block's height
     * @param width block's width
     * @return list of array bytes
     * at ind 0 : compressed
     * at ind 1 : without compression
     */
    List<byte[]> encode(final int[][] screenShot, final int x, final int y, final int height, final int width);

    /**
     * Decode and Decompress the image.
     *
     * @param encodedImage image to be decoded
     * @param compress to compress or not
     * @return decoded image matrix
     */
    int[][] decode(final byte[] encodedImage, boolean compress);

}
