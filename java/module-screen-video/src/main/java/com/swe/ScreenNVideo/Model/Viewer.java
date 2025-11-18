package com.swe.ScreenNVideo.Model;

import com.swe.networking.ClientNode;

/**
 * Represents a Viewer for Screen Share module.
 */
public class Viewer {
    /**
     * Client node for this viewer.
     */
    private final ClientNode node;
    /**
     * This viewer requires compressed feed or not.
     */
    private boolean requireCompressed;

    public ClientNode getNode() {
        return node;
    }

    public boolean isRequireCompressed() {
        return requireCompressed;
    }

    public Viewer(final ClientNode nodeArgs, final boolean requireCompressedArgs) {
        node = nodeArgs;
        requireCompressed = requireCompressedArgs;
    }

    public void setRequireCompressed(final boolean val) {
        requireCompressed = val;
    }
}
