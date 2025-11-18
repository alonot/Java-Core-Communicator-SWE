package com.swe.ScreenNVideo;


import com.swe.ScreenNVideo.Capture.BackgroundCaptureManager;
import com.swe.ScreenNVideo.Codec.ADPCMDecoder;
import com.swe.ScreenNVideo.Model.APackets;
import com.swe.ScreenNVideo.Model.CPackets;
import com.swe.ScreenNVideo.Model.Feed;
import com.swe.ScreenNVideo.Model.IPPacket;
import com.swe.ScreenNVideo.Model.NetworkPacketType;
import com.swe.ScreenNVideo.Model.RImage;
import com.swe.ScreenNVideo.Model.Viewer;
import com.swe.ScreenNVideo.PatchGenerator.CompressedPatch;
import com.swe.ScreenNVideo.Playback.AudioPlayer;
import com.swe.ScreenNVideo.Synchronizer.FeedData;
import com.swe.ScreenNVideo.Synchronizer.ImageSynchronizer;
import com.swe.core.RPCinterface.AbstractRPC;
import com.swe.networking.AbstractNetworking;
import com.swe.networking.ClientNode;
import com.swe.networking.MessageListener;
import com.swe.networking.ModuleType;

import javax.sound.sampled.LineUnavailableException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Media Manager for Screen N Video.
 * - Manages Screen Capture and Video Capture
 */
public class MediaCaptureManager implements CaptureManager {
    /**
     * Port for the server.
     */
    private final int port;

    /**
     * VideoComponent object : manages the video overlay and diffing.
     */
    private final VideoComponents videoComponent;
    /**
     * Image synchronizer object.
     */
    private final HashMap<String, ImageSynchronizer> imageSynchronizers;

    /**
     * Networking object.
     */
    private final AbstractNetworking networking;
    /**
     * RPC object.
     */
    private final AbstractRPC rpc;

    /**
     * Cached IP for this machine to avoid repeated socket calls.
     */
    private final String localIp;

    /**
     * List of viewers to send the video Feed.
     */
    private final HashMap<String, Viewer> viewers;

    /**
     * Client handler for incoming messages.
     */
    private final ClientHandler clientHandler;

    /**
     * Audio Player object.
     */
    private final AudioPlayer audioPlayer;

    /**
     * Audio Decoder object.
     */
    private final ADPCMDecoder audioDecoder;

    /**
     * Constructor for the MediaCaptureManager.
     *
     * @param argNetworking Networking object
     * @param argRpc        RPC object
     * @param portArgs      Port for the server
     */
    public MediaCaptureManager(final AbstractNetworking argNetworking, final AbstractRPC argRpc, final int portArgs) {
        this.rpc = argRpc;
        this.port = portArgs;
        this.networking = argNetworking;
        final CaptureComponents captureComponents = new CaptureComponents(networking, rpc, port, (k,v) -> updateImage(k,v));
        audioPlayer = new AudioPlayer(Utils.DEFAULT_SAMPLE_RATE, Utils.DEFAULT_CHANNELS, Utils.DEFAULT_SAMPLE_SIZE);
        audioDecoder = new ADPCMDecoder();
        final BackgroundCaptureManager backgroundCaptureManager = new BackgroundCaptureManager(captureComponents);
        videoComponent = new VideoComponents(Utils.FPS, rpc, captureComponents, backgroundCaptureManager);

        captureComponents.startAudioLoop();
        backgroundCaptureManager.start();
        try {
            audioPlayer.init();
        } catch (LineUnavailableException e) {
            System.err.println("Unable to connect to Line");
        }

        imageSynchronizers = new HashMap<>();
        viewers = new HashMap<>();

        // Cache local IP once to avoid repeated socket operations during capture
        this.localIp = Utils.getSelfIP();
        System.out.println(this.localIp);

//        addParticipant(localIp, false);

        clientHandler = new MediaCaptureManager.ClientHandler();

        networking.subscribe(ModuleType.SCREENSHARING.ordinal(), clientHandler);
    }

