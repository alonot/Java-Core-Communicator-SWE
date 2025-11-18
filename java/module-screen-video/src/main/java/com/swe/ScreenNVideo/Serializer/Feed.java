package com.swe.ScreenNVideo.Serializer;

public record Feed (byte[] compressedFeed, byte[] unCompressedFeed) {
}
