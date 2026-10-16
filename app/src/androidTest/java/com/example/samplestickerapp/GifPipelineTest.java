package com.example.samplestickerapp;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import androidx.test.platform.app.InstrumentationRegistry;
import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.FFprobeKit;
import com.arthenica.ffmpegkit.MediaInformation;
import com.arthenica.ffmpegkit.ReturnCode;
import com.arthenica.ffmpegkit.Session;
import com.facebook.animated.webp.WebPImage;
import com.facebook.imagepipeline.common.ImageDecodeOptions;
import org.junit.Test;
import java.io.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.Assert.*;

public class GifPipelineTest {
    private File fixture(Context context) {
        File dir = new File(context.getCacheDir(), "gif-pipeline-test");
        assertTrue(dir.isDirectory() || dir.mkdirs());
        File gif = new File(dir, "transparent odd size.GIF");
        // 65x49, transparency, infinite loop and a 600 ms first frame followed by
        // 200 ms frames. A cut at 250 ms must retain the first held frame.
        String input = "nullsrc=s=65x49:r=5:d=2,format=rgba,"
                + "geq=r=255:g=0:b=0:a='if(lt(X,10+10*T),255,0)',"
                + "setpts='if(eq(N,0),0,(N+2)/(5*TB))'";
        execute(new String[]{"-y", "-f", "lavfi", "-i", input, "-filter_complex",
                "[0:v]split[a][b];[a]palettegen=reserve_transparent=1[p];[b][p]paletteuse",
                "-vsync", "vfr", "-loop", "0", gif.getAbsolutePath()});
        return gif;
    }

    private void execute(String[] args) {
        Session session = FFmpegKit.executeWithArguments(args);
        assertTrue(session.getAllLogsAsString(), ReturnCode.isSuccess(session.getReturnCode()));
    }