    public Void updateImage(String ip, boolean val) {
        ImageSynchronizer imageSynchronizer = imageSynchronizers.get(ip);
        if (imageSynchronizer == null) {
            return null;
        }
        imageSynchronizer.reqCompression = val;
        imageSynchronizer.waitingForFullImage = true;
        return null;
    }

    /**
     * Broadcast join meeting to available IPs.
     * Only till broadcast is supported, multicast not supported yet.
     */
    public void broadcastJoinMeeting() {
        final IPPacket subscriberPacket = new IPPacket(localIp, true);

        // System.out.println("Broadcasting join meeting to : " + Arrays.toString(clientNodes));
        final byte[] subscribeData = subscriberPacket.serialize(NetworkPacketType.SUBSCRIBE_AS_VIEWER);
        networking.broadcast(subscribeData, ModuleType.SCREENSHARING.ordinal(), 2);
    }

    private void addParticipant(final String ip, final boolean reqCompression) {
        if (ip == null) {
            return;
        }
        if (ip.equals(localIp)) {
            return;
        }
        final ClientNode node = new ClientNode(ip, port);
        final Viewer viewer = viewers.computeIfAbsent(ip, k -> new Viewer(node, reqCompression));
        viewer.setRequireCompressed(reqCompression);
        imageSynchronizers.computeIfAbsent(ip, k -> new ImageSynchronizer(videoComponent.getVideoCodec()));
        rpc.call(Utils.SUBSCRIBE_AS_VIEWER, ip.getBytes());
    }

    private void removeViewer(final String ip) {
        viewers.remove(ip);
        imageSynchronizers.remove(ip);
    }

    /**
     * Server-side of the ScreenNVideo.
     */
    @Override
    public void startCapture() throws ExecutionException, InterruptedException {

        System.out.println("Starting capture");
        int[][] feed = null;
        while (true) {
            final Feed encodedFeed = videoComponent.captureScreenNVideo();
            final int[][] newFeed = videoComponent.getFeed();
            if (encodedFeed == null) {
                if (feed != null && newFeed == null) {
                    final IPPacket subscriberPacket = new IPPacket(localIp, false);
                    final byte[] subscribeData = subscriberPacket.serialize(NetworkPacketType.STOP_SHARE);
                    sendDataToViewers(subscribeData, k -> true);
                    feed = null;
                }
            } else {
                feed = newFeed;
                // send compressedFeed
//                System.out.println("Sending to Compress");
                sendDataToViewers(encodedFeed.compressedFeed(), Viewer::isRequireCompressed);
                // send unCompressedFeed
//                System.out.println("Sending to uncompress");
                sendDataToViewers(encodedFeed.unCompressedFeed(), viewer -> !viewer.isRequireCompressed());
            }
            // get audio Feed
            final byte[] encodedAudio = videoComponent.captureAudio();
            if (encodedAudio == null) {
                continue;
            }

            networking.broadcast(encodedAudio, ModuleType.SCREENSHARING.ordinal(), 2);
//            sendDataToViewers(encodedAudio, viewer -> true);
        }
    }

    /**
     * Applies filter and send data to those viewers.
     *
     * @param feed         the data to send
     * @param viewerFilter predicate to filter which viewers should receive the data
     */
    private void sendDataToViewers(final byte[] feed, final java.util.function.Predicate<Viewer> viewerFilter) {
        if (feed == null) {
            return;
        }

        final ClientNode[] clientNodes = viewers.values().stream()
            .filter(viewerFilter)
            .map(Viewer::getNode)
            .toArray(ClientNode[]::new);

        if (clientNodes.length == 0) {
            return;
        }

        System.out.println("Size : " + feed.length / Utils.KB + " KB");
        networking.sendData(feed, clientNodes, ModuleType.SCREENSHARING.ordinal(), 2);

        System.out.println("Sent to viewers " + clientNodes.length );
        for (ClientNode c : clientNodes) {
            System.out.println(c.hostName());
        }
//        CompletableFuture.runAsync(() -> {
//        try {
//            Thread.sleep(5000);
//        } catch (InterruptedException e) {
//            System.err.println("Error in timer");
//            throw new RuntimeException(e);
//        }
//        });
    }


