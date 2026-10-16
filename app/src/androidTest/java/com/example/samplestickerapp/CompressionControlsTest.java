package com.example.samplestickerapp;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.widget.EditText;
import android.widget.TextView;
import androidx.test.platform.app.InstrumentationRegistry;
import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.ReturnCode;
import com.arthenica.ffmpegkit.Session;
import org.junit.Test;
import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.Assert.*;

public class CompressionControlsTest {
    @Test public void nativeEncoderPublishesMeasuredIntermediatePercentage() throws Exception {
        CompressionJob previous = CompressionJob.pending(context);
        for (android.app.ActivityManager.RunningServiceInfo service :
                context.getSystemService(android.app.ActivityManager.class).getRunningServices(Integer.MAX_VALUE))
            assertNotEquals(CompressionService.class.getName(), service.service.getClassName());
        context.getSharedPreferences("compression_jobs", 0).edit().remove("pending").commit();
        CompressionJob job = null;
        File input = new File(context.getCacheDir(), "measured-progress-test.mp4");
        try {
            Session fixture = FFmpegKit.executeWithArguments(new String[]{"-y", "-f", "lavfi", "-i",
                    "testsrc2=s=512x512:r=30:d=4", "-c:v", "libx264", "-preset", "ultrafast", input.getAbsolutePath()});
            assertTrue(ReturnCode.isSuccess(fixture.getReturnCode()));
            StickerPack pack = new StickerPack("progress-test", "Progress", "Test", "tray.png", "", "", "", "", "1", false, true);
            pack.setStickers(new ArrayList<>());
            job = CompressionJob.create(context, new Intent().putExtra("file_path", input.getAbsolutePath()).putExtra("sticker_pack", pack),
                    new Rect(0, 0, 512, 512), 512, false, "1", "30", "100", "6");
            CompressionService.start(context, job);
            long deadline = System.currentTimeMillis() + 30000;
            int observed = 0;
            while (System.currentTimeMillis() < deadline) {
                CompressionJob state = CompressionJob.load(context, job.id);
                observed = state.data.optInt("progress");
                if (observed > 0 && observed < 99) break;
                assertTrue(state.data.toString(), state.running());
                Thread.sleep(100);
            }
            assertTrue("Must report a real intermediate percentage, observed " + observed, observed > 0 && observed < 99);
            job.request("cancel", null);
            waitFor(job.id, "cancelled", false);
        } finally {
            context.stopService(new Intent(context, CompressionService.class));
            Thread.sleep(300);
            if (job != null) { job.data.put("status", "cancelled"); job.save(); CompressionJob.consume(context, job.id);
                context.getSystemService(android.app.NotificationManager.class).cancel(CompressionService.notificationId(job.id)); }
            android.content.SharedPreferences.Editor edit = context.getSharedPreferences("compression_jobs", 0).edit();
            if (previous == null) edit.remove("pending"); else edit.putString("pending", previous.id);
            edit.commit(); input.delete();
        }
    }
    @Test public void organizedDialogUses95PercentWidthAndKeepsFieldsVisible() throws Exception {
        CompressionJob previous = CompressionJob.pending(context);
        for (android.app.ActivityManager.RunningServiceInfo service :
                context.getSystemService(android.app.ActivityManager.class).getRunningServices(Integer.MAX_VALUE))
            assertNotEquals(CompressionService.class.getName(), service.service.getClassName());
        context.getSharedPreferences("compression_jobs", 0).edit().remove("pending").commit();
        CompressionJob job = null;
        CropVideoActivity screen = null;
        File input = new File(context.getCacheDir(), "organized-dialog-test.mp4");
        try {
            Session fixture = FFmpegKit.executeWithArguments(new String[]{"-y", "-f", "lavfi", "-i",
                    "testsrc2=s=128x128:r=12:d=1", "-c:v", "libx264", "-preset", "ultrafast", input.getAbsolutePath()});
            assertTrue(ReturnCode.isSuccess(fixture.getReturnCode()));
            StickerPack pack = new StickerPack("layout-test", "Layout", "Test", "tray.png", "", "", "", "", "1", false, true);
            pack.setStickers(new ArrayList<>());
            Intent launch = new Intent(context, CropVideoActivity.class).putExtra("file_path", input.getAbsolutePath()).putExtra("sticker_pack", pack);
            job = CompressionJob.create(context, launch, new Rect(0, 0, 512, 512), 512, false, "1", "12", "55", "6");
            // Synthetic finished-attempt metadata: this test checks layout, not codec output.
            org.json.JSONObject metrics = new org.json.JSONObject().put("number", 8).put("quadrant", 1).put("quality", 48)
                    .put("fps", 12).put("compression", 6).put("speed", 1).put("detailSize", 512).put("bytes", 680000);
            job.data.put("status", "cancelled").put("message", "Compressão cancelada.")
                    .put("lastAttempt", metrics).put("currentAttempt", metrics); job.save();
            screen = (CropVideoActivity) instrumentation.startActivitySync(job.screen(context).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            CropVideoActivity editor = screen;
            Thread.sleep(1500);
            instrumentation.runOnMainSync(() -> editor.findViewById(R.id.compression_status).performClick());
            Thread.sleep(500);
            Bitmap screenshot = instrumentation.getUiAutomation().takeScreenshot();
            try (FileOutputStream out = new FileOutputStream(new File(context.getCacheDir(), "compression-dialog-organized.png"))) {
                screenshot.compress(Bitmap.CompressFormat.PNG, 100, out);
            } finally { screenshot.recycle(); }
            instrumentation.runOnMainSync(() -> {
                assertTrue(editor.progressDialog.isShowing());
                int expected = (int) (editor.getResources().getDisplayMetrics().widthPixels * .95);
                assertEquals(expected, editor.progressDialog.getWindow().getDecorView().getWidth(), 2);
                for (int id : new int[]{R.id.retry_quality, R.id.retry_fps, R.id.retry_compression, R.id.retry_speed,
                        R.id.retry_detail, R.id.retry_compression_button, R.id.cancel_compression})
                    assertTrue("Control must be visible: " + id, editor.progressDialog.findViewById(id).getGlobalVisibleRect(new Rect()));
            });
            job.data.put("status", "running").put("progress", 42); job.save();
            Thread.sleep(1000);
            instrumentation.runOnMainSync(() -> {
                android.widget.ProgressBar bar = editor.progressDialog.findViewById(R.id.progress_bar);
                assertFalse(bar.isIndeterminate());
                assertEquals(42, bar.getProgress());
                assertTrue(((TextView) editor.progressDialog.findViewById(R.id.compression_percentage)).getText().toString().contains("42%"));
            });
            screenshot = instrumentation.getUiAutomation().takeScreenshot();
            try (FileOutputStream out = new FileOutputStream(new File(context.getCacheDir(), "compression-dialog-progress42.png"))) {
                screenshot.compress(Bitmap.CompressFormat.PNG, 100, out);
            } finally { screenshot.recycle(); }
        } finally {
            CropVideoActivity editor = screen;
            instrumentation.runOnMainSync(() -> { if (editor != null) editor.finish(); });
            if (job != null) { job.data.put("status", "cancelled"); job.save(); CompressionJob.consume(context, job.id); }
            android.content.SharedPreferences.Editor edit = context.getSharedPreferences("compression_jobs", 0).edit();
            if (previous != null) edit.putString("pending", previous.id); else edit.remove("pending");
            edit.commit(); input.delete();
        }
    }
    private final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
    private final Context context = instrumentation.getTargetContext();

    private CompressionJob waitFor(String id, String state, boolean lastAttempt) throws Exception {
        long deadline = System.currentTimeMillis() + 180000;
        while (System.currentTimeMillis() < deadline) {
            CompressionJob job = CompressionJob.load(context, id);
            if (job != null && (state == null || state.equals(job.data.optString("status")))
                    && (!lastAttempt || job.data.has("lastAttempt"))) return job;
            if (job != null && "error".equals(job.data.optString("status"))) fail(job.data.toString());
            Thread.sleep(50);
        }
        throw new AssertionError("Timeout waiting for " + state);
    }

    private void values(CropVideoActivity editor, String quality, String fps, String detail) {
        ((EditText) editor.progressDialog.findViewById(R.id.retry_quality)).setText(quality);
        ((EditText) editor.progressDialog.findViewById(R.id.retry_fps)).setText(fps);
        ((EditText) editor.progressDialog.findViewById(R.id.retry_compression)).setText("6");
        ((EditText) editor.progressDialog.findViewById(R.id.retry_speed)).setText("1");
        ((EditText) editor.progressDialog.findViewById(R.id.retry_detail)).setText(detail);
    }

    @Test public void manualRestartInterruptsSearchAndCancelKeepsDialogOpen() throws Exception {
        CompressionJob previous = CompressionJob.pending(context);
        for (android.app.ActivityManager.RunningServiceInfo service :
                context.getSystemService(android.app.ActivityManager.class).getRunningServices(Integer.MAX_VALUE)) {
            assertNotEquals("Do not interrupt an active user encoder", CompressionService.class.getName(), service.service.getClassName());
        }
        android.content.SharedPreferences prefs = context.getSharedPreferences("compression_jobs", 0);
        String dismissed = prefs.getString("dismissedEditor", null);
        prefs.edit().remove("pending").commit();
        ArrayList<String> jobs = new ArrayList<>();
        CropVideoActivity screen = null;
        File input = new File(context.getCacheDir(), "manual compression controls.mp4");
        try {
            Session fixture = FFmpegKit.executeWithArguments(new String[]{"-y", "-f", "lavfi", "-i",
                    "testsrc2=s=512x512:r=30:d=10", "-c:v", "libx264", "-preset", "ultrafast", "-crf", "18", input.getAbsolutePath()});
            assertTrue(fixture.getAllLogsAsString(), ReturnCode.isSuccess(fixture.getReturnCode()));
            StickerPack pack = new StickerPack("controls-test", "Controls", "Test", "tray.png", "", "", "", "", "1", false, true);
            pack.setStickers(new ArrayList<>());
            Intent launch = new Intent(context, CropVideoActivity.class).putExtra("file_path", input.getAbsolutePath())
                    .putExtra("sticker_pack", pack).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            CompressionJob job = CompressionJob.create(context, launch, new Rect(0, 0, 512, 512), 512, false, "1", "30", "100", "0");
            jobs.add(job.id);
            screen = (CropVideoActivity) instrumentation.startActivitySync(job.screen(context).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            CropVideoActivity editor = screen;
            job = waitFor(job.id, "running", true);
            assertTrue("Fixture should require further compression", job.data.getJSONObject("lastAttempt").getLong("bytes") > 512000);
            Thread.sleep(900);
            instrumentation.runOnMainSync(() -> {
                assertTrue(editor.progressDialog.isShowing());
                String text = ((TextView) editor.progressDialog.findViewById(R.id.webp_info_tv)).getText().toString();
                assertTrue(text, text.contains("compressão"));
                assertTrue(((TextView) editor.progressDialog.findViewById(R.id.last_attempt_size)).getText().toString().contains("KB / 500 KB"));
                assertFalse(text.contains("Você pode minimizar"));
                values(editor, "101", "8", "128");
                editor.progressDialog.findViewById(R.id.retry_compression_button).performClick();
                assertTrue(((TextView) editor.progressDialog.findViewById(R.id.compression_action_status)).getText().toString().contains("Qualidade"));
                values(editor, "20", "8", "128");
                editor.progressDialog.findViewById(R.id.retry_compression_button).performClick();
            });
            CompressionJob done = waitFor(job.id, "done", true);
            assertEquals(1, done.data.getInt("appliedRevision"));
            assertTrue("Restart must stop an active native attempt", done.data.optInt("interruptedAttempts") > 0);
            assertEquals(20, done.data.getJSONObject("lastAttempt").getInt("quality"));
            assertEquals(8, done.data.getJSONObject("lastAttempt").getInt("fps"));
            assertEquals(128, done.data.getJSONObject("lastAttempt").getInt("detailSize"));
            assertEquals(done.result(0).length(), done.data.getJSONObject("lastAttempt").getLong("bytes"));
            assertTrue(done.result(0).length() <= 512000);
            Thread.sleep(900);
            instrumentation.runOnMainSync(() -> {
                values(editor, "100", "30", "512");
                editor.progressDialog.findViewById(R.id.retry_compression_button).performClick();
            });
            CompressionJob cancelling = CompressionJob.pending(context);
            assertNotEquals(job.id, cancelling.id); jobs.add(cancelling.id);
            long deadline = System.currentTimeMillis() + 10000;
            while (!CompressionJob.load(context, cancelling.id).data.has("currentAttempt") && System.currentTimeMillis() < deadline) Thread.sleep(50);
            instrumentation.runOnMainSync(() -> editor.progressDialog.findViewById(R.id.cancel_compression).performClick());
            CompressionJob cancelled = waitFor(cancelling.id, "cancelled", false);
            assertFalse(cancelled.shouldRestoreAutomatically(context));
            Thread.sleep(900);
            instrumentation.runOnMainSync(() -> {
                assertTrue("Cancelling must leave the dialog open", editor.progressDialog.isShowing());
                assertTrue(editor.progressDialog.findViewById(R.id.retry_compression_button).isEnabled());
                assertTrue("Retry action must stay visible", editor.progressDialog.findViewById(R.id.retry_compression_button)
                        .getGlobalVisibleRect(new Rect()));
                assertTrue("Cancel action must stay visible", editor.progressDialog.findViewById(R.id.cancel_compression)
                        .getGlobalVisibleRect(new Rect()));
            });
            Bitmap screenshot = instrumentation.getUiAutomation().takeScreenshot();
            try (FileOutputStream out = new FileOutputStream(new File(context.getCacheDir(), "compression-controls-cancelled.png"))) {
                screenshot.compress(Bitmap.CompressFormat.PNG, 100, out);
            } finally { screenshot.recycle(); }
            instrumentation.runOnMainSync(() -> editor.progressDialog.findViewById(R.id.compression_info).performClick());
            Thread.sleep(400);
            screenshot = instrumentation.getUiAutomation().takeScreenshot();
            try (FileOutputStream out = new FileOutputStream(new File(context.getCacheDir(), "compression-controls-info.png"))) {
                screenshot.compress(Bitmap.CompressFormat.PNG, 100, out);
            } finally { screenshot.recycle(); }
            instrumentation.runOnMainSync(() -> {
                try {
                    java.lang.reflect.Field field = CropVideoActivity.class.getDeclaredField("compressionInfo");
                    field.setAccessible(true);
                    android.widget.PopupWindow popup = (android.widget.PopupWindow) field.get(editor);
                    assertTrue("Info must appear only after tapping the icon", popup.isShowing());
                    popup.dismiss();
                } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
            });
            instrumentation.runOnMainSync(() -> {
                values(editor, "10", "8", "128");
                editor.progressDialog.findViewById(R.id.retry_compression_button).performClick();
            });
            CompressionJob restarted = CompressionJob.pending(context); jobs.add(restarted.id);
            done = waitFor(restarted.id, "done", true);
            assertEquals(10, done.data.getJSONObject("lastAttempt").getInt("quality"));
            assertTrue(done.result(0).length() <= 512000);
        } finally {
            CropVideoActivity editor = screen;
            instrumentation.runOnMainSync(() -> { if (editor != null) editor.finish(); });
            context.stopService(new Intent(context, CompressionService.class));
            Thread.sleep(500);
            for (String id : jobs) {
                CompressionJob job = CompressionJob.load(context, id);
                if (job != null) { job.data.put("status", "cancelled"); job.save(); CompressionJob.consume(context, id); }
                context.getSystemService(android.app.NotificationManager.class).cancel(CompressionService.notificationId(id));
            }
            android.content.SharedPreferences.Editor edit = prefs.edit();
            if (previous == null) edit.remove("pending"); else edit.putString("pending", previous.id);
            if (dismissed == null) edit.remove("dismissedEditor"); else edit.putString("dismissedEditor", dismissed);
            edit.commit(); input.delete();
        }
    }
}
