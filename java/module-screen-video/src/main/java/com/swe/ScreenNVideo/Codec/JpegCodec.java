/**
 * Contributed by Anup.
 */

package com.swe.ScreenNVideo.Codec;

import java.nio.ByteBuffer;

/**
 * Provides functionality for encoding and decoding images in the JPEG format.
 *
 * <p>
 * This class implements the {@link Codec} interface, offering methods to convert
 * raw image data into compressed JPEG byte streams and to reconstruct images
 * from JPEG-encoded data.
 * </p>
 *
 * @see Codec
 *
 */
public class JpegCodec implements Codec {
    /**
     * Image that needs to be encoded.
     */
    private int[][] screenshot;

    /** Offset for alpha component in the AARRGGBB value.*/
    private static final int A_OFFEST = 24;

    /** Offset for red color component in the AARRGGBB value.*/
    private static final int R_OFFSET = 16;

    /** Offset for green color component in the AARRGGBB value.*/
    private static final int G_OFFSET = 8;

    /** Mask for extracting color component. */
    private static final int MASK = 0xFF;

    /** Coefficient for red contribution in luminance(Y) calculation.*/
    private static final double Y_R_COEFF  = 0.299;

    /** Coefficient for green contribution in luminance(Y) calculation.*/
    private static final double Y_G_COEFF  = 0.587;

    /** Coefficient for blue contribution in luminance(Y) calculation.*/
    private static final double Y_B_COEFF  = 0.114;

    /** Coefficient for red contribution in chroma blue-difference (Cb) calculation. */
    private static final double CB_R_COEFF = -0.168736;

    /** Coefficient for green contribution in chroma blue-difference (Cb) calculation.*/
    private static final double CB_G_COEFF = -0.331264;

    /** Coefficient for blue contribution in chroma blue-difference (Cb) calculation. */
    private static final double CB_B_COEFF = 0.5;

    /** Coefficient for red contribution in chroma red-difference (Cr) calculation. */
    private static final double CR_R_COEFF = 0.5;

    /** Coefficient for green contribution in chroma red-difference (Cr) calculation. */
    private static final double CR_G_COEFF = -0.418688;

    /** Coefficient for blue contribution in chroma red-difference (Cr) calculation.*/
    private static final double CR_B_COEFF = -0.081312;

    /** Offset applied to Cb and Cr channels to center them around 128.*/
    private static final int CHROMA_OFFSET = 128;

    /** Coefficient for Cr contribution to red channel in YCbCr->RGB conversion.*/
    private static final double CR_TO_R = 1.402;

    /** Coefficient for Cb contribution to green channel in YCbCr->RGB conversion.*/
    private static final double CB_TO_G = 0.344136;

    /** Coefficient for Cr contribution to green channel in YCbCr->RGB conversion.*/
    private static final double CR_TO_G = 0.714136;

    /** Coefficient for Cb contribution to blue channel in YCbCr->RGB conversion.*/
    private static final double CB_TO_B = 1.772;

    /** Maximum value for a color channel (8-bit).*/
    private static final int COLOR_MAX = 255;

    /** Number of pixels in a 2*2 block for 4:2:0 chroma subsampling.*/
    private static final int SUBSAMPLE_BLOCK_SIZE = 4;

    /**
     * Creates a JpegCodec instance with screenshot.
     *
     * @param image screenshot that needs to be encoded
     */
    public JpegCodec(final int[][] image) {
        this.screenshot = image;
    }

    /**
     * Creates a JpegCode instance.
     *
     */
    public JpegCodec() {
    }

    /**
     * Sets the screenshot image for this object.
     *
     * @param image a 2D integer array representing the image,
     *              where each entry encodes a pixel color value.
     */
    public void setScreenshot(final int[][] image) {
        this.screenshot = image;
    }

    /**
     * Encodes the current screent into a compressed format.
     *
     * <p>
     * The encoding process includes:
     * <ul>
     *    <li>Converting the RGB pixel data of the screenshot into YCbCr color space.</li>
     *    <li>Applying 4:2:0 chroma sampling to reduce color resolution.</li>
     * </ul>
     * </p>
     */
    @Override
    public byte[] encode(final int topLeftX, final int topLeftY, final int height, final int width) {

        // System.out.println("topLeftX topLeftY W H = " + topLeftX + " " + topLeftY + ":" + width + " " + height);

        if (height % 2 == 1 || width % 2 == 1) {
            throw new RuntimeException("Invalid Matrix for encoding");
        }

        final int[][] yMatrix = new int[height][width];
        final int[][] cbMatrix = new int[height / 2][width / 2];
        final int[][] crMatrix = new int[height / 2][width / 2];

        final int bottomLeftX = topLeftX + width;
        final int bottomLeftY = topLeftY + height;
        for (int i = topLeftY; i < bottomLeftY; i += 2) {
            for (int j = topLeftX; j < bottomLeftX; j += 2) {
                double cbPixel = 0;
                double crPixel = 0;
                for (int ii = i; ii < i + 2; ++ii) {
                    for (int jj = j; jj < j + 2; ++jj) {
                        final int pixel = screenshot[ii][jj];

                        final int r = (pixel >> R_OFFSET) & MASK;
                        final int g = (pixel >> G_OFFSET) & MASK;
                        final int b = pixel & MASK;

                        // conversion to YCbCr
                        int y  = (int) (Y_R_COEFF * r + Y_G_COEFF * g + Y_B_COEFF * b);
                        final double cb = CHROMA_OFFSET + CB_R_COEFF * r + CB_G_COEFF * g + CB_B_COEFF * b;
                        final double cr = CHROMA_OFFSET + CR_R_COEFF * r + CR_G_COEFF * g + CR_B_COEFF * b;

                        cbPixel += cb;
                        crPixel += cr;

                        // Clamp to 0-255
                        y = Math.min(COLOR_MAX, Math.max(0, y));
                        yMatrix[ii - topLeftY][jj - topLeftX] = y;
                    }
                }

                final int posY = i - topLeftY;
                final int posX = j - topLeftX;
                cbMatrix[posY / 2][posX / 2] = Math.min(COLOR_MAX, Math.max(0, (int) (cbPixel / SUBSAMPLE_BLOCK_SIZE)));
                crMatrix[posY / 2][posX / 2] = Math.min(COLOR_MAX, Math.max(0, (int) (crPixel / SUBSAMPLE_BLOCK_SIZE)));

            }
        }

        final int dataLength = (height * width) * 2 * 4 + 8; // one matrix of (h x w) and 2 matrices of (h/2 * h/2)
        final ByteBuffer buffer = ByteBuffer.allocate(dataLength);
        buffer.putInt(height);
        buffer.putInt(width);
        zigZagScan(buffer, yMatrix);
        zigZagScan(buffer, cbMatrix);
        zigZagScan(buffer, crMatrix);
        buffer.flip();

        return buffer.array();
    }

