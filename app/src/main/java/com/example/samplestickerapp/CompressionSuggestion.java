package com.example.samplestickerapp;

/** Conservative heuristic for editable retry fields; never starts an encoder. */
final class CompressionSuggestion {
    static final long LIMIT = 500L * 1024;
    enum Mode {
        AGGRESSIVE(1.35), MODERATE(.85), GENTLE(.40);
        final double strength;
        Mode(double strength) { this.strength = strength; }
    }

    static CompressionSettings suggest(CompressionSettings measured, CompressionSettings current, long bytes, Mode mode) {
        if (bytes <= 0) throw new IllegalArgumentException("Aguarde uma tentativa concluída com tamanho válido.");
        if (bytes <= LIMIT) return current;
        // Use the settings tied to the measured bytes, bounded by reductions already in use.
        int quality = Math.min(measured.quality, current.quality);
        int fps = Math.min(measured.fps, current.fps);
        int detail = Math.min(measured.detailSize, current.detailSize);
        double excess = 1 - LIMIT / (double) bytes;
        double severity = Math.min(.85, excess * mode.strength);
        int nextQuality = Math.max(0, quality - (int) Math.ceil(2 * mode.strength + quality * .60 * severity));
        int nextFps = Math.max(Math.min(12, fps), fps - (int) Math.ceil(fps * .50 * severity));
        // Detail is an intermediate square: area scales roughly with the square of its side.
        int nextDetail = Math.max(1, (int) Math.floor(detail * Math.sqrt(1 - .70 * severity)));
        // Compression is effort, not quality: decreasing it would work against the size target.
        int compression = Math.min(6, Math.max(measured.compression, current.compression) + 1);
        return new CompressionSettings(nextQuality, nextFps, compression, current.speed, nextDetail);
    }
}
