/**
 * Contributed by Priyanshu Pandey.
 */

package com.swe.ScreenNVideo;


import com.swe.RPC.AbstractRPC;
import com.swe.ScreenNVideo.Capture.BackgroundCaptureManager;
import com.swe.ScreenNVideo.PatchGenerator.CompressedPatch;

import com.swe.ScreenNVideo.Serializer.CPackets;
import com.swe.ScreenNVideo.Serializer.NetworkPacketType;
import com.swe.ScreenNVideo.Serializer.NetworkSerializer;
import com.swe.ScreenNVideo.Serializer.RImage;
import com.swe.ScreenNVideo.Synchronizer.ImageSynchronizer;
import com.swe.networking.ClientNode;
import com.swe.networking.ModuleType;
import com.swe.networking.SimpleNetworking.AbstractNetworking;
import com.swe.networking.SimpleNetworking.MessageListener;

import java.util.ArrayList;
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
     * CaptureComponent object.
     */
    private final CaptureComponents captureComponents;
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
    private final ArrayList<ClientNode> viewers;


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
        captureComponents = new CaptureComponents(networking, rpc, port);
        videoComponent = new VideoComponents(Utils.FPS, rpc, captureComponents);
        final BackgroundCaptureManager backgroundCaptureManager = new BackgroundCaptureManager(captureComponents);
        backgroundCaptureManager.start();

        imageSynchronizers = new HashMap<>();
        viewers = new ArrayList<>();

        // Cache local IP once to avoid repeated socket operations during capture
        this.localIp = Utils.getSelfIP();
        System.out.println(this.localIp);

        networking.subscribe(ModuleType.CHAT, new MediaCaptureManager.ClientHandler());
//        addParticipant(getSelfIP());
//        addParticipant("10.32.11.242");
//        addParticipant("10.32.9.37");
    }

    private void addParticipant(final String ip) {
        final ClientNode node = new ClientNode(ip, port);
        viewers.add(node);
        imageSynchronizers.put(ip, new ImageSynchronizer(videoComponent.getVideoCodec()));
    }

    /**
     * Server-side of the ScreenNVideo.
     */
    @Override
    public void startCapture() throws ExecutionException, InterruptedException {

        System.out.println("Starting capture");
        //noinspection InfiniteLoopStatement
        while (true) {
            final byte[] encodedPatches = videoComponent.captureScreenNVideo();
            if (encodedPatches == null) {
                continue;
            }
            sendImageToViewers(encodedPatches);
        }
    }

    private void sendImageToViewers(final byte[] feed) {
//        System.out.println("Size : " + feed.length / Utils.KB + " KB");
//        networking.sendData(feed, viewers.toArray(new ClientNode[0]), ModuleType.CHAT, 2);
//        SimpleNetworking.getSimpleNetwork().closeNetworking();
//        System.out.println("Sent to viewers");
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

//            System.out.println("Recieved");
            if (data.length == 0) {
                return;
            }

            final byte packetType = data[0];
            if (packetType > enumVals.length) {
                final int printLen = 20;
                System.err.println("Error: Invalid packet type: " + packetType + "  " + data.length);
                System.err.println("Error: Packet data: " + (Arrays.toString(Arrays.copyOf(data, printLen))));
                return;
            }
            final NetworkPacketType type = enumVals[packetType];
            switch (type) {
                case NetworkPacketType.LIST_CPACKETS -> {
//                    System.out.println("Received CPackets : " + data.length / Utils.KB);
                    final CPackets networkPackets = CPackets.deserialize(data);
                    final List<CompressedPatch> patches = networkPackets.getPackets();
                    final ImageSynchronizer imageSynchronizer = imageSynchronizers.get(networkPackets.getIp());
                    if (imageSynchronizer == null) {
                        return;
                    }
                    final int[][] image = imageSynchronizer.synchronize(patches);
                    final RImage rImage = new RImage(image, networkPackets.getIp());
                    final byte[] serializedImage = rImage.serialize();
                    // Do not wait for result
                    try {
                        rpc.call(Utils.UPDATE_UI, serializedImage).get();
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                }
                case NetworkPacketType.SUBSCRIBE_AS_VIEWER -> {
                    final String viewerIP = NetworkSerializer.deserializeIP(data);
                    addParticipant(viewerIP);
                }
                default -> {
                }
            }

        }
    }
}
