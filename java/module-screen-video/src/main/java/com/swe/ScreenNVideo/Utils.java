/**
 * Contributed by Priyanshu Pandey.
 */

package com.swe.ScreenNVideo;

import java.awt.image.BufferedImage;
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
     * Key constant for subscribe_as_viewer.
     */
    public static final String SUBSCRIBE_AS_VIEWER = "subscribeAsViewer";
    /**
     * Key constant for unsubscribe_as_viewer.
     */
    public static final String UPDATE_UI = "updateUI";
    /**
     * Key constant for unsubscribe_as_viewer.
     */
    public static final String MODULE_REMOTE_KEY = "screenNVideo";
    /**
     * Key constant for unsubscribe_as_viewer.
     */
    public static final int BUFFER_SIZE = 1024 * 10; // 10 kb
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
    public static final int SERVER_WIDTH = 1200;
    /**
     * Height of the server.
     */
    public static final int SERVER_HEIGHT = 800;
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
     * @param feed the image
     * @return int[][] : RGB matrix 0xAARRGGBB / 0x00RRGGBB
     */
    public static int[][] convertToRGBMatrix(final BufferedImage feed) {
        final int[][] matrix = new int[feed.getHeight()][feed.getWidth()];
        for (int i = 0; i < feed.getHeight(); i++) {
            for (int j = 0; j < feed.getWidth(); j++) {
                matrix[i][j] = feed.getRGB(j, i);
            }
        }
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

}
