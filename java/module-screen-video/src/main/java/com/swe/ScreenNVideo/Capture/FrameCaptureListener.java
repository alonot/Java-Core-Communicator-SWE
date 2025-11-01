/**
 * Contributed by Bhupati Varun.
 */

package com.swe.ScreenNVideo.Capture;

/**
 * Interface for handling captured frames.
 */
public interface FrameCaptureListener {
    void onCaptureError(String error);
    
    void onCaptureStarted();

    void onCaptureStopped();
}
