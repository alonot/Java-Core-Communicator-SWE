package com.swe.ScreenNVideo.IntegrationTest;

import com.swe.ScreenNVideo.MediaCaptureManager;
import com.swe.core.RPC;
import com.swe.networking.AbstractController;
import com.swe.networking.AbstractNetworking;
import com.swe.networking.ClientNode;
import com.swe.networking.Networking;
import com.swe.networking.SimpleNetworking.SimpleNetworking;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import static com.swe.ScreenNVideo.Utils.getSelfIP;

/**
 * Entry point for the screen and video integration test.
 * <p>
 * The {@code MainController} sets up dummy networking and RPC components,
 * initializes a {@link MediaCaptureManager}, and starts the screen capture process
 * in a separate thread for testing purposes.
 * </p>
 */
public class MainController {
    /**
     * Server port for ScreenNVideo.
     */
    static final int SERVERPORT = 40000;


    static void main(final String[] args) throws InterruptedException {
//        final SimpleNetworking networking = SimpleNetworking.getSimpleNetwork();
//        final AbstractNetworking networking = Networking.getNetwork();
//        final AbstractNetworking networking = new DummyNetworking();
        final AbstractNetworking networking = new DummyNetworkingWithQueue();

        // Get IP address as string
        final String ipAddress = getSelfIP();
        final ClientNode deviceNode = new ClientNode(ipAddress, SERVERPORT);
        final ClientNode serverNode = new ClientNode("10.32.1.250", SERVERPORT);

        final RPC rpc = new RPC();

        final MediaCaptureManager screenNVideo =
            new MediaCaptureManager(networking, rpc, SERVERPORT);
        System.out.println("Connection RPC..");


        Thread handler = null;
        try {
            handler = rpc.connect(6942);
        } catch (IOException | ExecutionException e) {
            throw new RuntimeException(e);
        }

//        SimpleNetworking.getSimpleNetwork().addUser(deviceNode, serverNode);

//        AbstractController networkingCom = Networking.getNetwork();
//        networkingCom.addUser(deviceNode, serverNode); // DummyNetworking doesn't need this

        screenNVideo.broadcastJoinMeeting();
        System.out.println("COnnected");

        final Thread screenNVideoThread = new Thread(() -> {
            try {
                screenNVideo.startCapture();
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        screenNVideoThread.start();

        // Start UI
//        Thread uiThread = new Thread(() -> {
//            Application.launch(VideoUI.class, args);
//        });

//        uiThread.join();
        screenNVideoThread.join();
        handler.join();

        // Cleanup
//        if (networking instanceof DummyNetworking) {
//            ((DummyNetworking) networking).shutdown();
//        }
    }
}
