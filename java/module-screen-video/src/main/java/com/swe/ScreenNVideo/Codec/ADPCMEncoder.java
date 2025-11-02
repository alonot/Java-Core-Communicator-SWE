package com.swe.ScreenNVideo.Codec;

public class ADPCMEncoder {
    // IMA ADPCM step size table
    private static final int[] STEP_TABLE = {
        7, 8, 9, 10, 11, 12, 13, 14, 16, 17,
        19, 21, 23, 25, 28, 31, 34, 37, 41, 45,
        50, 55, 60, 66, 73, 80, 88, 97, 107, 118,
        130, 143, 157, 173, 190, 209, 230, 253, 279, 307,
        337, 371, 408, 449, 494, 544, 598, 658, 724, 796,
        876, 963, 1060, 1166, 1282, 1411, 1552, 1707, 1878, 2066,
        2272, 2499, 2749, 3024, 3327, 3660, 4026, 4428, 4871, 5358,
        5894, 6484, 7132, 7845, 8630, 9493, 10442, 11487, 12635, 13899,
        15289, 16818, 18500, 20350, 22385, 24623, 27086, 29794, 32767
    };

    // IMA ADPCM index adjustment table
    private static final int[] INDEX_TABLE = {
        -1, -1, -1, -1, 2, 4, 6, 8,
        -1, -1, -1, -1, 2, 4, 6, 8
    };

    private int predictor = 0;
    private int index = 0;

    /**
     * Encodes 16-bit PCM bytes into 4-bit ADPCM data.
     * Each pair of ADPCM codes is packed into one byte.
     */
    public byte[] encode(byte[] pcmBytes) {
        int numSamples = pcmBytes.length / 2;
        byte[] adpcmBytes = new byte[(numSamples + 1) / 2];

        int step = STEP_TABLE[index];
        int bufferIndex = 0;
        boolean highNibble = true;
        byte currentByte = 0;

        for (int i = 0; i < numSamples; i++) {
            int lo = pcmBytes[2 * i] & 0xFF;
            int hi = pcmBytes[2 * i + 1] << 8;
            int sample = (short) (hi | lo);

            int diff = sample - predictor;
            int sign = (diff < 0) ? 8 : 0;
            if (sign != 0) diff = -diff;

            int delta = 0;
            int tempStep = step;
            if (diff >= tempStep) { delta = 4; diff -= tempStep; }
            tempStep >>= 1;
            if (diff >= tempStep) { delta |= 2; diff -= tempStep; }
            tempStep >>= 1;
            if (diff >= tempStep) delta |= 1;

            int code = delta | sign;

            // Reconstruct predictor
            int diffq = step >> 3;
            if ((delta & 4) != 0) diffq += step;
            if ((delta & 2) != 0) diffq += step >> 1;
            if ((delta & 1) != 0) diffq += step >> 2;
            if (sign != 0)
                predictor -= diffq;
            else
                predictor += diffq;

            // Clamp predictor
            if (predictor > 32767) predictor = 32767;
            else if (predictor < -32768) predictor = -32768;

            // Update step index
            index += INDEX_TABLE[code];
            if (index < 0) index = 0;
            else if (index > 88) index = 88;
            step = STEP_TABLE[index];

            // Pack two 4-bit codes into one byte
            if (highNibble) {
                currentByte = (byte) (code << 4);
                highNibble = false;
            } else {
                currentByte |= (byte) (code & 0x0F);
                adpcmBytes[bufferIndex++] = currentByte;
                highNibble = true;
            }
        }

        // if odd number of samples
        if (!highNibble) {
            adpcmBytes[bufferIndex] = currentByte;
        }

        return adpcmBytes;
    }
}
