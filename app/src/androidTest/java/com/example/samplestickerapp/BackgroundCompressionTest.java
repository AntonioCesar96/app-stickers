package com.example.samplestickerapp;

import android.app.Instrumentation;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.service.notification.StatusBarNotification;

import androidx.test.platform.app.InstrumentationRegistry;

import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.ReturnCode;
import com.arthenica.ffmpegkit.Session;

import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

public class BackgroundCompressionTest {
    private final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
    private final Context context = instrumentation.getTargetContext();

    private Intent input() {
        return input(2);
    }

    private Intent input(double duration) {
        File video = new File(context.getCacheDir(), "background-compression-test.mp4");
        Session session = FFmpegKit.executeWithArguments(new String[]{"-y", "-f", "lavfi", "-i",
                "testsrc2=s=320x240:r=30:d=" + duration, "-c:v", "libx264", "-preset", "ultrafast", "-crf", "18",
                "-pix_fmt", "yuv420p", video.getAbsolutePath()});
        assertTrue(session.getAllLogsAsString(), ReturnCode.isSuccess(session.getReturnCode()));
        StickerPack pack = new StickerPack("background-test", "Background test", "Test", "tray.png",
                "", "", "", "", "1", false, true);
        pack.setStickers(new ArrayList<>());
        return new Intent(context, CropVideoActivity.class).putExtra("file_path", video.getAbsolutePath())
                .putExtra("sticker_pack", pack).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    }

