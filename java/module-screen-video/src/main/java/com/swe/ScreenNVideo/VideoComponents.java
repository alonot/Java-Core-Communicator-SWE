package com.swe.ScreenNVideo;

import com.swe.ScreenNVideo.Capture.BackgroundCaptureManager;
import com.swe.ScreenNVideo.Codec.ADPCMEncoder;
import com.swe.ScreenNVideo.Codec.Codec;
import com.swe.ScreenNVideo.Codec.JpegCodec;
import com.swe.ScreenNVideo.PatchGenerator.Hasher;
import com.swe.ScreenNVideo.PatchGenerator.IHasher;
import com.swe.ScreenNVideo.PatchGenerator.PacketGenerator;
import com.swe.ScreenNVideo.Model.APackets;
import com.swe.ScreenNVideo.Model.CPackets;
import com.swe.ScreenNVideo.Model.Feed;
import com.swe.ScreenNVideo.Model.FeedPatch;
import com.swe.ScreenNVideo.Model.RImage;
import com.swe.core.RPCinterface.AbstractRPC;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Class containing Components to capture and process video.
 */
public class VideoComponents {
    /**
     * Video codec object.
     */
    private final JpegCodec videoCodec;

    /**
     * Audio Encoder.
     */
    private final ADPCMEncoder audioEncoder;

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
     * Video Feed number.
     */
    private int videoFeedNumber = 0;

    /**
     * Audio Feed number.
     */
    private int audioFeedNumber = 0;

    /**
     * Current feed.
     */
    private int[][] feed;

    public int[][] getFeed() {
        return feed;
    }

    private int runCount = 0;

    /**
     * Class conatining Components to capture feed.
     */
    private final CaptureComponents captureComponents;

    /**
     * Backgound Thread that manages capturing task.
     */
    private final BackgroundCaptureManager bgCapManager;

    public boolean isVideoCaptureOn() {
        return captureComponents.isVideoCaptureOn();
    }

    public boolean isScreenCaptureOn() {
        return captureComponents.isScreenCaptureOn();
    }


    VideoComponents(final int fps, final AbstractRPC rpcArg, final CaptureComponents captureComponentsArgs,
                    final BackgroundCaptureManager bgCapManagerArgs) {
        this.rpc = rpcArg;
        this.captureComponents = captureComponentsArgs;
        this.bgCapManager = bgCapManagerArgs;
        final IHasher hasher = new Hasher(Utils.HASH_STRIDE);
        videoCodec = new JpegCodec();
        audioEncoder = new ADPCMEncoder();
        patchGenerator = new PacketGenerator(videoCodec, hasher);
        // initialize bounded queue and start worker thread that reads from the queue and updates the UI
        this.uiQueue = new ArrayBlockingQueue<>(UI_QUEUE_CAPACITY);
        final Thread uiWorkerThread = new Thread(this::uiWorkLoop, "MediaCaptureManager-UI-Worker");
        uiWorkerThread.setDaemon(true);
        uiWorkerThread.start();
        timeDelay = (1.0 / fps) * Utils.SEC_IN_NS;
    }

    /**
     * Captures the full image without diffing.
     *
     * @return encoded Patches to be sent through the network
     */
    public Feed captureFullImage() {
        if (feed == null) {
            return null;
        }

        final FeedPatch patches = patchGenerator.generateFullImage(feed);

        final CPackets compressedNetworkPackets =
            new CPackets(videoFeedNumber, localIp, true, true, feed.length, feed[0].length,
                patches.compressedPatches());
        System.out.println("Feed number : " + compressedNetworkPackets.packetNumber());
        final byte[] compressedEncodedPatches = serializeFeed(compressedNetworkPackets);

        final CPackets unCompressedNetworkPackets =
            new CPackets(videoFeedNumber, localIp, true, false, feed.length, feed[0].length,
                patches.unCompressedPatches());
        final byte[] unCompressedEncodedPatches = serializeFeed(unCompressedNetworkPackets);

        return new Feed(compressedEncodedPatches, unCompressedEncodedPatches);
    }

