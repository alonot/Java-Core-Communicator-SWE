/**
 * Contributed by Chirag Varshney.
 */

package com.swe.ScreenNVideo;

import com.swe.RPC.AbstractRPC;
import com.swe.ScreenNVideo.Codec.Codec;
import com.swe.ScreenNVideo.Codec.JpegCodec;
import com.swe.ScreenNVideo.PatchGenerator.CompressedPatch;
import com.swe.ScreenNVideo.PatchGenerator.Hasher;
import com.swe.ScreenNVideo.PatchGenerator.IHasher;
import com.swe.ScreenNVideo.PatchGenerator.PacketGenerator;
import com.swe.ScreenNVideo.Serializer.CPackets;
import com.swe.ScreenNVideo.Serializer.RImage;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Class containing Components to capture and process video.
 */
public class VideoComponents {
    /**
     * Video codec object.
     */
    private final Codec videoCodec;

    /**
     * Patch generator object.
     */
    private final PacketGenerator patchGenerator;

    /**
     * RPC object.
     */
    private final AbstractRPC rpc;

    /**
     * Keeps track of previous click.
     */
    private long start = 0L;

    /**
     * Limit the FPS at server.
     */
    private final double timeDelay;

    /**
     * Local IP address.
     */
    private final String localIp = Utils.getSelfIP();

    /**
     * Class conatining Components to capture feed.
     */
    private final CaptureComponents captureComponents;

    VideoComponents(final int fps, final AbstractRPC rpcArg, final CaptureComponents captureComponentsArgs) {
        this.rpc = rpcArg;
        this.captureComponents = captureComponentsArgs;
        final IHasher hasher = new Hasher(Utils.HASH_STRIDE);
        videoCodec = new JpegCodec();
        patchGenerator = new PacketGenerator(videoCodec, hasher);
        // initialize bounded queue and start worker thread that reads from the queue and updates the UI
        this.uiQueue = new ArrayBlockingQueue<>(UI_QUEUE_CAPACITY);
        final Thread uiWorkerThread = new Thread(this::uiWorkLoop, "MediaCaptureManager-UI-Worker");
        uiWorkerThread.setDaemon(true);
        uiWorkerThread.start();
        timeDelay = (1.0 / fps) * Utils.SEC_IN_NS;
    }

    private void uiWorkLoop() {
//        int count = 0;
//        long prev = 0;
        while (!Thread.currentThread().isInterrupted()) {
            try {
                final int[][] frame = uiQueue.take(); // blocks until a frame is available
                try {
//                        System.out.println("UI Frame Processed : " + (++count));
//                        long curr = System.nanoTime();
                    final RImage rImage = new RImage(frame, localIp);
                    final byte[] serializedImage = rImage.serialize();
//                        System.out.println("Data size : " + serializedImage.length / ((double)Utils.KB));
                    // Fire-and-forget; do not block capture thread — worker thread can block on network
//                        System.out.println("UI Serialization Time : "
//                            + (System.nanoTime() - curr) / ((double) Utils.MSEC_IN_NS));
                    System.out.println("Time from previous send: " + (System.nanoTime() - prev)
                        / ((double) Utils.MSEC_IN_NS));
                    try {
                        rpc.call(Utils.UPDATE_UI, serializedImage);
                    } catch (final Exception e) {
                        e.printStackTrace();
                    }
//                        System.out.println("UI RPC Time : "
//                            + (System.nanoTime() - curr) / ((double) Utils.MSEC_IN_NS));
//                        prev = System.nanoTime();
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            } catch (final InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public Codec getVideoCodec() {
        return videoCodec;
    }

    /**
     * Previous time stamp.
     */
    private long prev = 0;

    /**
     * limit the queue length.
     */
    private static final int UI_QUEUE_CAPACITY = 2;
    /**
     * Bounded queue for UI frames.
     */
    private final ArrayBlockingQueue<int[][]> uiQueue;

    // Submit an RImage serialization + RPC task to the background worker queue. If the queue is full,
    // this method will drop the frame (non-blocking) to keep capture smooth.
    private void submitUIUpdate(final int[][] frame) {
        // only add to the queue; worker thread handles serialization and sending
        if (frame == null) {
            return;
        }
        final boolean offered = uiQueue.offer(frame);
        if (!offered) {
            // drop the frame
            System.err.println("UI queue full — dropping frame");
        }
    }

    /**
     * Captures the Video using CaptureComponents, handles overlay ,diffing and converting it to bytes  .
     *
     * @return encoded Patches to be sent through the network
     */
    protected byte[] captureScreenNVideo() {
        final long currTime = System.nanoTime();
        final long diff = currTime - start;
        if (diff < timeDelay) {
            return null;
        }
//            System.out.println("Time Delay " + ((currTime - prev)
//                / ((double) Utils.MSEC_IN_NS)) + " " + (diff / ((double) Utils.MSEC_IN_NS)));
        System.out.println("\nServer FPS : "
            + (int) ((double) (Utils.SEC_IN_MS) / (diff / ((double) (Utils.MSEC_IN_NS)))));
        start = System.nanoTime();

        final int[][] feed = captureComponents.getFeed();
        if (feed == null) {
//            System.err.println("No feed");
            return null;
        }

//            System.out.println("Time taken : Video | PacketGen");
//            final long curr1 = System.nanoTime();
//            System.out.print((curr1 - start) / (double) (Utils.MSEC_IN_NS) + " | ");

//            System.out.println("Feed Size : " + feed.length + " x " + feed[0].length);
        videoCodec.setScreenshot(feed);

        final List<CompressedPatch> patches = patchGenerator.generatePackets(feed);

        if (patches.isEmpty()) {
            prev = System.nanoTime();
            return null;
        }

        final CPackets networkPackets = new CPackets(localIp, patches);
        byte[] encodedPatches = null;
        int tries = Utils.MAX_TRIES_TO_SERIALIZE;
        while (tries-- > 0) {
            // max tries 3 times to convert the patch
            try {
                encodedPatches = networkPackets.serializeCPackets();
                break;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (tries < 0 || encodedPatches == null) {
            System.err.println("Error: Unable to serialize compressed packets");
            prev = System.nanoTime();
            return null;
        }

        // Asynchronously send a serialized RImage to the UI so we don't block capture
        // (frame is deep-copied inside submitUIUpdate)
        submitUIUpdate(feed);

        prev = System.nanoTime();
//            System.out.print((prev - curr1) / (double) (Utils.MSEC_IN_NS));
//            System.out.println("\nSending to " + networkPackets.getIp());
        return encodedPatches;
    }
}
