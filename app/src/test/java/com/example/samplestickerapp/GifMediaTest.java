package com.example.samplestickerapp;

import org.junit.Test;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import static org.junit.Assert.*;

public class GifMediaTest {
    @Test public void recognizesUppercaseAndSpacesWithoutMisclassifyingOtherFiles() {
        assertTrue(GifMedia.isGif(new File("a funny animation.GIF")));
        assertFalse(GifMedia.isGif(new File("animation.gif.mp4")));
    }

    @Test public void timelineIsLocaleIndependentAndPreservesHeldFrameBeforeTrim() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(new Locale("pt", "BR"));
            String filter = GifMedia.timelineFilter(250, 1250, 1.5, 24);
            assertTrue(filter.contains("start=0.250:end=1.250"));
            assertTrue(filter.indexOf("fps=100") < filter.indexOf("trim="));
            assertTrue(filter.contains("/1.50000000"));
        } finally { Locale.setDefault(previous); }
    }

    @Test public void originalGifIsTheExportInputAndLoopsAreIgnored() {
        File source = new File("original animation.GIF");
        List<String> args = Arrays.asList(GifMedia.webpArguments(source, true, "format=rgba", 75, 6, new File("out.webp")));
        assertEquals(source.getAbsolutePath(), args.get(args.indexOf("-i") + 1));
        assertEquals("1", args.get(args.indexOf("-ignore_loop") + 1));
        assertFalse(Arrays.asList(GifMedia.webpArguments(new File("video.mp4"), false,
                "format=rgba", 75, 6, new File("out.webp"))).contains("-ignore_loop"));
    }

    @Test public void invalidRangesAndSpeedsAreRejected() {
        for (double speed : new double[]{0, -1, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class, () -> GifMedia.timelineFilter(0, 1000, speed, 24));
        }
        assertThrows(IllegalArgumentException.class, () -> GifMedia.timelineFilter(500, 500, 1, 24));
        assertThrows(IllegalArgumentException.class, () -> GifMedia.timelineFilter(-1, 500, 1, 24));
    }
}
