package com.example.samplestickerapp;

import org.junit.Test;
import static org.junit.Assert.*;

public class CompressionSuggestionTest {
    private final CompressionSettings base = new CompressionSettings(75, 30, 4, 1.25, 512);

    @Test public void modesHaveOrderedStrengthAndPreserveTiming() {
        CompressionSettings gentle = CompressionSuggestion.suggest(base, base, 1024000, CompressionSuggestion.Mode.GENTLE);
        CompressionSettings moderate = CompressionSuggestion.suggest(base, base, 1024000, CompressionSuggestion.Mode.MODERATE);
        CompressionSettings aggressive = CompressionSuggestion.suggest(base, base, 1024000, CompressionSuggestion.Mode.AGGRESSIVE);
        assertTrue(aggressive.quality < moderate.quality && moderate.quality < gentle.quality);
        assertTrue(aggressive.detailSize < moderate.detailSize && moderate.detailSize < gentle.detailSize);
        assertTrue(aggressive.fps <= moderate.fps && moderate.fps <= gentle.fps);
        assertEquals(base.speed, aggressive.speed, 0); assertEquals(5, aggressive.compression);
    }

    @Test public void largerOvershootProducesStrongerReduction() {
        CompressionSettings near = CompressionSuggestion.suggest(base, base, 520000, CompressionSuggestion.Mode.MODERATE);
        CompressionSettings far = CompressionSuggestion.suggest(base, base, 3000000, CompressionSuggestion.Mode.MODERATE);
        assertTrue(far.quality < near.quality); assertTrue(far.fps < near.fps); assertTrue(far.detailSize < near.detailSize);
    }

    @Test public void currentReductionsAreNeverRaisedAndLowFpsIsPreserved() {
        CompressionSettings current = new CompressionSettings(20, 8, 6, .75, 180);
        CompressionSettings result = CompressionSuggestion.suggest(base, current, 680000, CompressionSuggestion.Mode.AGGRESSIVE);
        assertTrue(result.quality <= 20); assertEquals(8, result.fps);
        assertTrue(result.detailSize <= 180); assertEquals(6, result.compression); assertEquals(.75, result.speed, 0);
    }

    @Test public void twelveFpsFloorAndAllParameterBoundsSurviveExtremeSize() {
        CompressionSettings floor = new CompressionSettings(0, 12, 6, 1, 1);
        CompressionSettings result = CompressionSuggestion.suggest(floor, floor, Long.MAX_VALUE, CompressionSuggestion.Mode.AGGRESSIVE);
        assertEquals(0, result.quality); assertEquals(12, result.fps); assertEquals(1, result.detailSize); assertEquals(6, result.compression);
    }

    @Test public void fittingOutputDoesNotDegradeAndSameEvidenceDoesNotCompound() {
        assertSame(base, CompressionSuggestion.suggest(base, base, CompressionSuggestion.LIMIT, CompressionSuggestion.Mode.AGGRESSIVE));
        CompressionSettings first = CompressionSuggestion.suggest(base, base, 680000, CompressionSuggestion.Mode.MODERATE);
        CompressionSettings again = CompressionSuggestion.suggest(base, base, 680000, CompressionSuggestion.Mode.MODERATE);
        assertEquals(first.quality, again.quality); assertEquals(first.detailSize, again.detailSize);
    }

    @Test public void noMeasurementCannotGenerateASuggestion() {
        try { CompressionSuggestion.suggest(base, base, 0, CompressionSuggestion.Mode.GENTLE); fail("No valid output"); }
        catch (IllegalArgumentException expected) { assertNotNull(expected.getMessage()); }
    }
}
