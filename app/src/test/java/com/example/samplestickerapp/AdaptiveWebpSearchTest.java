package com.example.samplestickerapp;

import org.junit.Test;
import static org.junit.Assert.*;

public class AdaptiveWebpSearchTest {
    @Test public void keepsFullQualityWhenItFits() {
        AdaptiveWebpSearch search = new AdaptiveWebpSearch(75, 30);
        assertTrue(search.recordSize(512000));
        assertTrue(search.isDone());
        assertEquals(75, search.getBestQuality());
        assertEquals(30, search.getFps());
    }

    @Test public void refinesQualityWithoutSacrificingFrameRate() {
        AdaptiveWebpSearch search = new AdaptiveWebpSearch(75, 30);
        int retained = -1;
        int attempts = 0;
        while (!search.isDone()) {
            int quality = search.getQuality();
            if (search.recordSize(quality <= 68 ? 500000 : 600000)) retained = quality;
            assertTrue(++attempts < 12);
        }
        assertEquals(30, search.getFps());
        assertEquals(68, retained);
    }

    @Test public void lowersFpsOnlyWhenQualityFloorDoesNotFit() {
        AdaptiveWebpSearch search = new AdaptiveWebpSearch(75, 30);
        while (!search.isDone()) {
            search.recordSize(search.getFps() <= 24 && search.getQuality() <= 70 ? 500000 : 600000);
        }
        assertEquals(24, search.getFps());
        assertEquals(70, search.getBestQuality());
    }

    @Test public void impossibleAndEmptyOutputsTerminateWithoutAResult() {
        for (long size : new long[]{0, 512001, Long.MAX_VALUE}) {
            AdaptiveWebpSearch search = new AdaptiveWebpSearch(100, 60);
            int attempts = 0;
            while (!search.isDone()) {
                assertFalse(search.recordSize(size));
                assertTrue(++attempts <= 16);
            }
            assertFalse(search.hasResult());
            assertEquals(12, search.getFps());
        }
    }

    @Test public void supportsExplicitLowQualityAndLowSourceFps() {
        AdaptiveWebpSearch search = new AdaptiveWebpSearch(30, 8);
        search.recordSize(600000);
        assertTrue(search.isDone());
        assertFalse(search.hasResult());
        assertEquals(8, search.getFps());
    }

    @Test public void sourceRateAccountsForPlaybackSpeed() {
        assertEquals(24, AdaptiveWebpSearch.sourceLimitedFps(30, "24000/1001", 1));
        assertEquals(12, AdaptiveWebpSearch.sourceLimitedFps(30, "24/1", .5));
        assertEquals(30, AdaptiveWebpSearch.sourceLimitedFps(30, "0/0", 1));
        assertEquals(30, AdaptiveWebpSearch.sourceLimitedFps(30, null, 1));
    }

    @Test public void independentQuadrantsDoNotShareSearchState() {
        AdaptiveWebpSearch first = new AdaptiveWebpSearch(75, 30);
        AdaptiveWebpSearch second = new AdaptiveWebpSearch(75, 30);
        first.recordSize(600000);
        assertEquals(55, first.getQuality());
        assertEquals(75, second.getQuality());
        second.recordSize(400000);
        assertTrue(second.hasResult());
        assertFalse(first.hasResult());
    }

    @Test public void laterOversizeAttemptDoesNotDiscardValidResult() {
        AdaptiveWebpSearch search = new AdaptiveWebpSearch(75, 30);
        search.recordSize(600000);
        assertTrue(search.recordSize(450000));
        while (!search.isDone()) assertFalse(search.recordSize(600000));
        assertTrue(search.hasResult());
        assertEquals(55, search.getBestQuality());
    }
}
