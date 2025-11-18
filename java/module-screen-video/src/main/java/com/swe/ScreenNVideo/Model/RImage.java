package com.swe.ScreenNVideo.Model;

import com.swe.ScreenNVideo.Utils;

import java.nio.ByteBuffer;

/**
 * Image to be sent via RPC.
 */
public class RImage {
    /**
     * Image to be sent to UI.
     */
    private final int[][] image;

    /**
     * ip of the user whose image is this.
     */
    private final String ip;

    public RImage(final int[][] imageArgs, final String ipArgs) {
        ip = ipArgs;
        image = imageArgs;
    }

    /**
     * Serializes the image.
     * @return serialized byte array
     */
    public byte[] serialize() {
        final int height = image.length;
        final int width = image[0].length;
        final byte[] ipBytes = ip.getBytes();
        final int lenReq = ipBytes.length + (height * width) * 3 + 12;
        final ByteBuffer buffer = ByteBuffer.allocate(lenReq); // three for rgb
        // put the ip
        buffer.putInt(ipBytes.length);
        buffer.put(ipBytes);
        // put the image
        buffer.putInt(height);
        buffer.putInt(width);
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                final int pixel = image[i][j];
                final byte r = (byte) ((pixel >> Utils.INT_MASK_16) & Utils.BYTE_MASK);
                final byte g = (byte) ((pixel >> Utils.INT_MASK_8) & Utils.BYTE_MASK);
                final byte b = (byte) (pixel & Utils.BYTE_MASK);

                buffer.put(r);
                buffer.put(g);
                buffer.put(b);
            }
        }
        return buffer.array();
    }

    public int[][] getImage() {
        return image;
    }

    public String getIp() {
        return ip;
    }
}
