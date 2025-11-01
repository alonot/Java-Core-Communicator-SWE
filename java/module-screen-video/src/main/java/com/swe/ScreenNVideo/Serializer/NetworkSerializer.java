/**
 * Contributed by Priyanshu Pandey.
 */

package com.swe.ScreenNVideo.Serializer;


import java.nio.ByteBuffer;
import java.security.InvalidParameterException;

/**
 * Serializer class for communicating with networking layer.
 */
public class NetworkSerializer {

    /**
     * Serializes the string for networking layer.
     * @param data the string to be sent
     * @return serialized byte array
     */
    public static byte[] serializeIP(final  String data) {
        final int len = data.length();
        final ByteBuffer buffer = ByteBuffer.allocate(len + 1);
        buffer.put((byte) (NetworkPacketType.SUBSCRIBE_AS_VIEWER.ordinal()));
        buffer.put(data.getBytes());
        return buffer.array();
    }

    /**
     * Deserializes the string from the networking layer.
     * @param data the byte array to be deserialized
     * @return the string
     */
    public static String deserializeIP(final byte[] data) {
        final byte packetType = data[0];
        if (packetType != NetworkPacketType.SUBSCRIBE_AS_VIEWER.ordinal()) {
            throw new InvalidParameterException(
                "Invalid Data type: Expected "
                    + NetworkPacketType.SUBSCRIBE_AS_VIEWER.ordinal() + " got : " + packetType);
        }
        return new String(data, 1, data.length - 1);
    }

}
