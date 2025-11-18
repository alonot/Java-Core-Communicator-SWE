package com.swe.ScreenNVideo;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.ByteArrayOutputStream;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * Utility class for ScreenN Video.
 */
public class Utils {
    /**
     * Hashing stride for the hashing algorithm.
     */
    public static final int HASH_STRIDE = 2;
    /**
     * Key constant for start_video_capture.
     */
    public static final String START_VIDEO_CAPTURE = "startVideoCapture";
    /**
     * Key constant for stop_video_capture.
     */
    public static final String STOP_VIDEO_CAPTURE = "stopVideoCapture";
    /**
     * Key constant for start_screen_capture.
     */
    public static final String START_SCREEN_CAPTURE = "startScreenCapture";
    /**
     * Key constant for stop_screen_capture.
     */
    public static final String STOP_SCREEN_CAPTURE = "stopScreenCapture";
    /**
     * Key constant for start_audio_capture.
     */
    public static final String START_AUDIO_CAPTURE = "startAudioCapture";
    /**
     * Key constant for stop_audio_capture.
     */
    public static final String STOP_AUDIO_CAPTURE = "stopAudioCapture";
    /**
     * Key constant for subscribe_as_viewer.
     */
    public static final String SUBSCRIBE_AS_VIEWER = "subscribeAsViewer";
    /**
     * Key constant for unSubscribe_as_viewer.
     */
    public static final String UNSUBSCRIBE_AS_VIEWER = "unSubscribeAsViewer";
    /**
     * Key constant for Updating UI.
     */
    public static final String UPDATE_UI = "updateUI";
    /**
     * Key constant for StopShare.
     */
    public static final String STOP_SHARE = "stopShare";
    /**
     * Key constant for unsubscribe_as_viewer.
     */
    public static final String MODULE_REMOTE_KEY = "screenNVideo";
    /**
     * Key constant for unsubscribe_as_viewer.
     */
    public static final int BUFFER_SIZE = 1024 * 10; // 10 kb

    public static final float DEFAULT_SAMPLE_RATE = 48000f;
    public static final int DEFAULT_CHANNELS = 1;
    public static final int DEFAULT_SAMPLE_SIZE = 16;

    /**
     * Scale factor for X axis.
     */
    public static final int SCALE_X = 7;
    /**
     * Scale factor for Y axis.
     */
    public static final int SCALE_Y = 5;
    /**
     * PaddingX for the videoCapture to stitch to the ScreenCapture.
     */
    public static final int VIDEO_PADDING_X = 20;
    /**
     * PaddingY for the videoCapture to stitch to the ScreenCapture.
     */
    public static final int VIDEO_PADDING_Y = 20;

    /**
     * Width of the server.
     */
    public static final int SERVER_WIDTH = 800;
    /**
     * Height of the server.
     */
    public static final int SERVER_HEIGHT = 600;
    /**
     * Width of the client.
     */
    public static final int BYTE_MASK = 0xff;
    /**
     * INT mask to get the first 8 bits.
     */
    public static final int INT_MASK_24 = 24;
    /**
     * INT mask to get the second 8 bits.
     */
    public static final int INT_MASK_16 = 16;
    /**
     * INT mask to get the third 8 bits.
     */
    public static final int INT_MASK_8 = 8;

    /**
     * Seconds in milliseconds.
     */
    public static final int SEC_IN_MS = 1000;

    /**
     * Milli-seconds in nanoseconds.
     */
    public static final int MSEC_IN_NS = 1_000_000;
    /**
     * Seconds in nanoseconds.
     */
    public static final int SEC_IN_NS = 1_000_000_000;
    /**
     * bytes in 1 kb.
     */
    public static final double KB = 1_024.0;

    /**
     * Max diffs stored in a heap.
     */
    public static final int MAX_HEAP_SIZE = 20;

    /**
     * Maximum tries to serialize the compressed packets.
     */
    public static final int MAX_TRIES_TO_SERIALIZE = 3;
    /**
     * Server Max FPS.
     */
    public static final int FPS = 40;

    /**
     * Writes the given int to the buffer in little endian.
     * @param bufferOut the buffer to write to
     * @param data the data to write
     */
    public  static void writeInt(final ByteArrayOutputStream bufferOut, final int data) {
        bufferOut.write((data >> INT_MASK_24) & Utils.BYTE_MASK);
        bufferOut.write((data >> INT_MASK_16) & Utils.BYTE_MASK);
        bufferOut.write((data >> INT_MASK_8) & Utils.BYTE_MASK);
        bufferOut.write(data & Utils.BYTE_MASK);
    }

    /**
     * Converts the given image to its rgb form.
     * @param img the image
     * @return int[][] : RGB matrix 0xAARRGGBB / 0x00RRGGBB
     */
    public static int[][] convertToRGBMatrix(final BufferedImage img) {
        final int width = img.getWidth();
        final int height = img.getHeight();

//        long startTime = System.nanoTime();
        // Direct buffer access (zero per-pixel overhead)
        final int[] data = ((DataBufferInt) img.getRaster().getDataBuffer()).getData();

        final int[][] matrix = new int[height][width];
        for (int y = 0; y < height; y++) {
            System.arraycopy(data, y * width, matrix[y], 0, width);
        }
//        System.out.println("Image to RGB Matrix Conversion Time: "
//                + (System.nanoTime() - startTime) / ((double) MSEC_IN_NS) + " ms");
        return matrix;
    }


    /**
     * Gives the IP address of self machine.
     * @return IP address of self machine
     */
    public static String getSelfIP() {
        // Get IP address as string
        try (DatagramSocket socket = new DatagramSocket()) {
            final int pingPort = 10002;
            socket.connect(InetAddress.getByName("8.8.8.8"), pingPort);
            return socket.getLocalAddress().getHostAddress();
        } catch (SocketException | UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Fills dstMatrix with the srcMatrix
     * @param srcMatrix matrix
     * @param dstMatrix matrix
     */
    public static void copyMatrix(int[][] srcMatrix, int[][] dstMatrix) {

        final int height = Math.min(srcMatrix.length, dstMatrix.length);
        final int srcWidth = (srcMatrix.length) > 0 ? srcMatrix[0].length : 0;
        final int dstWidth = (dstMatrix.length) > 0 ? dstMatrix[0].length : 0;
        final int width = Math.min(srcWidth, dstWidth);

        for (int i = 0; i < height; i++) {
            System.arraycopy(srcMatrix[i], 0, dstMatrix[i], 0, width);
        }
    }

    /**
     * Fills dstMatrix with the srcMatrix
     * @param srcMatrix matrix
     * @param dstMatrix matrix
     */
    public static void copyMatrix(long[][] srcMatrix, long[][] dstMatrix) {

        final int height = Math.min(srcMatrix.length, dstMatrix.length);
        final int srcWidth = (srcMatrix.length) > 0 ? srcMatrix[0].length : 0;
        final int dstWidth = (dstMatrix.length) > 0 ? dstMatrix[0].length : 0;
        final int width = Math.min(srcWidth, dstWidth);

        for (int i = 0; i < height; i++) {
            System.arraycopy(srcMatrix[i], 0, dstMatrix[i], 0, width);
        }
    }

}