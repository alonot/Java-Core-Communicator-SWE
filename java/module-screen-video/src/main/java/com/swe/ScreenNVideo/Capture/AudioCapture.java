package com.swe.ScreenNVideo.Capture;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Threaded audio capture class.
 * Continuously captures PCM audio frames and queues them for consumers.
 */
public class AudioCapture implements com.swe.ScreenNVideo.Capture.IAudioCapture, Runnable {

    /** The microphone input line capturing raw PCM audio. */
    private TargetDataLine microphone;

    /** The audio format specifying sample rate, size, and channel configuration. */
    private AudioFormat format;

    /** Indicates whether the capture thread is actively recording. */
    private boolean running = false;

    /** The background thread that continuously captures audio. */
    private Thread captureThread;

    /** Sample rate in Hz for audio capture (48 kHz). */
    private static final float SAMPLE_RATE = 48000.0f;

    /** Bit depth per audio sample (16-bit PCM). */
    private static final int SAMPLE_SIZE = 16;

    /** Number of audio channels (1 for mono). */
    private static final int CHANNELS = 1;

    /** Duration of each captured audio frame in milliseconds. */
    private static final int FRAME_DURATION_MS = 20;

    /** Bytes per captured frame based on sample rate, duration, and format. */
    private static final int BYTES_PER_FRAME =
            (int) (SAMPLE_RATE * FRAME_DURATION_MS / 1000 * (SAMPLE_SIZE / 8) * CHANNELS);

    /** Queue storing captured audio chunks for downstream consumers (e.g., encoder, sender). */
    private final BlockingQueue<byte[]> audioQueue = new LinkedBlockingQueue<>(50);

    /**
     * Constructs an AudioCapture instance with the default format.
     */
    public AudioCapture() {
        this.format = new AudioFormat(SAMPLE_RATE, SAMPLE_SIZE, CHANNELS, true, false);
    }

    @Override
    public boolean init() {
        try {
            final DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
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
        final byte[] buffer = new byte[BYTES_PER_FRAME];
        while (running && microphone.isOpen()) {
            final int bytesRead = microphone.read(buffer, 0, buffer.length);
            if (bytesRead > 0) {
                final byte[] chunk = new byte[bytesRead];
                System.arraycopy(buffer, 0, chunk, 0, bytesRead);
                audioQueue.offer(chunk); // non-blocking insert
            }
        }
        System.out.println("AudioCapture thread stopped. Total chunks captured: ");
    }

    @Override
    public byte[] getChunk() {
        return audioQueue.poll(); // returns null if empty (non-blocking)
    }

    /**
     * Returns the next available audio chunk, blocking if none are available.
     *
     * @return a byte array containing the captured audio data, or null if interrupted
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
