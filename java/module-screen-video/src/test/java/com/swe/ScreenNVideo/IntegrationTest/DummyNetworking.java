package com.swe.ScreenNVideo.IntegrationTest;

import com.swe.ScreenNVideo.Utils;
import com.swe.networking.ClientNode;
import com.swe.networking.ModuleType;
import com.swe.networking.AbstractNetworking;
import com.swe.networking.MessageListener;
import com.swe.networking.SimpleNetworking.SimpleNetworking;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.spec.RSAOtherPrimeInfo;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.swe.ScreenNVideo.IntegrationTest.MainController.SERVERPORT;
import static com.swe.ScreenNVideo.Utils.getSelfIP;

/**
 * Simple dummy networking implementation using TCP sockets.
 * Sends complete data over the network without chunking.
 */
public class DummyNetworking implements AbstractNetworking {

    /** Simulated subscriptions. */
    private final Map<String, MessageListener> subscriptions = new ConcurrentHashMap<>();

    /** Port for listening to incoming connections. */
    private final int listenPort;

    /** Self IP address. */
    private final String selfIP;

    /** Server socket for receiving data. */
    private ServerSocket serverSocket;

    /** Flag indicating whether the receive loop should continue running. */
    private volatile boolean running = true;

    /**
     * Constructor with specified port.
     * @param port Port to listen on
     */
    public DummyNetworking(int port) {
        this.listenPort = port;
        try {
            this.selfIP = InetAddress.getLocalHost().getHostAddress();
            startReceiver();
        } catch (IOException e) {
            throw new RuntimeException("Failed to initialize DummyNetworking", e);
        }
    }

    SimpleNetworking networking;

    /**
     * Constructor with default port 9999.
     */
    public DummyNetworking() {
        this(SERVERPORT);
    }

    public String getSelfIP() {
        return selfIP;
    }

    /**
     * Start the receiver thread.
     */
    private void startReceiver() throws IOException {
        serverSocket = new ServerSocket(listenPort);
        Thread receiverThread = new Thread(this::receiveLoop, "DummyNetworkingReceiver");
        receiverThread.start();
    }

    /**
     * Send data to destination IP addresses.
     * @param data Data to send
     * @param dest Destination IP addresses
     * @param port Destination ports
     */
    public void sendData(byte[] data, String[] dest, int[] port) {
        if (data == null || dest == null || port == null) {
            return;
        }

        for (int i = 0; i < dest.length && i < port.length; i++) {
            try (Socket socket = new Socket(dest[i], port[i]);
                 DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {

                // Send length first, then data
                out.writeInt(data.length);
                out.write(data);
                out.flush();

            } catch (IOException e) {
                System.err.println("Failed to send data to " + dest[i] + ":" + port[i] + " - " + e.getMessage());
            }
        }
    }


    public void removeSubscription(ModuleType name) {
        subscriptions.remove(Utils.MODULE_REMOTE_KEY);
    }

    /**
     * Receiver loop: accepts incoming connections and reads complete data.
     */
    private void receiveLoop() {
        while (running) {
            try {
                // Accept incoming connection
                Socket clientSocket = serverSocket.accept();

                // Handle each connection in a separate thread
                handleConnection(clientSocket);

            } catch (IOException e) {
                if (running) {
                    System.err.println("Error accepting connection: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Handle a single connection and read complete data.
     */
    private void handleConnection(Socket socket) {
        try (DataInputStream in = new DataInputStream(socket.getInputStream())) {

            // Read length
            int length = in.readInt();

            // Read complete data
            byte[] data = new byte[length];
            in.readFully(data);

            // Notify subscribers
            MessageListener listener = subscriptions.get(Utils.MODULE_REMOTE_KEY);
            if (listener != null) {
                listener.receiveData(data);
            }

        } catch (IOException e) {
            System.err.println("Error reading data: " + e.getMessage());
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    /**
     * Shutdown the networking.
     */
    public void shutdown() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing server socket: " + e.getMessage());
        }
    }

    @Override
    public void sendData(byte[] data, ClientNode[] dest, int module, int priority) {
//        if (networking == null) {
//            networking = SimpleNetworking.getSimpleNetwork();
//        }
        if (data == null || dest == null) {
            return;
        }

//        networking.sendData(data, dest, ModuleType.SCREENSHARING, priority);

        String[] ips = new String[dest.length];
        int[] ports = new int[dest.length];

        for (int i = 0; i < dest.length; i++) {
            ips[i] = dest[i].hostName();
            ports[i] = dest[i].port(); // Use same port for all destinations
        }

        sendData(data, ips, ports);
    }

    @Override
    public void broadcast(byte[] data, int module, int priority) {
        sendData(data, new ClientNode[]{new ClientNode("10.32.1.250", 40000)},module,priority);
    }

    @Override
    public void subscribe(int name, MessageListener function) {
        subscriptions.put(Utils.MODULE_REMOTE_KEY, function);
    }

    @Override
    public void removeSubscription(int name) {

    }
}