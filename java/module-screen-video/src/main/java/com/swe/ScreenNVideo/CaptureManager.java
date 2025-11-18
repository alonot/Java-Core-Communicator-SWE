package com.swe.ScreenNVideo;

import java.util.concurrent.ExecutionException;

/**
 * Interface to other modules.
 */
public interface CaptureManager {
    /**
     * Start Screen And Video Caputering service
     * Never returns.
     */
    void startCapture() throws ExecutionException, InterruptedException;

}
