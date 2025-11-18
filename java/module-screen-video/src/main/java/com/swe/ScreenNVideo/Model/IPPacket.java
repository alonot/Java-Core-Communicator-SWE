package com.swe.ScreenNVideo.Model;


import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Subscribe Packet.
 */
public record IPPacket(String ip, boolean reqCompression) {


    /**
     * Serializes the string for networking layer.
     * @return serialized byte array
     */
    public byte[] serialize(final NetworkPacketType networkPacketType) {
        final int len = 4 * Integer.BYTES + 1; // 4 int for ip and one for boolean
        final ByteBuffer buffer = ByteBuffer.allocate(len + 1);
        buffer.put((byte) (networkPacketType.ordinal()));

        final int[] ipInts = Arrays.stream(ip.split("\\.")).mapToInt(Integer::parseInt).toArray();
        for (int i = 0; i < ipInts.length; i++) {
            buffer.putInt(ipInts[i]);
        }
        buffer.put((byte) (reqCompression ? 1 : 0));

        return buffer.array();
    }

    /**
     * Deserializes the string from the networking layer.
     *
     * @param data the byte array to be deserialized
     * @return the string
     */
    public static IPPacket deserialize(final byte[] data) {
        final ByteBuffer buffer = ByteBuffer.wrap(data);
        final byte type = buffer.get();
        final int[] ipInts = new int[4];
        for (int i = 0; i < 4; i++) {
            ipInts[i] = buffer.getInt();
        }
        String ip = Arrays.stream(ipInts).mapToObj(String::valueOf).collect(Collectors.joining("."));
        final boolean reqCompression = buffer.get() == 1;
        return new IPPacket(ip, reqCompression);
    }

}