    class ClientHandler implements MessageListener {
        /**
         * Cache for NetworkType enum.
         */
        private final NetworkPacketType[] enumVals;

        ClientHandler() {
            enumVals = NetworkPacketType.values();
        }

        @Override
        public void receiveData(final byte[] data) {

//            System.out.println("Received");
            if (data.length == 0) {
                return;
            }
//            // System.out.println("first 40 bytes:" + (Arrays.toString(Arrays.copyOf(data, 40))));
            final byte packetType = data[0];
            if (packetType > enumVals.length) {
                final int printLen = 34;
                System.err.println("Error: Invalid packet type: " + packetType + "  " + data.length);
                System.err.println("Error: Packet data: " + (Arrays.toString(Arrays.copyOf(data, printLen))));
                return;
            }
            final NetworkPacketType type = enumVals[packetType];
            switch (type) {
                case NetworkPacketType.LIST_CPACKETS -> {
//                    // System.out.println(Arrays.toString(Arrays.copyOf(data, 10)));
                    final CPackets networkPackets = CPackets.deserialize(data);
                    System.out.println(
                        "Received CPackets : " + data.length / Utils.KB + " KB " + networkPackets.packetNumber());
                    System.out.println(
                        "Height: " + networkPackets.height() + " Width: " + networkPackets.width() + " from "
                            + networkPackets.ip() + " " + networkPackets.compress());

                    ImageSynchronizer imageSynchronizer = imageSynchronizers.get(networkPackets.ip());
                    if (imageSynchronizer == null) {
                        // add new participant if not already present, with true by default
                        addParticipant(networkPackets.ip(), true);
                        imageSynchronizer = imageSynchronizers.get(networkPackets.ip());
                    }


//                     System.out.println("Recieved " + networkPackets.packetNumber() + "; Expected : " +
//                        imageSynchronizer.getExpectedFeedNumber() + " " + imageSynchronizer.getHeap().size() + " " + imageSynchronizer.waitingForFullImage);

                    if (networkPackets.isFullImage()) {
                        System.out.println("Full Image");
                        // reset expected feed number
                        imageSynchronizer.setExpectedFeedNumber(networkPackets.packetNumber());

                        imageSynchronizer.waitingForFullImage = false;

                    }

                    imageSynchronizer.getHeap().add(new FeedData(networkPackets.packetNumber(), networkPackets));

                    // if heap is growing too large, request a full frame to resync
                    if (imageSynchronizer.getHeap().size() >= Utils.MAX_HEAP_SIZE) {
                        System.out.println("Too Large");
                        askForFullImage(networkPackets.ip(), imageSynchronizer.reqCompression);
                        imageSynchronizer.waitingForFullImage = true;
                        imageSynchronizer.getHeap().clear();
                        return;
                    }


                    // drop all entries older than this full image
                    while (!imageSynchronizer.getHeap().isEmpty()
                        && imageSynchronizer.getHeap().peek().getFeedNumber()
                        < imageSynchronizer.getExpectedFeedNumber()) {
//                        System.out.println("Removing " + imageSynchronizer.getHeap().peek().getFeedNumber());
                        imageSynchronizer.getHeap().poll();
                    }


                    int[][] image = null;
                    while (true) {

                        // If the next expected patch hasn't arrived yet, wait
                        final FeedData feedData = imageSynchronizer.getHeap().peek();
                        if (feedData == null || feedData.getFeedNumber() != imageSynchronizer.getExpectedFeedNumber()) {
//                            if (feedData != null) {
//                                System.out.println("Expected " + imageSynchronizer.getExpectedFeedNumber()
//                                + " GOT : " + feedData.getFeedNumber() + " Len " + imageSynchronizer.getHeap().size());
//                            }
                            break;
                        }

                        final FeedData minFeedNumPacket = imageSynchronizer.getHeap().poll();

                        if (minFeedNumPacket == null) {
                            break;
                        }

                        final CPackets minFeedCPacket = minFeedNumPacket.getFeedPackets();
                        System.out.println("Min Feed Packet " + minFeedCPacket.packetNumber());
                        final List<CompressedPatch> patches = minFeedCPacket.packets();
                        final int newHeight = minFeedCPacket.height();
                        final int newWidth = minFeedCPacket.width();

                        imageSynchronizer.setExpectedFeedNumber(imageSynchronizer.getExpectedFeedNumber() + 1);

                        try {
                            image = imageSynchronizer.synchronize(newHeight, newWidth, patches,
                                networkPackets.compress());
                        } catch (Exception e) {
                            System.out.println(
                                "-----------------------------=------------------------Exception " + e.getMessage());
                            e.printStackTrace();
                            askForFullImage(networkPackets.ip(), imageSynchronizer.reqCompression);
                            imageSynchronizer.getHeap().clear();
                            return;
                        }
                    }

                    if (image == null) {
                        return;
                    }


                    final RImage rImage = new RImage(image, networkPackets.ip());
                    final byte[] serializedImage = rImage.serialize();
                    System.out.println("Sending to UI" + ("; Expected : "
                        + imageSynchronizer.getExpectedFeedNumber()));
                    try {
                        final byte[] res = rpc.call(Utils.UPDATE_UI, serializedImage).get();
                        if (res.length == 0) {
                            return;
                        }
                        final boolean success = res[0] == 1;
                        if (!success) {
                            addParticipant(networkPackets.ip(), imageSynchronizer.reqCompression);
                        }
                        System.out.println("Done");
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace(System.out);
                    }

                }
                case NetworkPacketType.SUBSCRIBE_AS_VIEWER -> {
                    final IPPacket viewerIP = IPPacket.deserialize(data);
                    System.out.println("Viewer joined" + viewerIP);
                    addUserNFullImageRequest(viewerIP.ip(), viewerIP.reqCompression());
                }
                case STOP_SHARE -> {
                    final String viewerIP = IPPacket.deserialize(data).ip();
                    rpc.call(Utils.STOP_SHARE, viewerIP.getBytes());
                }
                case APACKETS -> {
                    final APackets audioPackets = APackets.deserialize(data);
                     System.out.println("Audio" + audioPackets.packetNumber());
                    final byte[] audioBytes = audioDecoder.decode(audioPackets.data());
                    audioPlayer.play(audioBytes);
                }
                case UNSUBSCRIBE_AS_VIEWER -> {
                    final IPPacket viewerIP = IPPacket.deserialize(data);
                    System.out.println("Viewer requested to be removed" + viewerIP);
                    removeViewer(viewerIP.ip());
                }
                default -> {
                }
            }
        }

