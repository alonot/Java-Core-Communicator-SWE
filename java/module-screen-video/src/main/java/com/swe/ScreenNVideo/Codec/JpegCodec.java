package com.swe.ScreenNVideo.Codec;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

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
 */
public class JpegCodec implements Codec {

    /**
     * Offset for alpha component in the AARRGGBB value.
     */
    private static final int A_OFFSET = 24;

    /**
     * Offset for red color component in the AARRGGBB value.
     */
    private static final int R_OFFSET = 16;

    /**
     * Offset for green color component in the AARRGGBB value.
     */
    private static final int G_OFFSET = 8;

    /**
     * Mask for extracting color component.
     */
    private static final int MASK = 0xFF;

    /**
     * Coefficient for red contribution in luminance(Y) calculation.
     */
    private static final double Y_R_COEFF = 0.299;

    /**
     * Coefficient for green contribution in luminance(Y) calculation.
     */
    private static final double Y_G_COEFF = 0.587;

    /**
     * Coefficient for blue contribution in luminance(Y) calculation.
     */
    private static final double Y_B_COEFF = 0.114;

    /**
     * Coefficient for red contribution in chroma blue-difference (Cb) calculation.
     */
    private static final double CB_R_COEFF = -0.168736;

    /**
     * Coefficient for green contribution in chroma blue-difference (Cb) calculation.
     */
    private static final double CB_G_COEFF = -0.331264;

    /**
     * Coefficient for blue contribution in chroma blue-difference (Cb) calculation.
     */
    private static final double CB_B_COEFF = 0.5;

    /**
     * Coefficient for red contribution in chroma red-difference (Cr) calculation.
     */
    private static final double CR_R_COEFF = 0.5;

    /**
     * Coefficient for green contribution in chroma red-difference (Cr) calculation.
     */
    private static final double CR_G_COEFF = -0.418688;

    /**
     * Coefficient for blue contribution in chroma red-difference (Cr) calculation.
     */
    private static final double CR_B_COEFF = -0.081312;

    /**
     * Offset applied to Cb and Cr channels to center them around 128.
     * Also used as a level-shift value for Y.
     */
    private static final int CHROMA_OFFSET = 128;

    /**
     * Coefficient for Cr contribution to red channel in YCbCr->RGB conversion.
     */
    private static final double CR_TO_R = 1.402;

    /**
     * Coefficient for Cb contribution to green channel in YCbCr->RGB conversion.
     */
    private static final double CB_TO_G = 0.344136;

    /**
     * Coefficient for Cr contribution to green channel in YCbCr->RGB conversion.
     */
    private static final double CR_TO_G = 0.714136;

    /**
     * Coefficient for Cb contribution to blue channel in YCbCr->RGB conversion.
     */
    private static final double CB_TO_B = 1.772;

    /**
     * Maximum value for a color channel (8-bit).
     */
    private static final int COLOR_MAX = 255;

    /**
     * Number of pixels in a 2*2 block for 4:2:0 chroma subsampling.
     * Note: Not used in 4:4:4 mode, kept for compatibility.
     */
    private static final int SUBSAMPLE_BLOCK_SIZE = 4;

    /**
     * Side dimension of the 2x2 subsampling block.
     * Note: Not used in 4:4:4 mode, kept for compatibility.
     */
    private static final int BLOCK_SIDE = 2;

    /**
     * Side dimension of the 8x8 DCT block.
     */
    private static final int DCT_BLOCK_SIZE = 8;

    /**
     * Double-precision side dimension of the 8x8 DCT block for ceiling calculations.
     */
    private static final double DCT_BLOCK_SIZE_DOUBLE = 8.0;

    // --- Constants for maxLen calculation ---
    /**
     * Factor for total pixels (Y + Cb + Cr) based on 4:4:4 subsampling (1 + 1 + 1 = 3.0).
     */
    private static final double TOTAL_PIXEL_FACTOR = 3.0;
    /**
     * Bytes per RLE pair (short value, short count).
     */
    private static final int BYTES_PER_RLE_PAIR = 4;
    /**
     * Bytes to store dimensions (short height, short width) per matrix.
     */
    private static final int MATRIX_DIM_BYTES = 4;
    /**
     * Number of matrices (Y, Cb, Cr).
     */
    private static final int NUM_MATRICES = 3;
    /**
     * Offset for integer rounding.
     */
    private static final double INT_ROUNDING_OFFSET = 0.5;


    /**
     * Time taken for ZigZag operations.
     */
    public long zigZagtime = 0;
    /**
     * Time taken for DCT operations.
     */
    public long dctTime = 0;
    /**
     * Time taken for quantization operations.
     */
    public long quantTime = 0;

    private final Compressor compressor = new Compressor();
    private final IDeCompressor decompressor = new DeCompressor();
    private final IRLE enDeRLE = EncodeDecodeRLEHuffman.getInstance();

