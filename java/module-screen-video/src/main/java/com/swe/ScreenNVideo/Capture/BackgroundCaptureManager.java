package com.swe.ScreenNVideo.Capture;

import com.swe.ScreenNVideo.CaptureComponents;
import com.swe.ScreenNVideo.Utils;

import java.awt.AWTException;


/**
 * Manages background screen and video capture on a dedicated daemon thread.
 */
public final class BackgroundCaptureManager {

    /**
     * The dedicated background thread for performing captures.
     */
    private static Thread captureThread;
    /**
     * Instance for capturing the entire screen.
     */
    private ICapture screenCapture;
    /**
     * Instance for capturing a specific video region.
     */
    private final ICapture videoCapture;

    /**
     * CaptureComponents Object.
     */
    private final CaptureComponents capCom;

    /**
     * Private constructor to prevent instantiation.
     * @param capComObjArgs CaptureComponents Object.
     */
    public BackgroundCaptureManager(final CaptureComponents capComObjArgs) {
        this.capCom = capComObjArgs;
        screenCapture = new ScreenCapture();
        videoCapture = new VideoCapture();
    }

    /**
     * Starts the background capture daemon thread.
     */
    public void start() {
        if (captureThread != null && captureThread.isAlive()) {
            return; // Already running
        }
        captureThread = new Thread(this::captureLoop, "Background-Capture-Thread");
        captureThread.setDaemon(true); // Ensure thread doesn't block JVM shutdown
        captureThread.start();
        System.out.println("Background Capture Thread started.");
    }

    public void reInitVideo() {
        videoCapture.reInit();
    }

    public void reInitScreen() {
        screenCapture = new ScreenCapture();
    }


    /**
     * The main loop for the background capture thread.
     */
    private void captureLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                if (!capCom.isScreenCaptureOn() && !capCom.isVideoCaptureOn()) {
                    Thread.sleep(Utils.SEC_IN_MS);
                    continue;
                }

                if (capCom.isScreenCaptureOn()) {
                    try {
                        // Overwrite the volatile variable with the latest frame
//                        System.out.println("Capturing..");
                        capCom.setLatestScreenFrame(screenCapture.capture());
//                        System.out.println("Done Capturedd..");
                    } catch (AWTException e) {
                        System.err.println("Failed to capture screen: " + e.getMessage());
                        Thread.sleep(500);
                        screenCapture = new ScreenCapture();
                        capCom.setLatestScreenFrame(null); // Clear frame on error
                    }
                } else {
                    capCom.setLatestScreenFrame(null);
                }

                if (capCom.isVideoCaptureOn()) {
                    try {
                        // Overwrite the volatile variable with the latest frame
                        capCom.setLatestVideoFrame(videoCapture.capture());
                    } catch (AWTException e) {
                        System.err.println("Failed to capture video: " + e.getMessage());
                        capCom.setLatestVideoFrame(null); // Clear frame on error
                    }
                } else {
                    capCom.setLatestVideoFrame(null);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Preserve interrupt flag
                System.out.println("Background capture thread interrupted. Stopping capture loop.");
                break;
            }

        }
    }
}
