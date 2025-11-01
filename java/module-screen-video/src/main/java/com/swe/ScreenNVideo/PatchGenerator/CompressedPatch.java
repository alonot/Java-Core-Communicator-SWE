/**
 * Contributed by Priyanshu Pandey.
 */

package com.swe.ScreenNVideo.PatchGenerator;

import java.nio.ByteBuffer;

/**
 * Represents a compressed patch of an image or frame.
 * Contains coordinates (x, y), dimensions, and the compressed tile data.
 * @param x X coordinate of the patch.
 * @param y Y coordinate of the patch.
 * @param width Width of the patch.
 * @param height Height of the patch.
 * @param data Compressed tile data as a string.
 */
public record CompressedPatch(int x, int y, int width, int height, byte[] data)  {

    /**
     * Serializes this packet to be sent via network.
     *
     * @return byte[] to be sent
     */
    public byte[] serializeCPacket() {
        final int lenRequired = data.length + 16; // 4 * 4 variables + data
        final ByteBuffer buffer = ByteBuffer.allocate(lenRequired + 4);
        buffer.putInt(lenRequired);
        buffer.putInt(x);
        buffer.putInt(y);
        buffer.putInt(width);
        buffer.putInt(height);

        buffer.put(data);

        return buffer.array();
    }

    /**
     * Creates a Compresses patch from incoming packet from networking module.
     * using byte[]
     *
     * @param packet incoming data
     * @return CompressedPatch using the data
     */
    public static CompressedPatch deserializeCPacket(final byte[] packet) {
        final ByteBuffer buffer = ByteBuffer.wrap(packet);
        return deserializeCPacket(buffer, packet.length);
    }

    /**
     * Creates a Compresses patch from incoming packet from networking module.
     * using ByteBuffer.
     * Side Effects: Advances the buffer internal currentPos pointer.
     *
     * @param packetBuffer incoming data
     * @param packetLength length of data that corresponds to this packet
     * @return CompressedPatch using the data
     */
    public static CompressedPatch deserializeCPacket(final ByteBuffer packetBuffer, final int packetLength) {
        final int x = packetBuffer.getInt();
        final int y = packetBuffer.getInt();
        final int width = packetBuffer.getInt();
        final int height = packetBuffer.getInt();
        final int variableSpaces = 16;
        final int dataLength = packetLength - variableSpaces;
        final byte[] data = new byte[dataLength];
        try {
            packetBuffer.get(data, 0, dataLength);
        } catch (final Exception e) {
            System.err.println("Buffer Overflow : Required " + dataLength + " Got : " + packetBuffer.remaining());
            return null;
        }
        return new CompressedPatch(
            x, y, width, height, data
        );
    }
}