    @Test public void proxyAndDirectExportPreserveTimingDimensionsAndAlpha() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        File gif = fixture(context);
        File preview = new File(gif.getParentFile(), "preview.mp4");
        execute(GifMedia.previewArguments(gif, preview));
        MediaInformation sourceInfo = FFprobeKit.getMediaInformation(gif.getAbsolutePath()).getMediaInformation();
        MediaInformation previewInfo = FFprobeKit.getMediaInformation(preview.getAbsolutePath()).getMediaInformation();
        assertNotNull(sourceInfo);
        assertNotNull(previewInfo);
        assertEquals(66, previewInfo.getStreams().get(0).getWidth().intValue());
        assertEquals(50, previewInfo.getStreams().get(0).getHeight().intValue());
        assertEquals(Double.parseDouble(sourceInfo.getDuration()), Double.parseDouble(previewInfo.getDuration()), .05);
        File output = new File(gif.getParentFile(), "direct.webp");
        String filter = GifMedia.timelineFilter(250, 1250, 1, 30)
                + "format=rgba,pad=65:65:0:(oh-ih)/2:color=#00000000,scale=512:512:flags=lanczos";
        execute(GifMedia.webpArguments(gif, true, filter, 75, 6, output));
        validate(output);
    }

    @Test public void importAndTrimUseTheOriginalGifThroughTheActualScreens() throws Exception {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Context context = instrumentation.getTargetContext();
        FilesHelper.setContext(context);
        File gif = fixture(context);
        StickerPack pack = new StickerPack("gif-test", "GIF test", "Test", "tray.png",
                "", "", "", "", "1", false, true);
        pack.setStickers(new ArrayList<>());
        File output = new File(FilesHelper.getTempDir(), "output_cropped.webp");
        byte[] previous = output.exists() ? read(output) : null;
        FileExplorerActivity explorer = null;
        CustomVideoRangeActivity trim = null;
        CropVideoActivity crop = null;
        Instrumentation.ActivityMonitor trimMonitor = instrumentation.addMonitor(CustomVideoRangeActivity.class.getName(), null, false);
        Instrumentation.ActivityMonitor cropMonitor = instrumentation.addMonitor(CropVideoActivity.class.getName(), null, false);
        try {
            Intent intent = new Intent(context, FileExplorerActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("sticker_pack", pack);
            explorer = (FileExplorerActivity) instrumentation.startActivitySync(intent);
            FileExplorerActivity host = explorer;
            instrumentation.runOnMainSync(() -> new FileExplorerHelper(host, pack).extracted(gif, CustomVideoRangeActivity.class));
            trim = (CustomVideoRangeActivity) instrumentation.waitForMonitorWithTimeout(trimMonitor, 20000);
            assertNotNull("GIF must open trimming without a resolution dialog", trim);
            assertEquals(gif.getAbsolutePath(), trim.getIntent().getStringExtra(GifMedia.SOURCE));
            String proxy = trim.getIntent().getStringExtra("file_path");
            CustomVideoRangeActivity trimScreen = trim;
            Field duration = CustomVideoRangeActivity.class.getDeclaredField("videoDuration");
            duration.setAccessible(true);
            long deadline = System.currentTimeMillis() + 10000;
            AtomicBoolean ready = new AtomicBoolean();
            while (!ready.get() && System.currentTimeMillis() < deadline) {
                instrumentation.runOnMainSync(() -> {
                    try { ready.set(duration.getInt(trimScreen) > 0); }
                    catch (IllegalAccessException e) { throw new AssertionError(e); }
                });
                Thread.sleep(50);
            }
            assertTrue("Preview must become ready", ready.get());
            Field start = CustomVideoRangeActivity.class.getDeclaredField("currentStartMs");
            Field end = CustomVideoRangeActivity.class.getDeclaredField("currentEndMs");
            start.setAccessible(true); end.setAccessible(true);
            instrumentation.runOnMainSync(() -> {
                try { start.setInt(trimScreen, 250); end.setInt(trimScreen, 1250); }
                catch (IllegalAccessException e) { throw new AssertionError(e); }
                trimScreen.findViewById(R.id.show_range_button).performClick();
            });
            crop = (CropVideoActivity) instrumentation.waitForMonitorWithTimeout(cropMonitor, 10000);
            assertNotNull(crop);
            assertEquals(proxy, crop.getIntent().getStringExtra("file_path"));
            assertEquals(gif.getAbsolutePath(), crop.getIntent().getStringExtra(GifMedia.SOURCE));
            assertEquals(250L, crop.getIntent().getLongExtra(GifMedia.START_MS, -1));
            assertEquals(1250L, crop.getIntent().getLongExtra(GifMedia.END_MS, -1));
            CropVideoActivity cropScreen = crop;
            ready.set(false);
            deadline = System.currentTimeMillis() + 30000;
            while (!ready.get() && System.currentTimeMillis() < deadline) {
                instrumentation.runOnMainSync(() -> ready.set(cropScreen.findViewById(R.id.btn_salvar).isEnabled()));
                Thread.sleep(50);
            }
            assertTrue("Crop screen must produce a valid sticker directly from GIF", ready.get());
            validate(output);
        } finally {
            CropVideoActivity c = crop; CustomVideoRangeActivity t = trim; FileExplorerActivity e = explorer;
            instrumentation.runOnMainSync(() -> {
                if (c != null) c.finish();
                if (t != null) t.finish();
                if (e != null) e.finish();
            });
            instrumentation.waitForIdleSync();
            instrumentation.removeMonitor(trimMonitor);
            instrumentation.removeMonitor(cropMonitor);
            if (previous != null) {
                try (OutputStream out = new FileOutputStream(output)) { out.write(previous); }
            } else output.delete();
        }
    }

    private byte[] read(File file) throws IOException {
        try (InputStream in = new FileInputStream(file); ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192]; int n;
            while ((n = in.read(buffer)) != -1) bytes.write(buffer, 0, n);
            return bytes.toByteArray();
        }
    }

    private void validate(File output) throws IOException {
        assertTrue(output.length() > 0 && output.length() <= AdaptiveWebpSearch.MAX_BYTES);
        WebPImage image = WebPImage.createFromByteArray(read(output), ImageDecodeOptions.defaults());
        Bitmap bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888);
        try {
            assertEquals(512, image.getWidth()); assertEquals(512, image.getHeight());
            assertTrue(image.getFrameCount() > 1);
            assertEquals(1000, image.getDuration(), 40);
            for (int ms : image.getFrameDurations()) assertTrue(ms >= 8);
            com.facebook.animated.webp.WebPFrame frame = image.getFrame(0);
            Bitmap region = Bitmap.createBitmap(frame.getWidth(), frame.getHeight(), Bitmap.Config.ARGB_8888);
            try {
                // Animated WebP stores cropped rectangles, not necessarily full canvases.
                frame.renderFrame(frame.getWidth(), frame.getHeight(), region);
                new Canvas(bitmap).drawBitmap(region, frame.getXOffset(), frame.getYOffset(), null);
            } finally { region.recycle(); frame.dispose(); }
            assertTrue(Color.alpha(bitmap.getPixel(50, 256)) > 200);
            assertEquals("Cut inside a held frame must not jump to the next frame", 0, Color.alpha(bitmap.getPixel(100, 256)));
            assertEquals("Transparency must survive export", 0, Color.alpha(bitmap.getPixel(500, 256)));
        } finally { bitmap.recycle(); image.dispose(); }
    }
}
