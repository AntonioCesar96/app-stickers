package com.example.samplestickerapp;

import java.util.ArrayList;
import java.util.List;

/** Bounded search: preserve motion, then find a quality near the size budget. */
final class AdaptiveWebpSearch {
    static final long MAX_BYTES = 500L * 1024;
    private final int maxQuality;
    private final int minQuality;
    private final List<Integer> frameRates = new ArrayList<>();
    private int rateIndex;
    private int quality;
    private int low;
    private int high;
    private int bestQuality = -1;
    private boolean done;

    AdaptiveWebpSearch(int maxQuality, int maxFps) {
        if (maxQuality < 0 || maxQuality > 100 || maxFps < 1 || maxFps > 60) {
            throw new IllegalArgumentException("Qualidade: 0–100; FPS: 1–60.");
        }
        this.maxQuality = maxQuality;
        // Respect an explicitly selected lower quality, but don't silently degrade to it.
        minQuality = Math.min(55, maxQuality);
        frameRates.add(maxFps);
        for (int fps : new int[]{48, 30, 24, 20, 18, 15, 12}) {
            if (fps < maxFps) frameRates.add(fps);
        }
        quality = maxQuality;
        low = minQuality;
        high = maxQuality;
    }

    int getQuality() { return quality; }
    int getFps() { return frameRates.get(rateIndex); }
    int getBestQuality() { return bestQuality; }
    boolean isDone() { return done; }
    boolean hasResult() { return bestQuality >= 0; }

    /** Returns true only when the caller should retain this encoded file. */
    boolean recordSize(long bytes) {
        if (done) throw new IllegalStateException("Busca concluída");
        boolean fits = bytes > 0 && bytes <= MAX_BYTES;
        boolean keep = fits && quality > bestQuality;
        if (keep) bestQuality = quality;
        if (fits && quality == maxQuality) {
            done = true;
        } else if (!fits && quality == minQuality) {
            if (++rateIndex == frameRates.size()) {
                rateIndex--;
                done = true;
            } else {
                // Size is only an estimate, so start one ladder step above the projected
                // fitting rate. Every accepted result still needs an actual encode.
                double estimatedFps = frameRates.get(rateIndex - 1)
                        * (MAX_BYTES / (double) Math.max(1, bytes));
                while (rateIndex + 1 < frameRates.size()
                        && frameRates.get(rateIndex + 1) >= estimatedFps) rateIndex++;
                quality = minQuality;
                low = minQuality;
                high = maxQuality;
            }
        } else if (!fits && quality == maxQuality && bestQuality < 0) {
            high = quality - 1;
            quality = minQuality;
        } else {
            if (fits) low = quality + 1;
            else high = quality - 1;
            if (low > high) done = true;
            else quality = low + (high - low) / 2;
        }
        return keep;
    }

    static int sourceLimitedFps(int requested, String sourceRate, double speed) {
        try {
            String[] parts = sourceRate.split("/");
            double rate = Double.parseDouble(parts[0]);
            if (parts.length == 2) rate /= Double.parseDouble(parts[1]);
            rate *= speed;
            if (Double.isFinite(rate) && rate > 0) {
                return Math.min(requested, Math.max(1, (int) Math.ceil(rate)));
            }
        } catch (RuntimeException ignored) {
            // Some containers don't provide a useful average frame rate.
        }
        return requested;
    }
}
