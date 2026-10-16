package com.example.samplestickerapp;

import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.FFmpegKitConfig;
import com.arthenica.ffmpegkit.Level;
import com.arthenica.ffmpegkit.ReturnCode;
import com.arthenica.ffmpegkit.Session;
import com.facebook.animated.webp.WebPImage;
import com.facebook.imagepipeline.common.ImageDecodeOptions;
import org.junit.Test;
import java.io.*;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

/** Optional real-device comparison for the 720x484 sample, never bundled in the APK. */
public class WebpEncodingTest {
    @Test public void compareProvidedVideoOnBundledEncoder() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        File dir = new File(context.getFilesDir(), "webp-benchmark");
        File source = new File(dir, "input.mp4");
        assumeTrue("Provide the sample under internal files/webp-benchmark/input.mp4", source.exists());
        // Explicit opt-in allows resuming expensive failed-size trials for an unchanged sample.
        Map<String, Long> previous = new HashMap<>();
        if ("true".equals(InstrumentationRegistry.getArguments().getString("reuseOversizeTrials"))) {
            Pattern pattern = Pattern.compile("new attempt=\\d+ fps=(\\d+) quality=(\\d+) bytes=(\\d+)");
            try (BufferedReader reader = new BufferedReader(new FileReader(new File(dir, "previous-comparison.txt")))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    Matcher match = pattern.matcher(line);
                    if (match.find() && Long.parseLong(match.group(3)) > AdaptiveWebpSearch.MAX_BYTES)
                        previous.put(match.group(1) + ":" + match.group(2), Long.parseLong(match.group(3)));
                }
            }
        }
        File log = new File(dir, "comparison.txt");
        try (PrintWriter report = new PrintWriter(new OutputStreamWriter(new FileOutputStream(log), "UTF-8"))) {
            File old = new File(dir, "old.webp");
            int fps = 30, quality = 75, attempt = 0;
            do {
                encode(source, old, fps, quality, true);
                attempt++;
                report.printf(Locale.US, "old attempt=%d fps=%d quality=%d bytes=%d%n", attempt, fps, quality, old.length());
                report.flush();
                if (old.length() <= AdaptiveWebpSearch.MAX_BYTES) break;
                if (attempt == 1 && old.length() - 700 * 1024 > AdaptiveWebpSearch.MAX_BYTES) {
                    quality = 60; fps = 16;
                } else {
                    if (quality > 60) quality = Math.max(60, quality - 10);
                    if (fps > 15) fps = Math.max(15, fps - 2);
                    else if (fps > 1) fps--;
                    else fail("Original algorithm cannot fit this sample");
                }
            } while (attempt < 32);
            validate(old, report, "OLD", fps, quality);

            AdaptiveWebpSearch search = new AdaptiveWebpSearch(75, 30);
            File candidate = new File(dir, "candidate.webp");
            File best = new File(dir, "new.webp");
            attempt = 0;
            while (!search.isDone()) {
                Long cachedSize = previous.get(search.getFps() + ":" + search.getQuality());
                if (cachedSize == null) encode(source, candidate, search.getFps(), search.getQuality(), false);
                long size = cachedSize == null ? candidate.length() : cachedSize;
                report.printf(Locale.US, "new attempt=%d fps=%d quality=%d bytes=%d%n",
                        ++attempt, search.getFps(), search.getQuality(), size);
                if (cachedSize != null) report.println("  reused measured oversize trial from unchanged sample and encoder");
                report.flush();
                if (search.recordSize(size)) {
                    try (InputStream in = new FileInputStream(candidate); OutputStream out = new FileOutputStream(best)) {
                        byte[] buffer = new byte[8192]; int n;
                        while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
                    }
                }
                assertTrue(attempt <= 22);
            }
            assertTrue("Sample needs a shorter duration to keep balanced quality", search.hasResult());
            validate(best, report, "NEW", search.getFps(), search.getBestQuality());
        }
    }

    private void encode(File input, File output, int fps, int quality, boolean old) {
        FFmpegKitConfig.setLogLevel(Level.AV_LOG_DEBUG);
        String filter = old
                ? "trim=duration=8,setpts=PTS-STARTPTS,format=rgba,pad=720:720:(ow-iw)/2:(oh-ih)/2:color=#00000000,crop=720:720:0:0,scale=512:512:force_original_aspect_ratio=decrease,pad=512:512:(ow-iw)/2:(oh-ih)/2:color=#00000000,fps=" + fps
                : "trim=duration=8,setpts=PTS-STARTPTS,fps=" + fps + ",format=rgba,pad=720:720:(ow-iw)/2:(oh-ih)/2:color=#00000000,crop=720:720:0:0,scale=512:512:force_original_aspect_ratio=decrease:flags=lanczos,pad=512:512:(ow-iw)/2:(oh-ih)/2:color=#00000000";
        Session session = FFmpegKit.executeWithArguments(new String[]{"-y", "-loglevel", "debug",
                "-i", input.getAbsolutePath(), "-vf", filter, "-c:v", "libwebp_anim",
                "-lossless", "0", old ? "-q:v" : "-quality", String.valueOf(quality),
                "-compression_level", "6", "-preset", old ? "default" : "none",
                "-loop", "0", "-an", "-vsync", "0", output.getAbsolutePath()});
        assertTrue(session.getAllLogsAsString(), ReturnCode.isSuccess(session.getReturnCode()));
        assertTrue("Requested compression method must be honored: " + session.getAllLogsAsString(),
                session.getAllLogsAsString().contains(old ? "method=4" : "method=6"));
    }

    private void validate(File file, PrintWriter report, String label, int fps, int quality) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (InputStream in = new FileInputStream(file)) {
            byte[] buffer = new byte[8192]; int n;
            while ((n = in.read(buffer)) != -1) bytes.write(buffer, 0, n);
        }
        WebPImage image = WebPImage.createFromByteArray(bytes.toByteArray(), ImageDecodeOptions.defaults());
        try {
            assertTrue(file.length() <= AdaptiveWebpSearch.MAX_BYTES);
            assertEquals(512, image.getWidth());
            assertEquals(512, image.getHeight());
            assertTrue(image.getFrameCount() > 1);
            assertTrue(image.getDuration() <= 10000);
            for (int duration : image.getFrameDurations()) assertTrue(duration >= 8);
            report.printf(Locale.US, "%s FINAL requestedFps=%d quality=%d frames=%d durationMs=%d bytes=%d%n",
                    label, fps, quality, image.getFrameCount(), image.getDuration(), file.length());
            report.flush();
        } finally { image.dispose(); }
    }
}
