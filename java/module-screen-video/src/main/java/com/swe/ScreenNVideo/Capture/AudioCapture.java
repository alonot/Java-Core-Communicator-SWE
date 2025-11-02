package com.swe.ScreenNVideo.Capture;

import javax.sound.sampled.*;
import java.io.IOException;

/**
 * AudioCapture class that captures raw PCM audio from the microphone.
 * - Initialize once using init()
 * - Use getNextChunk() to read the next chunk of audio data
 * - Stop() to release microphone resources
 */
public class AudioCapture implements com.swe.ScreenNVideo.Capture.IAudioCapture {
    private TargetDataLine microphone;
    private AudioFormat format;
    private boolean initialized = false;
    private final int bufferSize = 960;

    public AudioCapture() {
        // 44.1 kHz, 16-bit, mono, signed, little endian
        this.format = new AudioFormat(44100.0f, 16, 1, true, false);
    }

    /** Initializes and starts the microphone for capture */
    @Override
    public boolean init() {
        if (initialized) return true; // already initialized

        try {
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            microphone = (TargetDataLine) AudioSystem.getLine(info);
            microphone.open(format);
            microphone.start();
            initialized = true;
            return true;
        } catch (LineUnavailableException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Reads and returns the next chunk of audio data.
     * Returns null if not initialized or error occurs.
     */
    @Override
    public byte[] getChunk() {
        if (!initialized || microphone == null) {
            System.err.println("AudioCapture not initialized!");
            return null;
        }

        byte[] buffer = new byte[bufferSize];
        int bytesRead = microphone.read(buffer, 0, buffer.length);
        if (bytesRead > 0) {
            // Return exact size chunk
            byte[] chunk = new byte[bytesRead];
            System.arraycopy(buffer, 0, chunk, 0, bytesRead);
            return chunk;
        }
        return null;
    }

    /** Stops and closes the microphone */
    @Override
    public void stop() {
        if (!initialized) return;

        try {
            microphone.stop();
            microphone.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            initialized = false;
        }
    }

    /** Reinitializes audio capture (useful if device reset or stream restarted) */
    @Override
    public boolean reinit() {
        stop();
        return init();
    }
}