    private CompressionJob awaitJob(boolean done) throws Exception {
        long deadline = System.currentTimeMillis() + 180000;
        while (System.currentTimeMillis() < deadline) {
            CompressionJob job = CompressionJob.pending(context);
            if (job != null) {
                if (done && !job.running()) { assertTrue(job.data.toString(), job.done()); return job; }
                if (!done && job.data.optInt("progress") >= 0
                        && job.data.optString("message").contains("tentativa")) return job;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("Timed out waiting for compression");
    }

    private Notification notification(CompressionJob job) {
        for (StatusBarNotification n : context.getSystemService(NotificationManager.class).getActiveNotifications()) {
            if (n.getId() == CompressionService.notificationId(job.id)) return n.getNotification();
        }
        return null;
    }

    @Test public void minimizedJobNotifiesAndRestoresEditorThroughNotificationAndLauncher() throws Exception {
        assertNull("Do not overwrite a user's unfinished compression", CompressionJob.pending(context));
        CropVideoActivity screen = null;
        CropVideoActivity restored = null;
        Instrumentation.ActivityMonitor monitor = null;
        CompressionJob job = null;
        try {
            screen = (CropVideoActivity) instrumentation.startActivitySync(input());
            job = awaitJob(false);
            String id = job.id;
            long notificationDeadline = System.currentTimeMillis() + 12000;
            Notification progress;
            do { progress = notification(job); Thread.sleep(100); }
            while (progress == null && System.currentTimeMillis() < notificationDeadline);
            assertNotNull("A running foreground notification must be visible", progress);
            assertTrue((progress.flags & Notification.FLAG_ONGOING_EVENT) != 0);
            CropVideoActivity editor = screen;
            instrumentation.runOnMainSync(() -> {
                assertTrue(editor.progressDialog.isShowing());
                editor.progressDialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).performClick();
            });
            instrumentation.waitForIdleSync();
            // Destroying the editor used to cancel FFmpeg. The service must survive it.
            instrumentation.runOnMainSync(editor::finish);
            job = awaitJob(true);
            assertEquals(id, job.id);
            assertTrue(job.result(0).length() > 0 && job.result(0).length() <= AdaptiveWebpSearch.MAX_BYTES);
            long deadline = System.currentTimeMillis() + 5000;
            Notification finished;
            do { finished = notification(job); Thread.sleep(100); }
            while ((finished == null || (finished.flags & Notification.FLAG_ONGOING_EVENT) != 0)
                    && System.currentTimeMillis() < deadline);
            assertNotNull(finished);
            assertEquals(0, finished.flags & Notification.FLAG_ONGOING_EVENT);
            assertEquals("Figurinha pronta", finished.extras.getCharSequence(Notification.EXTRA_TITLE).toString());
            monitor = instrumentation.addMonitor(CropVideoActivity.class.getName(), null, false);
            finished.contentIntent.send();
            restored = (CropVideoActivity) instrumentation.waitForMonitorWithTimeout(monitor, 15000);
            assertNotNull("Notification must reopen the editor", restored);
            assertEquals(id, restored.getIntent().getStringExtra(CompressionJob.EXTRA_ID));
            waitSave(restored);
            CropVideoActivity firstRestore = restored;
            instrumentation.runOnMainSync(firstRestore::finish);
            // Reopening via the icon must restore the pending result too.
            monitor = instrumentation.addMonitor(CropVideoActivity.class.getName(), null, false);
            context.startActivity(new Intent(context, SplashActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            restored = (CropVideoActivity) instrumentation.waitForMonitorWithTimeout(monitor, 15000);
            assertNotNull("Launcher must restore the pending result", restored);
            waitSave(restored);
            assertEquals(id, restored.getIntent().getStringExtra(CompressionJob.EXTRA_ID));
            assertEquals("No second conversion on reopen", job.data.getJSONArray("results").toString(),
                    CompressionJob.load(context, id).data.getJSONArray("results").toString());
            CropVideoActivity launcherRestore = restored;
            instrumentation.runOnMainSync(launcherRestore::finish);
            waitDestroyed(launcherRestore);
            // Returning to another existing app screen must recover the job as well.
            monitor = instrumentation.addMonitor(CropVideoActivity.class.getName(), null, false);
            context.startActivity(new Intent(context, EntryActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            restored = (CropVideoActivity) instrumentation.waitForMonitorWithTimeout(monitor, 15000);
            assertNotNull("Returning to the app must restore the pending editor", restored);
            waitSave(restored);
            assertEquals(id, restored.getIntent().getStringExtra(CompressionJob.EXTRA_ID));
        } finally {
            CropVideoActivity a = screen, b = restored;
            instrumentation.runOnMainSync(() -> { if (a != null) a.finish(); if (b != null) b.finish(); });
            if (monitor != null) instrumentation.removeMonitor(monitor);
            cleanup(job);
        }
    }

    @Test public void committedSearchResumesAfterServiceRecreationAndKeepsQuadrantsSeparate() throws Exception {
        assertNull("Do not overwrite a user's unfinished compression", CompressionJob.pending(context));
        CompressionJob job = CompressionJob.create(context, input(.7), new Rect(0, 0, 512, 512),
                512, true, "1", "30", "75", "6");
        try {
            // Persist a simulated oversized first trial, as if Android had reclaimed the process
            // between attempts. The restart must replay that checkpoint rather than reset it.
            job.data.getJSONArray("trials").put(AdaptiveWebpSearch.MAX_BYTES * 2);
            job.save();
            CompressionService.start(context, job);
            CompressionJob done = awaitJob(true);
            assertEquals(job.id, done.id);
            assertEquals(4, done.data.getJSONArray("results").length());
            assertEquals(0, done.data.getJSONArray("trials").length());
            assertTrue(done.data.getJSONArray("results").getString(0).contains("Qualidade:"));
            assertFalse("Checkpoint must exclude the already rejected quality 75",
                    done.data.getJSONArray("results").getString(0).contains("Qualidade: 75"));
            for (int i = 0; i < 4; i++) {
                assertTrue(done.result(i).exists());
                assertTrue(done.result(i).length() <= AdaptiveWebpSearch.MAX_BYTES);
            }
            assertTrue(new File(done.data.getString("preview")).getParentFile().equals(done.directory));
            assertEquals("1", done.data.getString("speed"));
            assertEquals(new Rect(0, 0, 512, 512), done.crop(512));
        } finally { cleanup(job); }
    }

    @Test public void invalidSettingsNotifyErrorAndRestoreWithoutStartingAnotherEncode() throws Exception {
        assertNull("Do not overwrite a user's unfinished compression", CompressionJob.pending(context));
        CompressionJob job = CompressionJob.create(context, input(.2), new Rect(0, 0, 512, 512),
                512, false, "0", "30", "75", "6");
        CropVideoActivity screen = null;
        Instrumentation.ActivityMonitor monitor = null;
        try {
            CompressionService.start(context, job);
            long deadline = System.currentTimeMillis() + 10000;
            CompressionJob failed;
            do { failed = CompressionJob.load(context, job.id); Thread.sleep(100); }
            while (failed.running() && System.currentTimeMillis() < deadline);
            assertEquals("error", failed.data.getString("status"));
            Notification error;
            do { error = notification(failed); Thread.sleep(100); }
            while ((error == null || (error.flags & Notification.FLAG_ONGOING_EVENT) != 0)
                    && System.currentTimeMillis() < deadline);
            assertNotNull(error);
            assertEquals("Não foi possível comprimir", error.extras.getCharSequence(Notification.EXTRA_TITLE).toString());
            monitor = instrumentation.addMonitor(CropVideoActivity.class.getName(), null, false);
            error.contentIntent.send();
            screen = (CropVideoActivity) instrumentation.waitForMonitorWithTimeout(monitor, 10000);
            assertNotNull(screen);
            assertEquals(job.id, screen.getIntent().getStringExtra(CompressionJob.EXTRA_ID));
            assertFalse(screen.findViewById(R.id.btn_salvar).isEnabled());
            assertEquals("error", CompressionJob.load(context, job.id).data.getString("status"));
        } finally {
            CropVideoActivity editor = screen;
            instrumentation.runOnMainSync(() -> { if (editor != null) editor.finish(); });
            if (monitor != null) instrumentation.removeMonitor(monitor);
            cleanup(job);
        }
    }

    private void waitSave(CropVideoActivity screen) throws Exception {
        long deadline = System.currentTimeMillis() + 10000;
        AtomicBoolean enabled = new AtomicBoolean();
        while (!enabled.get() && System.currentTimeMillis() < deadline) {
            instrumentation.runOnMainSync(() -> enabled.set(screen.findViewById(R.id.btn_salvar).isEnabled()));
            Thread.sleep(100);
        }
        assertTrue("Restored output must be ready to save", enabled.get());
    }

    private void waitDestroyed(CropVideoActivity screen) throws Exception {
        long deadline = System.currentTimeMillis() + 5000;
        AtomicBoolean destroyed = new AtomicBoolean();
        while (!destroyed.get() && System.currentTimeMillis() < deadline) {
            instrumentation.runOnMainSync(() -> destroyed.set(screen.isDestroyed()));
            Thread.sleep(100);
        }
        assertTrue("Editor must have left the foreground before testing app return", destroyed.get());
    }

    private void cleanup(CompressionJob job) throws Exception {
        context.stopService(new Intent(context, CompressionService.class));
        if (job != null) {
            CompressionJob.consume(context, job.id);
            context.getSystemService(NotificationManager.class).cancel(CompressionService.notificationId(job.id));
        }
        Thread.sleep(300);
    }
}
