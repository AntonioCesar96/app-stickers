package com.example.samplestickerapp;

/** Validated starting point for an automatic compression search. */
final class CompressionSettings {
    final int quality, fps, compression, detailSize;
    final double speed;

    CompressionSettings(int quality, int fps, int compression, double speed, int detailSize) {
        if (quality < 0 || quality > 100) throw new IllegalArgumentException("Qualidade: 0 a 100.");
        if (fps < 1 || fps > 60) throw new IllegalArgumentException("FPS: 1 a 60.");
        if (compression < 0 || compression > 6) throw new IllegalArgumentException("Compressão: 0 a 6.");
        if (!Double.isFinite(speed) || speed <= 0) throw new IllegalArgumentException("Velocidade deve ser maior que zero.");
        if (detailSize < 1 || detailSize > 512) throw new IllegalArgumentException("Nitidez: 1 a 512.");
        this.quality = quality; this.fps = fps; this.compression = compression;
        this.speed = speed; this.detailSize = detailSize;
    }

    void validateDuration(double sourceSeconds) {
        double seconds = sourceSeconds / speed;
        if (!Double.isFinite(seconds) || seconds <= 0 || seconds > 10)
            throw new IllegalArgumentException("A velocidade escolhida deve produzir um trecho de até 10 segundos.");
    }
}
