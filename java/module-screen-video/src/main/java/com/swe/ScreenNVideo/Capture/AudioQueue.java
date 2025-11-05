package com.swe.ScreenNVideo.Capture;

import java.util.LinkedList;

public class AudioQueue {
    private final LinkedList<byte[]> queue = new LinkedList<>();
    private final int maxSize;

    public AudioQueue(int maxSize) {
        this.maxSize = maxSize;
    }

    /** Inserts an element if space is available; otherwise discards it (non-blocking) */
    public synchronized void offer(byte[] data) {
        if (queue.size() < maxSize) {
            queue.addLast(data);
            notify(); // wake up waiting consumer
        } else {
            // queue full — discard oldest or skip new one (choose policy)
            // Example: discard oldest to reduce latency
            queue.removeFirst();
            queue.addLast(data);
        }
    }

    /** Retrieves and removes the head of the queue, or returns null if empty */
    public synchronized byte[] poll() {
        if (queue.isEmpty()) return null;
        return queue.removeFirst();
    }

    /** Retrieves and removes the head of the queue, waiting if necessary (blocking version) */
    public synchronized byte[] take() throws InterruptedException {
        while (queue.isEmpty()) {
            wait(); // wait until producer adds something
        }
        return queue.removeFirst();
    }

    /** Returns current queue size (for monitoring/debugging) */
    public synchronized int size() {
        return queue.size();
    }
}
