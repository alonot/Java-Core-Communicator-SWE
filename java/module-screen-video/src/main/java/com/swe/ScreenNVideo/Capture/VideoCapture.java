package com.swe.ScreenNVideo.Capture;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;

/**
 * VideoCapture class for capturing video frames from webcam.
 * Provides interface to capture images and convert them to matrix format.
 */
public class VideoCapture extends ICapture {

    /** Capture parameters. */
    private Dimension captureArea;
    /** Capture parameters. */
    private Point captureLocation;

    /** Listener for frame capture events. */
    private FrameCaptureListener listener;

    // --- Field ADDED from the 'another branch' code ---
    /** Webcam object for video capture. */
    private Webcam webcam;
    // --- End of ADDED field ---

    /** Default capture settings. */
    private static final int DEFAULT_WIDTH = 640;
    /** Default capture settings. */
    private static final int DEFAULT_HEIGHT = 480;
    /** Default capture settings. */
    private static final int DEFAULT_X = 100;
    /** Default capture settings. */
    private static final int DEFAULT_Y = 100;
    
    /** Check if running on macOS. */
    private static final boolean IS_MAC = System.getProperty("os.name").toLowerCase().contains("mac");

    /**
     * Constructor - initializes with default screen capture area.
     *
     */
    public VideoCapture() {
        this.captureArea = new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        this.captureLocation = new Point(DEFAULT_X, DEFAULT_Y);
    }

    /**
     * Constructor with custom capture area.
     * @param x X coordinate of capture area
     * @param y Y coordinate of capture area
     * @param width Width of capture area
     * @param height Height of capture area
     */
    public VideoCapture(final int x, final int y, final int width, final int height) {
        this();
        this.captureLocation = new Point(x, y);
        this.captureArea = new Dimension(width, height);
    }

    private void openWebcam() {
        try {
            this.webcam = Webcam.getDefault();
            if (this.webcam == null) {
                throw new IllegalStateException("No webcam found. Check camera permissions" 
                    + (IS_MAC ? " in System Preferences > Security & Privacy > Camera" : ""));
            }

            // Use the captureArea set by the constructor
            Dimension resolution = this.captureArea;
            // If default Robot values were used, switch to a standard webcam res
            if (resolution.width == DEFAULT_WIDTH && resolution.height == DEFAULT_HEIGHT) {
                resolution = WebcamResolution.VGA.getSize();
            }

            this.webcam.setCustomViewSizes(new Dimension[]{resolution});
            this.webcam.setViewSize(resolution);
            
            // Mac-specific: Use async open with wait to avoid timeout issues
            if (IS_MAC) {
                this.webcam.open(true); // async=true for Mac
                // Wait for webcam to be ready (max 10 seconds)
                int waitCount = 0;
                while (!this.webcam.isOpen() && waitCount < 100) {
                    Thread.sleep(100);
                    waitCount++;
                }
                if (!this.webcam.isOpen()) {
                    throw new IllegalStateException("Webcam open timeout. Check: camera permissions, "
                        + "no other app using camera, IDE has camera access");
                }
            } else {
                this.webcam.open();
            }

            System.out.println("VideoCapture initialized: " + webcam.getName());

        } catch (Exception e) {
            System.err.println("Error initializing Webcam: " + e.getMessage());
            e.printStackTrace();
            this.webcam = null;
            if (listener != null) {
                listener.onCaptureError("Failed to initialize capture: " + e.getMessage());
            }
        }
    }

    /**
     * Converts INT_RGB image to INT_ARGB image.
     * @param src INT_ARGB image
     * @return INT_ARGB image
     */
    public static BufferedImage ensureIntARGB(final BufferedImage src) {
        if (src.getType() == BufferedImage.TYPE_INT_ARGB) {
            final BufferedImage clone = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
            final Graphics2D g = clone.createGraphics();
            g.drawImage(src, 0, 0, null);
            g.dispose();
            return clone;
        }

        final BufferedImage converted = new BufferedImage(
                src.getWidth(),
                src.getHeight(),
                BufferedImage.TYPE_INT_ARGB
        );
        final Graphics2D g = converted.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return converted;
    }


    /**
     * Capture a single frame.
     * @return BufferedImage of the captured frame
     */
    public BufferedImage capture() {


        // Lazy initialization: Initialize webcam on first capture attempt
        if (this.webcam == null) {
            System.out.println("Initializing webcam for the first time...");
            openWebcam();
        }

        // --- Webcam capture logic ---
        if (webcam == null || !webcam.isOpen()) {
            System.err.println("Capture not started or webcam not available");
            return null;
        }

        try {
            // Removed empty 'if' block that caused EmptyBlock error
            return ensureIntARGB(webcam.getImage());

        } catch (Exception e) {
            System.err.println("Error capturing frame: " + e.getMessage());
            if (listener != null) {
                listener.onCaptureError("Frame capture error: " + e.getMessage());
            }
            return null;
        }
    }

    @Override
    public void reInit() {
        openWebcam();
    }

    /**
     * Set capture area and location.
     * NOTE: Due to constraints, calling this will NOT update the webcam
     * resolution after the first capture.)
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @param width Width of capture area
     * @param height Height of capture area
     */
    public void setCaptureArea(final int x, final int y, final int width, final int height) {
        this.captureLocation = new Point(x, y);
        this.captureArea = new Dimension(width, height);
        System.out.println("Capture area updated: " + width + "x" + height + " at (" + x + "," + y + ")");
    }


    /**
     * Get screen dimensions.
     * @return Dimension of the screen.
     */
    public static Dimension getScreenDimensions() {
        final Toolkit toolkit = Toolkit.getDefaultToolkit();
        return toolkit.getScreenSize();
    }

    // --- Method ADDED from 'another branch' to work with test_video.java ---
    /**
     * Closes the webcam to release the resource.
     * This is an important new method to call when you are done.
     */
    public void close() {
        if (webcam != null && webcam.isOpen()) {
            webcam.close();
            System.out.println("Webcam closed.");
        }
    }
    // --- End of ADDED method ---


    // Added here so the file could be self-contained for testing.
}