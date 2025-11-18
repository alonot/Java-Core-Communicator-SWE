package com.swe.ScreenNVideo.Model;

import com.swe.ScreenNVideo.PatchGenerator.CompressedPatch;

import java.util.List;

public record FeedPatch (List<CompressedPatch> compressedPatches, List<CompressedPatch> unCompressedPatches) {
}
