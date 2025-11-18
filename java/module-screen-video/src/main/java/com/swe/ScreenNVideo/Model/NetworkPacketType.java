package com.swe.ScreenNVideo.Model;

/**
 * The type of the network packet.
 */
public enum NetworkPacketType {
    /**
     * Depicts list of compressed packets.
     */
    LIST_CPACKETS,
    /**
     * Depicts a request to subscribe as a viewer.
     */
    SUBSCRIBE_AS_VIEWER,
    /**
     * Represents a request to unsubscribe as a viewer.
     */
    UNSUBSCRIBE_AS_VIEWER,
    /**
     * Depicts a request to nullify the image at viewer side.
     */
    STOP_SHARE,
    /**
     * Depicts audio Packet.
     */
    APACKETS,
}
