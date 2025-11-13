package com.swe.ScreenNVideo.Capture;

import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * The {@code ScreenCapture} class is responsible for capturing
 * the entire screen as a {@link BufferedImage}.
 *
 * <p>It uses the {@link Robot} class to create a screenshot of the
 * current display. Works on Windows, macOS, and Linux.
 */
public class ScreenCapture extends ICapture {

    /** The {@link Robot} used to capture the screen image. */
    private Robot robot;

    /** Cached screen rectangle to avoid repeated Toolkit calls. */
    private Rectangle screenRect;

    /** Executor for timeout protection. */
    private static final ExecutorService timeoutExecutor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "ScreenCapture-Timeout-Thread");
        t.setDaemon(true);
        return t;
    });

    /** Timeout for capture operations in seconds. */
    private static final int CAPTURE_TIMEOUT_SECONDS = 5;

    public ScreenCapture() {
        reInit();
    }

    /**
     * Captures the entire screen and returns it as a BufferedImage.
     *
     * @return BufferedImage containing the screenshot
     * @throws AWTException if the platform configuration does not allow low-level input control
     */
    @Override
    public BufferedImage capture() throws AWTException {

        // Capture with timeout protection
        return executeWithTimeout(
            () -> robot.createScreenCapture(screenRect),
            "Screen capture"
        );
    }

    /**
     * Executes a task with timeout protection.
     *
     * @param task The task to execute
     * @param operationName Name of the operation for error messages
     * @param <T> The return type of the task
     * @return The result of the task
     * @throws AWTException if the operation times out, is interrupted, or fails
     */
    private <T> T executeWithTimeout(Callable<T> task, String operationName) throws AWTException {
        try {
            Future<T> future = timeoutExecutor.submit(task);
            return future.get(CAPTURE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            String message = operationName + " timed out after " + CAPTURE_TIMEOUT_SECONDS + " seconds";
            System.err.println(message);
            reInit();
            throw new AWTException(message);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AWTException(operationName + " was interrupted: " + e.getMessage());
        } catch (Exception e) {
            throw new AWTException(operationName + " failed: " + e.getMessage());
        }
    }

    @Override
    public void reInit() {
        try {
            GraphicsDevice[] screens = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
            robot = new Robot(screens[0]);
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            screenRect = new Rectangle(screenSize);
            System.out.println("Reinitialized");
        } catch (AWTException e) {
            throw new RuntimeException(e);
        }
    }
}
