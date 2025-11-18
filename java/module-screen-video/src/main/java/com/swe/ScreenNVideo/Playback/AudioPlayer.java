package com.swe.ScreenNVideo.Playback;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

/**
 * Plays raw PCM audio bytes through the system audio output.
 */
public class AudioPlayer {

    /** Sample rate in Hz (must match the decoded PCM data).*/
    private final float sampleRate;

    /** Number of channels (1 for mono, 2 for stereo).*/
    private final int channels;

    /** Bit depth per sample (16-bit PCM).*/
    private final int sampleSize;

    /**
     * The audio output line used to send PCM bytes to the system speakers.
     */
    private SourceDataLine line;

    public AudioPlayer(final float argSampleRate, final int argChannels, final int argSampleSize) {
        this.sampleRate = argSampleRate;
        this.channels = argChannels;
        this.sampleSize = argSampleSize;
    }

    /**
     * Initializes the audio output line.
     */
    public void init() throws LineUnavailableException {
        final AudioFormat format = new AudioFormat(
                sampleRate,
                sampleSize,
                channels,
                true,   // signed
                false   // little-endian
        );

        final DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        line = (SourceDataLine) AudioSystem.getLine(info);
        line.open(format);
        line.start();
    }

    /**
     * Plays PCM audio bytes.
     *
     * @param pcmBytes decoded PCM bytes (16-bit, little-endian)
     */

    public void play(final byte[] pcmBytes) {
        if (line != null) {
            line.write(pcmBytes, 0, pcmBytes.length);
        }
    }

    /**
     * Stops playback and releases resources.
     */
    public void stop() {
        if (line != null) {
            line.drain();
            line.stop();
            line.close();
        }
    }
}