    /**
     * Buffer for RLE results with compression On.
     */
    private final ByteBuffer resCompressedRLEBuffer;

    /**
     * Buffer for RLE results with compression OFF.
     */
    private final ByteBuffer resUnCompressedRLEBuffer;

    /**
     * Creates a JpegCode instance.
     */
    public JpegCodec() {
        int maxLen = (int) ((1200 * 800 * 1.5 * 4) + (4 * 3) + 0.5);
        resCompressedRLEBuffer = ByteBuffer.allocate(maxLen);
        resUnCompressedRLEBuffer = ByteBuffer.allocate(maxLen);
    }

    @Override
    public List<byte[]> encode(final int[][] screenshot, final int topLeftX, final int topLeftY, final int height, final int width) {

        if (height % BLOCK_SIDE == 1 || width % BLOCK_SIDE == 1) {
            throw new RuntimeException("Invalid Matrix for encoding");
        }

        final int halfHeight = height / BLOCK_SIDE;
        final int halfWidth = width / BLOCK_SIDE;
        final int cbHeight = (int) Math.ceil(halfHeight / DCT_BLOCK_SIZE_DOUBLE) * DCT_BLOCK_SIZE;
        final int cbWidth = (int) Math.ceil(halfWidth / DCT_BLOCK_SIZE_DOUBLE) * DCT_BLOCK_SIZE;
        final short[][] yMatrix = new short[height][width];
        final short[][] cbMatrix = new short[cbHeight][cbWidth];
        final short[][] crMatrix = new short[cbHeight][cbWidth];

        final int bottomLeftX = topLeftX + width;
        final int bottomLeftY = topLeftY + height;
        for (int i = topLeftY; i < bottomLeftY; i += BLOCK_SIDE) {
            for (int j = topLeftX; j < bottomLeftX; j += BLOCK_SIDE) {
                double cbPixel = 0;
                double crPixel = 0;
                for (int ii = i; ii < i + BLOCK_SIDE; ++ii) {
                    for (int jj = j; jj < j + BLOCK_SIDE; ++jj) {
                        final int pixel = screenshot[ii][jj];

                        final int r = (pixel >> R_OFFSET) & MASK;
                        final int g = (pixel >> G_OFFSET) & MASK;
                        final int b = pixel & MASK;

                        // conversion to YCbCr
                        int y = (int) (Y_R_COEFF * r + Y_G_COEFF * g + Y_B_COEFF * b);
                        final double cb = CHROMA_OFFSET + CB_R_COEFF * r + CB_G_COEFF * g + CB_B_COEFF * b;
                        final double cr = CHROMA_OFFSET + CR_R_COEFF * r + CR_G_COEFF * g + CR_B_COEFF * b;

                        cbPixel += cb;
                        crPixel += cr;

                        // Clamp to 0-255
                        y = Math.min(COLOR_MAX, Math.max(0, y));
                        yMatrix[ii - topLeftY][jj - topLeftX] = (short) (y - CHROMA_OFFSET);
                    }
                }

                final int posY = i - topLeftY;
                final int posX = j - topLeftX;
                final int cbValue = (int) (cbPixel / SUBSAMPLE_BLOCK_SIZE);
                final int crValue = (int) (crPixel / SUBSAMPLE_BLOCK_SIZE);

                cbMatrix[posY / BLOCK_SIDE][posX / BLOCK_SIDE] =
                        (short) (Math.min(COLOR_MAX, Math.max(0, cbValue)) - CHROMA_OFFSET);
                crMatrix[posY / BLOCK_SIDE][posX / BLOCK_SIDE] =
                        (short) (Math.min(COLOR_MAX, Math.max(0, crValue)) - CHROMA_OFFSET);

            }
        }

        resCompressedRLEBuffer.clear();
        resUnCompressedRLEBuffer.clear();

        // add the uncompressed versions first
        enDeRLE.zigZagRLE(yMatrix, resUnCompressedRLEBuffer);
        enDeRLE.zigZagRLE(cbMatrix, resUnCompressedRLEBuffer);
        enDeRLE.zigZagRLE(crMatrix, resUnCompressedRLEBuffer);


        // YMatrix;
        compressor.compressLumin(yMatrix, (short) height, (short) width, resCompressedRLEBuffer);

        // CbMatrix;
        compressor.compressChrome(cbMatrix, (short) cbHeight, (short) cbWidth, resCompressedRLEBuffer);

        // CyMatrix
        compressor.compressChrome(crMatrix, (short) cbHeight, (short) cbWidth, resCompressedRLEBuffer);

        // add the compressed patches
        enDeRLE.zigZagRLE(yMatrix, resCompressedRLEBuffer);
        enDeRLE.zigZagRLE(cbMatrix, resCompressedRLEBuffer);
        enDeRLE.zigZagRLE(crMatrix, resCompressedRLEBuffer);

        final byte[] compressedData = new byte[resCompressedRLEBuffer.position()];
        resCompressedRLEBuffer.rewind();
        resCompressedRLEBuffer.get(compressedData);

        final byte[] unCompressedData = new byte[resUnCompressedRLEBuffer.position()];
        resUnCompressedRLEBuffer.rewind();
        resUnCompressedRLEBuffer.get(unCompressedData);

        final ArrayList<byte[]> res = new ArrayList<>();
        res.add(compressedData);
        res.add(unCompressedData);
        return res;
    }

