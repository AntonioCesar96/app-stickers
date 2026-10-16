package com.example.samplestickerapp;

import java.io.File;
import java.util.Locale;

/** Frame-accurate, silent preview with a fresh timeline for native looping playback. */
final class VideoTrim {
    private VideoTrim() {}

    static String[] arguments(File input, File output, long startMs, long endMs) {
        if (startMs < 0 || endMs <= startMs) throw new IllegalArgumentException("Intervalo inválido.");
        return new String[]{"-y", "-ss", seconds(startMs), "-i", input.getAbsolutePath(),
                "-t", seconds(endMs - startMs), "-map", "0:v:0", "-an", "-sn", "-dn",
                "-vf", "setpts=PTS-STARTPTS,pad=ceil(iw/2)*2:ceil(ih/2)*2",
                "-c:v", "libx264", "-preset", "veryfast", "-crf", "18", "-bf", "0",
                "-pix_fmt", "yuv420p", "-movflags", "+faststart", output.getAbsolutePath()};
    }

    private static String seconds(long ms) { return String.format(Locale.US, "%.3f", ms / 1000.0); }
}
