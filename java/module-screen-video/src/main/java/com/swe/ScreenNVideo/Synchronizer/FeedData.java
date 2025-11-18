package com.swe.ScreenNVideo.Synchronizer;

import com.swe.ScreenNVideo.Model.CPackets;


/**
 * Represents a single video feed packet entry used in synchronization.
 * Stores the packet's feed number (for ordering) and its compressed data.
 */
public class FeedData {

    /** The sequence number of this packet.*/
    private final int feedNumber;

    /** The compressed packets.*/
    private final CPackets packets;

    /**
     * Creates a FeedData entry with its feed number and packet content.
     *
     * @param feedNum the sequence/feed number for ordering
     * @param packets the compressed packets associated with this sequence
     */
    public FeedData(int feedNum, CPackets packets) {
        this.feedNumber = feedNum;
        this.packets = packets;
    }

    /**
     * @return the feed/sequence number of this packet
     */
    public int getFeedNumber() {
        return this.feedNumber;
    }

    /**
     * @return the compressed Packet data
     */
    public CPackets getFeedPackets() {
        return this.packets;
    }
}