    private void uiWorkLoop() {
//        int count = 0;
//        long prev = 0;
        while (!Thread.currentThread().isInterrupted()) {
            try {
                final int[][] frame = uiQueue.take(); // blocks until a frame is available
                try {
                    final RImage rImage = new RImage(frame, localIp);
                    final byte[] serializedImage = rImage.serialize();

                    if (serializedImage == null) {
                        continue;
                    }
//                    System.out.println("Time from previous send: " + (System.nanoTime() - prev)
//                        / ((double) Utils.MSEC_IN_NS));
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
     * Captures the Audio, encode it and return back the bytes.
     */
    protected byte[] captureAudio() {
        final byte[] audioFeed = captureComponents.getAudioFeed();
        if (audioFeed == null) {
            return null;
        }
        final byte[] feed = audioEncoder.encode(audioFeed);
        final APackets audioPacket = new APackets(audioFeedNumber, feed);
        audioFeedNumber += 1;
        byte[] encodedPacket = null;
        int tries = Utils.MAX_TRIES_TO_SERIALIZE;
        while (tries-- > 0) {
            // max tries 3 times to convert the patch
            try {
                encodedPacket = audioPacket.serializeAPackets();
                break;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return encodedPacket;
    }

    /**
     * Captures the Video using CaptureComponents, handles overlay ,diffing and converting it to bytes  .
     *
     * @return encoded Patches to be sent through the network
     */
    protected Feed captureScreenNVideo() {
        final long currTime = System.nanoTime();
        final long diff = currTime - start;
        if (diff < timeDelay) {
            return null;
        }
//            System.out.println("Time Delay " + ((currTime - prev)
//                / ((double) Utils.MSEC_IN_NS)) + " " + (diff / ((double) Utils.MSEC_IN_NS)));
        start = System.nanoTime();

        final int[][] newFeed = captureComponents.getFeed();
        if (newFeed == null) {
            if (feed != null) {
                // previous feed exists
                feed = null;
                rpc.call(Utils.STOP_SHARE, localIp.getBytes());
            }
            return null;
        }

//        submitUIUpdate(newFeed);

//        final boolean toCompress = captureComponents.isVideoCaptureOn() && !captureComponents.isScreenCaptureOn();
        System.out.println("Server FPS : "
            + (int) ((double) (Utils.SEC_IN_MS) / (diff / ((double) (Utils.MSEC_IN_NS)))));

//        System.out.println("Time to get feed : " + (start - currTime) / ((double) (Utils.MSEC_IN_NS)));

        long curr1 = System.nanoTime();
        videoCodec.quantTime = 0;
        videoCodec.dctTime = 0;
        videoCodec.zigZagtime = 0;

        final FeedPatch patches = patchGenerator.generatePackets(newFeed);
        runCount++;


        // update the feed
        feed = newFeed;

        final CPackets compressedNetworkPackets =
            new CPackets(videoFeedNumber, localIp, false, true, feed.length, feed[0].length,
                patches.compressedPatches());
        System.out.println("Feed number : " + compressedNetworkPackets.packetNumber());
        final byte[] compressedEncodedPatches = serializeFeed(compressedNetworkPackets);

        final CPackets unCompressedNetworkPackets =
            new CPackets(videoFeedNumber, localIp, false, false, feed.length, feed[0].length,
                patches.unCompressedPatches());
        final byte[] unCompressedEncodedPatches = serializeFeed(unCompressedNetworkPackets);

        if (compressedEncodedPatches == null && unCompressedEncodedPatches == null) {
            // both are null
            if (runCount > 500) {
                System.err.println("Reinit the Video and Screen");
                if (captureComponents.isVideoCaptureOn()) {
                    bgCapManager.reInitVideo();
                }
                if (captureComponents.isScreenCaptureOn()) {
                    bgCapManager.reInitScreen();
                }
                runCount = 0;
            }
            prev = System.nanoTime();
            return null;
        }

        // make it zero. This will fill up to 500 in case no diff is detected from long time
        runCount = 0;

        videoFeedNumber++;

        // Asynchronously send a serialized RImage to the UI so we don't block capture
        submitUIUpdate(feed);

        prev = System.nanoTime();
        return new Feed(compressedEncodedPatches, unCompressedEncodedPatches);
    }

    /**
     * Serializes the Given packet.
     *
     * @param networkPackets to serialize
     * @return serialized bytes
     */
    private byte[] serializeFeed(final CPackets networkPackets) {
        byte[] encodedPatches = null;
        if (networkPackets.packets().isEmpty()) {
//            System.out.println("Empty");
            return null;
        }
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
        return encodedPatches;
    }
}