    /**
     * Decodes a encoded string back into the screenshot matrix.
     *
     * <p>
     * The decoding process includes:
     * <ul>
     *    <li>Reconstructing the coefficient matrix from the zigzag scanned string.</li>
     *    <li>Reversing 4:2:0 chroma subsampling to approximate the original chroma values.</li>
     *    <li>Converting the data from YCbCr color space back to RGB pixel values.</li>
     * </ul>
     * </p>
     */
    @Override
    public int[][] decode(final byte[] encodedImage) {
        final ByteBuffer buffer = ByteBuffer.wrap(encodedImage);
        final int height = buffer.getInt();
        final int width = buffer.getInt();

        final int[][] y = reverseZigZagScan(height, width, buffer);
        // System.out.println("Done" + Cbstring);
        final int[][] cb = reverseZigZagScan(height / 2, width / 2, buffer);
        // System.out.println("Done");
        final int[][] cr = reverseZigZagScan(height / 2, width / 2, buffer);

        final int[][] rgbMatrix = convertYCbCrToRGB(y, cb, cr);

        return rgbMatrix;
    }


    private int[][] convertYCbCrToRGB(final int[][] yMatrix, final int[][] cbMatrix, final int[][] crMatrix) {
        final int height = yMatrix.length;
        final int width = yMatrix[0].length;

        final int[][] rgb = new int[height][width];

        for (int i = 0; i < height / 2; ++i) {
            for (int j = 0; j < width / 2; ++j) {
                // stored Cb/Cr are biased by +128; convert to signed offsets
                final int cbOffset = cbMatrix[i][j] - 128;
                final int crOffset = crMatrix[i][j] - 128;

                for (int ii = 0; ii < 2; ++ii) {
                    for (int jj = 0; jj < 2; ++jj) {
                        final int y = yMatrix[2 * i + ii][2 * j + jj];

                        // use offsets in the reconstruction formula
                        int r = (int) Math.round(y + CR_TO_R * crOffset);
                        int g = (int) Math.round(y - CB_TO_G * cbOffset - CR_TO_G * crOffset);
                        int b = (int) Math.round(y + CB_TO_B * cbOffset);

                        r = Math.min(COLOR_MAX, Math.max(0, r));
                        g = Math.min(COLOR_MAX, Math.max(0, g));
                        b = Math.min(COLOR_MAX, Math.max(0, b));

                        rgb[2 * i + ii][2 * j + jj] = (MASK << A_OFFEST) | (r << R_OFFSET) | (g << G_OFFSET) | b;
                    }
                }
            }
        }
        return rgb;
    }


    private void zigZagScan(final ByteBuffer buffer, final int[][] matrix) {
        final int m = matrix.length;
        final int n = matrix[0].length;

        /**
         * number of diagonals = M+N-1;
         * rule 1 :
         *   if diagonal index is even then move bottom -> top
         * rule 2:
         *   if diagonal index is odd then move top -> bottom
         */
        for (int diag = 0; diag < (m + n - 1); ++diag) {
            final int rowStart = Math.max(0, diag - (n - 1));
            final int rowEnd = Math.min(m - 1, diag);

            if ((diag & 1) == 1) {
                //odd diagonal index : top->bottom
                for (int i = rowStart; i <= rowEnd; ++i) {
                    final int j = diag - i;
                    buffer.putInt(matrix[i][j]);
                }
            } else {
                //even diagonal index : bottom->top
                for (int i = rowEnd; i >= rowStart; --i) {
                    final int j = diag - i;
                    buffer.putInt(matrix[i][j]);
                }
            }
        }
    }

    private int[][] reverseZigZagScan(final int height, final int width, final ByteBuffer zigZagData) {
        // System.out.println(zigZagString);
        final int[][] reqMatrix = new int[height][width];

        final int cellCounter = 0;

        for (int diag = 0; diag < (height + width - 1); ++diag) {
            final int rowStart = Math.max(0, diag - (width - 1));
            final int rowEnd = Math.min(height - 1, diag);

            if ((diag & 1) == 1) {
                //odd diagonal index : top->bottom
                for (int i = rowStart; i <= rowEnd; ++i) {
                    final int j = diag - i;
                    reqMatrix[i][j] = zigZagData.getInt();
                }
            } else {
                //even diagonal index : bottom->top
                for (int i = rowEnd; i >= rowStart; --i) {
                    final int j = diag - i;
                    reqMatrix[i][j] = zigZagData.getInt();
                }
            }
        }

        return reqMatrix;
    }

}
