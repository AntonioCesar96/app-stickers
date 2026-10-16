package com.example.samplestickerapp;

import org.junit.Test;
import static org.junit.Assert.*;

public class CompressionSettingsTest {
    @Test public void rejectsInvalidOverridesBeforeInterruptingEncoder() {
        assertThrows(IllegalArgumentException.class, () -> new CompressionSettings(-1, 12, 6, 1, 512));
        assertThrows(IllegalArgumentException.class, () -> new CompressionSettings(101, 12, 6, 1, 512));
        assertThrows(IllegalArgumentException.class, () -> new CompressionSettings(35, 0, 6, 1, 512));
        assertThrows(IllegalArgumentException.class, () -> new CompressionSettings(35, 61, 6, 1, 512));
        assertThrows(IllegalArgumentException.class, () -> new CompressionSettings(35, 12, 7, 1, 512));
        assertThrows(IllegalArgumentException.class, () -> new CompressionSettings(35, 12, 6, Double.NaN, 512));
        assertThrows(IllegalArgumentException.class, () -> new CompressionSettings(35, 12, 6, 0, 512));
        assertThrows(IllegalArgumentException.class, () -> new CompressionSettings(35, 12, 6, 1, 0));
    }

    @Test public void speedMustRespectAnimatedStickerDuration() {
        new CompressionSettings(35, 12, 6, 1, 512).validateDuration(10);
        assertThrows(IllegalArgumentException.class, () ->
                new CompressionSettings(35, 12, 6, .5, 512).validateDuration(6));
    }

    @Test public void customStartingSharpnessIsNeverRaisedByFallback() {
        AdaptiveWebpSearch search = new AdaptiveWebpSearch(2, 8, 200);
        for (int quality = 2; quality >= 0; quality--) {
            assertEquals(quality, search.getQuality());
            assertEquals(200, search.getDetailSize());
            search.recordSize(600000);
        }
        assertEquals(192, search.getDetailSize());
        assertEquals(8, search.getFps());
        assertTrue(search.recordSize(400000));
        assertTrue(search.isDone());
    }
}
