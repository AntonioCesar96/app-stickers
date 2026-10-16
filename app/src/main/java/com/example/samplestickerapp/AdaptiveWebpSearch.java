package com.example.samplestickerapp;

import java.util.ArrayList;
import java.util.List;

/** Prefer quality/motion first, then reduce quality gradually and finally sharpness. */
final class AdaptiveWebpSearch {
    static final long MAX_BYTES = 500L * 1024;
    private static final int[] DETAIL_SIZES = {512, 480, 448, 416, 384, 352, 320, 288,
            256, 224, 192, 160, 128, 96, 64, 48, 32, 24, 16, 8, 4, 2, 1};
    private final int maxQuality;
    private final int minQuality;
    private final List<Integer> frameRates = new ArrayList<>();
    private int rateIndex;
    private int quality;
    private int low;
    private int high;
    private int bestQuality = -1;
    private boolean done;
    private boolean extraCompression;
    private int detailIndex;
    private final List<Integer> detailSizes = new ArrayList<>();

    AdaptiveWebpSearch(int maxQuality, int maxFps) {
        this(maxQuality, maxFps, 512);
    }

    AdaptiveWebpSearch(int maxQuality, int maxFps, int initialDetailSize) {
        if (maxQuality < 0 || maxQuality > 100 || maxFps < 1 || maxFps > 60) {
            throw new IllegalArgumentException("Qualidade: 0–100; FPS: 1–60.");
        }
        this.maxQuality = maxQuality;
        if (initialDetailSize < 1 || initialDetailSize > 512) throw new IllegalArgumentException("Nitidez: 1–512.");
        detailSizes.add(initialDetailSize);
        for (int size : DETAIL_SIZES) if (size < initialDetailSize) detailSizes.add(size);
        // The preferred floor is followed by an explicitly announced gradual fallback.
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
    boolean isExtraCompression() { return extraCompression; }
    int getDetailSize() { return detailSizes.get(detailIndex); }

    /** Returns true only when the caller should retain this encoded file. */
    boolean recordSize(long bytes) {
        if (done) throw new IllegalStateException("Busca concluída");
        boolean fits = bytes > 0 && bytes <= MAX_BYTES;
        boolean keep = fits && quality > bestQuality;
        if (keep) bestQuality = quality;
        if (extraCompression) {
            // Test every quality value: retain the first fitting result instead of dropping
            // several points or restarting the binary search below the preferred floor.
            if (fits) done = true;
            else continueBelowFloor();
            return keep;
        }
        if (fits && quality == maxQuality) {
            done = true;
        } else if (!fits && quality == minQuality) {
            if (++rateIndex == frameRates.size()) {
                rateIndex--;
                continueBelowFloor();
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

    private void continueBelowFloor() {
        extraCompression = true;
        if (quality > 0) {
            quality--;
        } else if (detailIndex + 1 < detailSizes.size()) {
            // Keep the FPS floor. Blur finer detail only after quality 0 was actually tried.
            detailIndex++;
        } else {
            // Valid codecs have finite ranges; never loop forever on an impossible result.
            done = true;
        }
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
