package com.example.samplestickerapp;

import org.junit.Test;
import static org.junit.Assert.*;

public class CompressionProgressTest {
    @Test public void usesFrameTimelineWhenWebpMuxerReportsZeroTime() {
        CompressionProgress progress = new CompressionProgress(4, 10);
        progress.statistics(0);
        assertEquals(0, progress.get());
        progress.frameLog("[Parsed_showinfo_8 @ 0x1] n: 19 pts: 19 pts_time:1.9 pos: -1");
        assertEquals(50, progress.get());
        progress.statistics(0);
        assertEquals(50, progress.get());
    }

    @Test public void reservesCompletionForEncoderAndResetsForNextAttempt() {
        CompressionProgress progress = new CompressionProgress(2, 10);
        progress.frameLog("[Parsed_showinfo_8] pts_time:1.9");
        assertEquals(99, progress.get());
        progress.statistics(3000);
        assertEquals(99, progress.get());
        assertEquals(0, new CompressionProgress(2, 10).get());
    }

    @Test public void ignoresInvalidLogsAndOutOfOrderStats() {
        CompressionProgress progress = new CompressionProgress(5, 30);
        progress.statistics(2500);
        progress.statistics(1000);
        progress.statistics(Double.NaN);
        progress.frameLog(null);
        progress.frameLog("pts_time:4.0");
        progress.frameLog("[showinfo] pts_time:--");
        assertEquals(50, progress.get());
        assertThrows(IllegalArgumentException.class, () -> new CompressionProgress(0, 30));
    }
}
