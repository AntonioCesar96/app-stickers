package com.example.samplestickerapp;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Progress measured from output-frame timestamps, never from elapsed wall-clock time. */
final class CompressionProgress {
    private static final Pattern FRAME_TIME = Pattern.compile("pts_time:([+\\-0-9.eE]+)");
    private final double durationSeconds;
    private final int fps;
    private final AtomicInteger percent = new AtomicInteger();

    CompressionProgress(double durationSeconds, int fps) {
        if (!Double.isFinite(durationSeconds) || durationSeconds <= 0 || fps < 1)
            throw new IllegalArgumentException("Duração e FPS devem ser positivos.");
        this.durationSeconds = durationSeconds;
        this.fps = fps;
    }

    void frameLog(String message) {
        if (message == null || !message.contains("showinfo")) return;
        Matcher matcher = FRAME_TIME.matcher(message);
        if (matcher.find()) {
            try { updateSeconds(Double.parseDouble(matcher.group(1)) + 1.0 / fps); }
            catch (NumberFormatException ignored) { }
        }
    }

    void statistics(double milliseconds) { updateSeconds(milliseconds / 1000.0); }

    private void updateSeconds(double seconds) {
        if (!Double.isFinite(seconds) || seconds < 0) return;
        // The encoder may still be finalizing the file after the last filtered frame.
        int measured = (int) Math.min(99, Math.floor(seconds * 100 / durationSeconds));
        percent.accumulateAndGet(measured, Math::max);
    }

    int get() { return percent.get(); }
}
