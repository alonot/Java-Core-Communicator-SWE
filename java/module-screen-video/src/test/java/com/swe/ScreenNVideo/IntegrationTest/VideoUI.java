/**
 * Contributed by Priyanshu Pandey.
 */

package com.swe.ScreenNVideo.IntegrationTest;

import com.swe.ScreenNVideo.Utils;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * JavaFX-based UI for displaying video or screen frames received from the server.
 * <p>
 *     The {@code VideoUI} class creates a window that renders incoming frames
 *     using an {@link ImageView}. It drops frames if an update is still in progress
 *     to maintain smooth performance and avoid UI thread congestion.
 * </p>
 *
 */
public class VideoUI extends Application {

    /** The {@link ImageView} component used to display the current video frame. */
    private static ImageView imageView;

    /** Timestamp of the last rendered frame, used to calculate client-side FPS. */
    private static long start = 0;

    /** flag to track if an update is in progress. */
    private static final AtomicBoolean UPDATING = new AtomicBoolean(false);

    @Override
    public void start(final Stage stage) {
        imageView = new ImageView();
        imageView.setPreserveRatio(true);
        imageView.setFitWidth(Utils.SERVER_WIDTH);
        imageView.setFitHeight(Utils.SERVER_HEIGHT);

        final StackPane root = new StackPane(imageView);
        final Scene scene = new Scene(root, Utils.SERVER_WIDTH, Utils.SERVER_HEIGHT);

        stage.setTitle("Screen & Video Display");
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Display a frame from byte[] (converted to int[][] outside).
     * Drops new frames if the previous one is still being processed.
     *
     * @param pixels a 2D array of ARGB pixel values representing the image frame to display
     */
    public static void displayFrame(final int[][] pixels) {
        if (imageView == null) {
            System.err.println("No Image View");
            return;
        }

        // if already updating, drop this frame
        if (!UPDATING.compareAndSet(false, true)) {
            return;
        }

        final int height = pixels.length;
        final int width = pixels[0].length;

        final WritableImage writableImage = new WritableImage(width, height);
        final PixelWriter pw = writableImage.getPixelWriter();

        for (int x = 0; x < height; x++) {
            for (int y = 0; y < width; y++) {
                pw.setArgb(y, x, pixels[x][y]);
            }
        }

        Platform.runLater(() -> {
            try {
                System.out.println("Client FPS : "
                        + (int) ((double) Utils.SEC_IN_MS / ((System.nanoTime() - start)
                        / (double) Utils.MSEC_IN_NS)));
                imageView.setImage(writableImage);
                start = System.nanoTime();
            } finally {
                // release flag so next frame can proceed
                UPDATING.set(false);
            }
        });
    }
}
