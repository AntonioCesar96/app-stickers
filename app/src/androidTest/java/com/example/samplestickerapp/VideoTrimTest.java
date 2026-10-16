package com.example.samplestickerapp;

import android.content.Context;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import androidx.test.platform.app.InstrumentationRegistry;
import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.ReturnCode;
import com.arthenica.ffmpegkit.Session;
import org.junit.Test;
import java.io.File;
import static org.junit.Assert.*;

public class VideoTrimTest {
    private void execute(String[] args) {
        Session session = FFmpegKit.executeWithArguments(args);
        assertTrue(session.getAllLogsAsString(), ReturnCode.isSuccess(session.getReturnCode()));
    }

    @Test public void cutBetweenKeyframesHasZeroTimelineNoPrerollAndNoAudio() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        File input = new File(context.getCacheDir(), "trim long gop input.mp4");
        File output = new File(context.getCacheDir(), "trim zero timeline.mp4");
        try {
            execute(new String[]{"-y", "-f", "lavfi", "-i", "testsrc2=s=240x432:r=30:d=12",
                    "-f", "lavfi", "-i", "sine=frequency=440:duration=12", "-c:v", "libx264",
                    "-preset", "ultrafast", "-g", "300", "-keyint_min", "300", "-sc_threshold", "0",
                    "-c:a", "aac", "-shortest", input.getAbsolutePath()});
            execute(VideoTrim.arguments(input, output, 3700, 9700));
            MediaExtractor extractor = new MediaExtractor();
            try {
                extractor.setDataSource(output.getAbsolutePath());
                assertEquals("Audio must be removed", 1, extractor.getTrackCount());
                MediaFormat format = extractor.getTrackFormat(0);
                assertEquals("video/avc", format.getString(MediaFormat.KEY_MIME));
                assertEquals(240, format.getInteger(MediaFormat.KEY_WIDTH));
                assertEquals(432, format.getInteger(MediaFormat.KEY_HEIGHT));
                assertEquals(6000000L, format.getLong(MediaFormat.KEY_DURATION), 34000L);
                extractor.selectTrack(0);
                assertEquals("First displayed sample starts at zero", 0L, extractor.getSampleTime());
                int count = 0;
                long previous = -1;
                do {
                    long time = extractor.getSampleTime();
                    assertTrue("No negative or backward sample times", time > previous);
                    previous = time;
                    count++;
                } while (extractor.advance());
                assertEquals("Six seconds at original 30 FPS; no keyframe preroll", 180, count);
                extractor.seekTo(0, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
                assertEquals("Loop seek must restart at zero", 0L, extractor.getSampleTime());
            } finally { extractor.release(); }
        } finally { input.delete(); output.delete(); }
    }
}
