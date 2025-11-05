package com.swe.ScreenNVideo.Capture;

import javax.sound.sampled.*;

/**
 * Threaded audio capture class.
 * Continuously captures PCM audio frames and queues them for consumers.
 */
@SuppressWarnings("checkstyle:SummaryJavadoc")
public class AudioCapture implements com.swe.ScreenNVideo.Capture.IAudioCapture, Runnable {

    private TargetDataLine microphone;
    private AudioFormat format;
    private boolean running = false;
    private Thread captureThread;

    // Recommended for speech-quality streaming (20 ms chunks at 48kHz mono)
    private static final float SAMPLE_RATE = 48000.0f;
    private static final int SAMPLE_SIZE = 16;
    private static final int CHANNELS = 1;
    private static final int FRAME_DURATION_MS = 20;
    private static final int BYTES_PER_FRAME = (int) (SAMPLE_RATE * FRAME_DURATION_MS / 1000 * (SAMPLE_SIZE / 8) * CHANNELS);

    // Queue for consumers (e.g., encoder, network sender)
//    private final BlockingQueue<byte[]> audioQueue = new LinkedBlockingQueue<>();
    private final com.swe.ScreenNVideo.Capture.AudioQueue audioQueue = new com.swe.ScreenNVideo.Capture.AudioQueue(50); // holds up to ~1 sec of audio (50 × 20ms)

    public AudioCapture() {
        this.format = new AudioFormat(SAMPLE_RATE, SAMPLE_SIZE, CHANNELS, true, false);
    }

    @Override
    public boolean init() {
        try {
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            microphone = (TargetDataLine) AudioSystem.getLine(info);
            microphone.open(format);
            microphone.start();
            running = true;

            captureThread = new Thread(this, "AudioCaptureThread");
            captureThread.start();

            System.out.println("Audio capture started at " + SAMPLE_RATE + " Hz");
            return true;
        } catch (LineUnavailableException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void run() {
        byte[] buffer = new byte[BYTES_PER_FRAME];
        while (running && microphone.isOpen()) {
            int bytesRead = microphone.read(buffer, 0, buffer.length);
            if (bytesRead > 0) {
                byte[] chunk = new byte[bytesRead];
                System.arraycopy(buffer, 0, chunk, 0, bytesRead);
                audioQueue.offer(chunk); // non-blocking insert
            }
        }
    }

    @Override
    public byte[] getChunk() {
        return audioQueue.poll(); // returns null if empty (non-blocking)
    }

    /**
     * If your main thread wants to wait until audio is available instead of polling
     */
    public byte[] getChunkBlocking() {
        try {
            return audioQueue.take();
        } catch (InterruptedException e) {
            return null;
        }
    }

    @Override
    public void stop() {
        running = false;
        try {
            if (microphone != null) {
                microphone.stop();
                microphone.close();
            }
            if (captureThread != null) {
                captureThread.join();
            }
            System.out.println("Audio capture stopped.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean reinit() {
        stop();
        return init();
    }
}
