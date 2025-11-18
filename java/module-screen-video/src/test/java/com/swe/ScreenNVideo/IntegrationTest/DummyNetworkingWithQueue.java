package com.swe.ScreenNVideo.IntegrationTest;

import com.swe.ScreenNVideo.Utils;
import com.swe.networking.ClientNode;
import com.swe.networking.ModuleType;
import com.swe.networking.AbstractNetworking;
import com.swe.networking.MessageListener;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Dummy networking for integration tests.
 * Simulates sending and receiving packets with headers.
 */
public class DummyNetworkingWithQueue implements AbstractNetworking {

    /** Simulated subscriptions. */
    private final Map<String, MessageListener> subscriptions = new ConcurrentHashMap<>();

    /** Queue for "network" pakcets. */
    private final BlockingQueue<byte[]> packetQueue = new LinkedBlockingQueue<>();

    /** Simulated self IP. */
    private final String selfIP = "127.0.0.1";

    /** Flag indicating whether the recieve loop should continue running. */
    private volatile boolean running = true;

    /** Number of bytes used to store the message length in the packet header. */
    private static final int HEADER_LENGTH_BYTES = 4;

    public DummyNetworkingWithQueue() {
        // Start a background thread to simulate receiving packets
        final Thread receiverThread = new Thread(this::receiveLoop, "DummyReceiverThread");
//        receiverThread.setDaemon(true);
        receiverThread.start();
    }

    public String getSelfIP() {
        return selfIP;
    }

    /**
     * Deprecated.
     * @param data data to send
     * @param dest dest
     * @param port port
     */
    public void sendData(final byte[] data, final String[] dest, final int[] port) {
        if (data == null) {
            return;
        }

        // Build header: 4 bytes for length
        final ByteBuffer buffer = ByteBuffer.allocate(4 + data.length);
        buffer.putInt(data.length);
        buffer.put(data);

        // Instead of sending over network, enqueue locally
        packetQueue.offer(buffer.array());
    }

    @Override
    public void subscribe(int name, MessageListener function) {
        subscriptions.put(Utils.MODULE_REMOTE_KEY, function);
    }

    @Override
    public void removeSubscription(int name) {

    }

    /**
     * Receiver loop: consumes queued packets, reconstructs payload,
     * and forwards to subscribers.
     */
    private void receiveLoop() {
        try {
            while (running) {
                final byte[] packet = packetQueue.take(); // blocks until available
                if (packet.length < HEADER_LENGTH_BYTES) {
                    continue;
                }

                final ByteBuffer buffer = ByteBuffer.wrap(packet);
                final int length = buffer.getInt();
                final byte[] payload = new byte[length];
                buffer.get(payload);

                // Send to "screen_share" subscription
                final MessageListener listener = subscriptions.get(Utils.MODULE_REMOTE_KEY);
                if (listener != null) {
                    listener.receiveData(payload);
                }
            }
        } catch (InterruptedException e) {
            System.out.println("Thread died");
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void sendData(byte[] data, ClientNode[] dest, int module, int priority) {
        if (data == null || dest == null) {
            return;
        }

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
        sendData(data, new String[0], new int[0]);
    }

}
