package com.example.samplestickerapp;

import android.app.Instrumentation;
import android.content.ClipData;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.View;
import androidx.test.platform.app.InstrumentationRegistry;
import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.ReturnCode;
import com.arthenica.ffmpegkit.Session;
import org.junit.Test;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.Assert.*;

public class ShareMediaTest {
    private final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
    private final Context context = instrumentation.getTargetContext();

    private Intent incoming(Uri uri, String mime) {
        Intent intent = new Intent(Intent.ACTION_SEND).setType(mime).putExtra(Intent.EXTRA_STREAM, uri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setClipData(ClipData.newRawUri("Shared test", uri));
        return intent;
    }

    private Uri publish(File source, String mime) throws Exception {
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, "stickers-share-test-" + UUID.randomUUID() + (mime.startsWith("video/") ? ".mp4" : ".gif"));
        values.put(MediaStore.MediaColumns.MIME_TYPE, mime);
        Uri uri = context.getContentResolver().insert(mime.startsWith("video/") ? MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                : MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        assertNotNull(uri);
        try (FileInputStream in = new FileInputStream(source); OutputStream out = context.getContentResolver().openOutputStream(uri)) {
            byte[] buffer = new byte[65536]; int n; while ((n = in.read(buffer)) != -1) out.write(buffer, 0, n);
        }
        return uri;
    }

    private void ready(ShareMediaActivity screen) throws Exception {
        long deadline = System.currentTimeMillis() + 15000;
        AtomicBoolean ready = new AtomicBoolean();
        while (!ready.get() && System.currentTimeMillis() < deadline) {
            instrumentation.runOnMainSync(() -> ready.set(screen.findViewById(R.id.shared_crop).isEnabled()));
            Thread.sleep(50);
        }
        assertTrue("Shared media and a compatible destination pack must load", ready.get());
    }

    @Test public void sendResolverSupportsImagesAndVideosButNotText() throws Exception {
        for (String mime : new String[]{"video/mp4", "video/webm", "image/jpeg", "image/png", "image/gif"}) {
            assertTrue(mime, context.getPackageManager().queryIntentActivities(new Intent(Intent.ACTION_SEND).setType(mime)
                    .setPackage(context.getPackageName()), PackageManager.MATCH_DEFAULT_ONLY).stream()
                    .anyMatch(info -> ShareMediaActivity.class.getName().equals(info.activityInfo.name)));
        }
        assertFalse(context.getPackageManager().queryIntentActivities(new Intent(Intent.ACTION_SEND).setType("text/plain")
                .setPackage(context.getPackageName()), PackageManager.MATCH_DEFAULT_ONLY).stream()
                .anyMatch(info -> ShareMediaActivity.class.getName().equals(info.activityInfo.name)));
        for (String action : new String[]{Intent.ACTION_SEND, Intent.ACTION_VIEW}) {
            try { SharedMediaImport.receive(context, new Intent(action).setType("image/png")); fail("Invalid share should be rejected"); }
            catch (java.io.IOException expected) { }
        }
    }

    @Test public void sharedVideoSurvivesRecreationAndOpensBothExistingEditors() throws Exception {
        for (android.app.ActivityManager.RunningServiceInfo service : context.getSystemService(android.app.ActivityManager.class).getRunningServices(100))
            assertNotEquals("Do not interrupt an active user compression", CompressionService.class.getName(), service.service.getClassName());
        File video = new File(context.getCacheDir(), "share-video-fixture.mp4");
        Uri uri = null; ShareMediaActivity share = null;
        CustomVideoRangeActivity trim = null; CropVideoActivity crop = null;
        ArrayList<Instrumentation.ActivityMonitor> monitors = new ArrayList<>();
        try {
            Session session = FFmpegKit.executeWithArguments(new String[]{"-y", "-f", "lavfi", "-i", "testsrc2=s=160x120:r=12:d=2",
                    "-c:v", "libx264", "-preset", "ultrafast", video.getAbsolutePath()});
            assertTrue(session.getAllLogsAsString(), ReturnCode.isSuccess(session.getReturnCode()));
            uri = publish(video, "video/mp4");
            share = (ShareMediaActivity) instrumentation.startActivitySync(incoming(uri, "video/mp4").setClass(context, ShareMediaActivity.class));
            ready(share);
            ShareMediaActivity first = share;
            Instrumentation.ActivityMonitor recreation = instrumentation.addMonitor(ShareMediaActivity.class.getName(), null, false); monitors.add(recreation);
            // Delete the provider's original: subsequent routing must use the imported private copy.
            context.getContentResolver().delete(uri, null, null); uri = null;
            instrumentation.runOnMainSync(first::recreate);
            share = (ShareMediaActivity) instrumentation.waitForMonitorWithTimeout(recreation, 10000);
            assertNotNull(share); ready(share);
            ShareMediaActivity editor = share;
            Instrumentation.ActivityMonitor trimMonitor = instrumentation.addMonitor(CustomVideoRangeActivity.class.getName(), null, false); monitors.add(trimMonitor);
            instrumentation.runOnMainSync(() -> editor.findViewById(R.id.shared_trim).performClick());
            trim = (CustomVideoRangeActivity) instrumentation.waitForMonitorWithTimeout(trimMonitor, 10000);
            assertNotNull("Aparar must open existing trim editor", trim);
            assertTrue(new File(trim.getIntent().getStringExtra("file_path")).isFile());
            assertTrue(((StickerPack) trim.getIntent().getParcelableExtra("sticker_pack")).animatedStickerPack);
            CustomVideoRangeActivity trimEditor = trim;
            instrumentation.runOnMainSync(trimEditor::finish);
            Thread.sleep(500); ready(editor);
            Instrumentation.ActivityMonitor cropMonitor = instrumentation.addMonitor(CropVideoActivity.class.getName(), null, false); monitors.add(cropMonitor);
            instrumentation.runOnMainSync(() -> editor.findViewById(R.id.shared_crop).performClick());
            crop = (CropVideoActivity) instrumentation.waitForMonitorWithTimeout(cropMonitor, 10000);
            assertNotNull("Cortar must open existing crop editor", crop);
            assertTrue(new File(crop.getIntent().getStringExtra("file_path")).isFile());
        } finally {
            ShareMediaActivity s = share; CustomVideoRangeActivity t = trim; CropVideoActivity c = crop;
            instrumentation.runOnMainSync(() -> { if (c != null) c.finish(); if (t != null) t.finish(); if (s != null) s.finish(); });
            for (Instrumentation.ActivityMonitor monitor : monitors) instrumentation.removeMonitor(monitor);
            if (uri != null) context.getContentResolver().delete(uri, null, null); video.delete();
        }
    }

    @Test public void photoUsesImageCropAndGifKeepsAnimatedImport() throws Exception {
        Uri image = null, gif = null;
        ShareMediaActivity share = null; CropImageActivity crop = null;
        File gifFile = new File(context.getCacheDir(), "shared-gif-fixture.gif");
        Instrumentation.ActivityMonitor monitor = null;
        try {
            ContentValues values = new ContentValues(); values.put(MediaStore.MediaColumns.DISPLAY_NAME, "share-test-" + UUID.randomUUID() + ".png");
            values.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
            image = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            Bitmap bitmap = Bitmap.createBitmap(160, 120, Bitmap.Config.ARGB_8888); bitmap.eraseColor(android.graphics.Color.GREEN);
            try (OutputStream out = context.getContentResolver().openOutputStream(image)) { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)); }
            finally { bitmap.recycle(); }
            share = (ShareMediaActivity) instrumentation.startActivitySync(incoming(image, "image/png").setClass(context, ShareMediaActivity.class));
            ready(share); ShareMediaActivity imageShare = share;
            instrumentation.runOnMainSync(() -> assertEquals(View.GONE, imageShare.findViewById(R.id.shared_trim).getVisibility()));
            monitor = instrumentation.addMonitor(CropImageActivity.class.getName(), null, false);
            instrumentation.runOnMainSync(() -> imageShare.findViewById(R.id.shared_crop).performClick());
            crop = (CropImageActivity) instrumentation.waitForMonitorWithTimeout(monitor, 10000);
            assertNotNull(crop);
            assertFalse(((StickerPack) crop.getIntent().getParcelableExtra("sticker_pack")).animatedStickerPack);
            Thread.sleep(500);
            CropImageActivity imageEditor = crop;
            instrumentation.runOnMainSync(() -> { imageEditor.finish(); imageShare.finish(); });
            share = null; crop = null;
            Session session = FFmpegKit.executeWithArguments(new String[]{"-y", "-f", "lavfi", "-i", "testsrc2=s=64x64:r=5:d=1", gifFile.getAbsolutePath()});
            assertTrue(session.getAllLogsAsString(), ReturnCode.isSuccess(session.getReturnCode()));
            gif = publish(gifFile, "image/gif");
            // Exercise ClipData fallback and detect GIF from its signature rather than display-name extension.
            Intent gifIntent = incoming(gif, "image/*"); gifIntent.removeExtra(Intent.EXTRA_STREAM);
            SharedMediaImport imported = SharedMediaImport.receive(context, gifIntent);
            try { assertEquals("gif", imported.kind); assertTrue(imported.file.getName().endsWith(".gif")); }
            finally { imported.file.delete(); }
        } finally {
            ShareMediaActivity s = share; CropImageActivity c = crop;
            instrumentation.runOnMainSync(() -> { if (c != null) c.finish(); if (s != null) s.finish(); });
            if (monitor != null) instrumentation.removeMonitor(monitor);
            if (image != null) context.getContentResolver().delete(image, null, null);
            if (gif != null) context.getContentResolver().delete(gif, null, null); gifFile.delete();
        }
    }
}
