/**
 * Contributed by Priyanshu Pandey.
 */

package com.swe.ScreenNVideo.IntegrationTest;

import com.swe.RPC.AbstractRPC;
import com.swe.ScreenNVideo.CaptureManager;
import com.swe.ScreenNVideo.MediaCaptureManager;
import com.swe.networking.ClientNode;
import com.swe.networking.SimpleNetworking.SimpleNetworking;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutionException;

/**
 * Entry point for the screen and video integration test.
 * <p>
 *     The {@code MainController} sets up dummy networking and RPC components,
 *     initializes a {@link MediaCaptureManager}, and starts the screen capture process
 *     in a separate thread for testing purposes.
 * </p>
 */
public class MainController {
    /**
     * Server port for ScreenNVideo.
     */
    static final int SERVERPORT = 40000;
    /**
     * Port where to ping to get self ip.
     */
    static final int PINGPORT = 10002;

    private static String getSelfIP() {
        // Get IP address as string
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.connect(InetAddress.getByName("8.8.8.8"), PINGPORT);
            return socket.getLocalAddress().getHostAddress();
        } catch (SocketException | UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    static void main(final String[] args) throws InterruptedException {
        final SimpleNetworking networking =  SimpleNetworking.getSimpleNetwork();

        // Get IP address as string
        final String ipAddress = getSelfIP();
        final ClientNode deviceNode = new ClientNode(ipAddress, SERVERPORT);
        final ClientNode serverNode = new ClientNode("10.32.11.242", SERVERPORT);

        final AbstractRPC rpc = new DummyRPC();

        final CaptureManager screenNVideo = new MediaCaptureManager(networking, rpc, SERVERPORT);

        networking.addUser(deviceNode, serverNode);
        Thread handler = null;
        try {
            handler = rpc.connect();
        } catch (IOException | ExecutionException e) {
            throw new RuntimeException(e);
        }

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
        networking.closeNetworking();
    }
}
