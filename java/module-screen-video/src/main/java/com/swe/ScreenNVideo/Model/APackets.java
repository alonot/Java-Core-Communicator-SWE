package com.swe.ScreenNVideo.Model;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.InvalidParameterException;

/**
 * Audio Packets to be sent over networking layer.
 * @param data data to be sent
 * @param packetNumber packet number of the feed
 */
public record APackets(int packetNumber, byte[] data) {
    /**
     * Serializes AudioPacket for networking layer.
     *
     * @return serialized byte array
     * @throws IOException Can have exception while writing to the outputStream while creating the buffer data
     */
    public byte[] serializeAPackets() throws IOException {

        final ByteBuffer buffer = ByteBuffer.allocate(1 + 4 + data.length);
        // Write the packet Type
        buffer.put((byte) NetworkPacketType.APACKETS.ordinal());

        buffer.putInt(packetNumber);
        buffer.put(data);
        final byte[] output = new byte[buffer.position()];
        buffer.rewind();
        buffer.get(output);
        return output;
    }


    /**
     * get the Audio Packet.
     *
     * @param data incoming data
     * @return Audio Packet.
     */
    public static APackets deserialize(final byte[] data) {
        final ByteBuffer buffer = ByteBuffer.wrap(data);
        // get packet type
        final byte packetType = buffer.get();

        if (packetType != NetworkPacketType.APACKETS.ordinal()) {
            throw new InvalidParameterException(
                "Invalid Data type: Expected " + NetworkPacketType.APACKETS.ordinal() + " got : " + packetType);
        }
        // get feed number
        final int packetNumber = buffer.getInt();

        final byte[] audioData = new byte[buffer.remaining()];
        buffer.get(audioData);

        return new APackets(packetNumber, audioData);
    }
}