    /**
     * Decodes a encoded string back into the screenshot matrix.
     *
     * <p>
     * The decoding process includes:
     * <ul>
     * <li>Reconstructing the coefficient matrix from the zigzag scanned string.</li>
     * <li>Reversing 4:2:0 chroma subsampling to approximate the original chroma values.</li>
     * <li>Converting the data from YCbCr color space back to RGB pixel values.</li>
     * </ul>
     * </p>
     */
    @Override
    public int[][] decode(final byte[] encodedImage, final boolean compress) {
        final ByteBuffer buffer = ByteBuffer.wrap(encodedImage);
        final short[][] yMatrix = enDeRLE.revZigZagRLE(buffer);
        final short[][] cbMatrix = enDeRLE.revZigZagRLE(buffer);
        final short[][] crMatrix = enDeRLE.revZigZagRLE(buffer);

        if (compress) {
            decompressor.decompressLumin(yMatrix, (short) yMatrix.length, (short) yMatrix[0].length);
            decompressor.decompressChrome(cbMatrix, (short) cbMatrix.length, (short) cbMatrix[0].length);
            decompressor.decompressChrome(crMatrix, (short) crMatrix.length, (short) crMatrix[0].length);
        }

        return convertYCbCrToRGB(yMatrix, cbMatrix, crMatrix);
    }

    /**
     * Clamps an integer value to the 0-255 color range.
     *
     * @param value the value to clamp
     * @return the clamped value
     */
    private int rescale(final int value) {
        return Math.min(COLOR_MAX, Math.max(0, value));
    }

    /**
     * Converts YCbCr 4:2:0 data back to an ARGB 8:8:8:8 pixel matrix.
     *
     * @param yMatrix  the luminance matrix
     * @param cbMatrix the chroma-blue matrix (subsampled)
     * @param crMatrix the chroma-red matrix (subsampled)
     * @return a 2D integer array of ARGB pixels
     */
    private int[][] convertYCbCrToRGB(final short[][] yMatrix, final short[][] cbMatrix, final short[][] crMatrix) {
        final int height = yMatrix.length;
        final int width = yMatrix[0].length;
        final int[][] rgb = new int[height][width];

        // Chroma matrices may be padded to multiples of 8, but we only need height/2 x width/2
        final int chromaHeight = Math.min(cbMatrix.length, height / 2);
        final int chromaWidth = Math.min(cbMatrix[0].length, width / 2);

        // Process chroma subsampled data - only use the first height/2 x width/2 elements
        // even if chroma matrices are padded to multiples of 8
        for (int i = 0; i < chromaHeight; ++i) {
            for (int j = 0; j < chromaWidth; ++j) {
                // Level shift: [-128, 127] → [0, 255]
                final int cbOffset = rescale(cbMatrix[i][j] + CHROMA_OFFSET);
                final int crOffset = rescale(crMatrix[i][j] + CHROMA_OFFSET);

                // Center chroma for YCbCr → RGB conversion
                final int cbCentered = cbOffset - CHROMA_OFFSET;
                final int crCentered = crOffset - CHROMA_OFFSET;

                for (int ii = 0; ii < BLOCK_SIDE; ++ii) {
                    for (int jj = 0; jj < BLOCK_SIDE; ++jj) {
                        final int yRow = BLOCK_SIDE * i + ii;
                        final int yCol = BLOCK_SIDE * j + jj;
                        final int y = rescale(yMatrix[yRow][yCol] + CHROMA_OFFSET);

                        // YCbCr → RGB with centered chroma
                        int r = (int) Math.round(y + CR_TO_R * crCentered);
                        int g = (int) Math.round(y - CB_TO_G * cbCentered - CR_TO_G * crCentered);
                        int b = (int) Math.round(y + CB_TO_B * cbCentered);

                        // Clamp final RGB
                        r = Math.min(COLOR_MAX, Math.max(0, r));
                        g = Math.min(COLOR_MAX, Math.max(0, g));
                        b = Math.min(COLOR_MAX, Math.max(0, b));

                        rgb[yRow][yCol] = (MASK << A_OFFSET) | (r << R_OFFSET) | (g << G_OFFSET) | b;
                    }
                }
            }
        }

        return rgb;
    }


}