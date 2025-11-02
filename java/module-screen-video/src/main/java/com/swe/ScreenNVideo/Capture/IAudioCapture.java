package com.swe.ScreenNVideo.Capture;

public interface IAudioCapture {
    boolean init();
    byte[] getChunk();
    void stop();
    boolean reinit();
}