        private void askForFullImage(final String ip, final boolean reqCompress) {
            System.out.println("Asking for data...");
            final IPPacket subscribePacket = new IPPacket(localIp, reqCompress);
            final byte[] subscribeData = subscribePacket.serialize(NetworkPacketType.SUBSCRIBE_AS_VIEWER);
            final ClientNode destNode = new ClientNode(ip, port);
            networking.sendData(subscribeData, new ClientNode[] {destNode}, ModuleType.SCREENSHARING.ordinal(), 2);
        }

        public void addUserNFullImageRequest(final String ip, final boolean reqCompression) {
            addParticipant(ip, reqCompression);
            final Feed fullFeed = videoComponent.captureFullImage();
            if (fullFeed == null) {
                return;
            }
            // get the required type(Compress/UnCompress) from viewer list
            // use Compress if not video and asked for it
            final boolean isOnlyVideoOn = videoComponent.isVideoCaptureOn() && !videoComponent.isScreenCaptureOn();
            final boolean useCompress = isOnlyVideoOn || reqCompression;
            byte[] fullImageEncoded = null;
            if (useCompress) {
                fullImageEncoded = fullFeed.compressedFeed();
            } else {
                fullImageEncoded = fullFeed.unCompressedFeed();
            }
            if (fullImageEncoded == null) {
                return;
            }
            System.out.println("Sending Full Image");
            networking.sendData(fullImageEncoded, new ClientNode[] {new ClientNode(ip, port)},
                ModuleType.SCREENSHARING.ordinal(), 2);
        }
    }
}
