package com.example.samplestickerapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.FFmpegSession;
import com.arthenica.ffmpegkit.FFprobeKit;
import com.arthenica.ffmpegkit.MediaInformation;
import com.arthenica.ffmpegkit.ReturnCode;
import com.arthenica.ffmpegkit.StreamInformation;
import com.facebook.animated.webp.WebPImage;
import com.facebook.imagepipeline.common.ImageDecodeOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.TimeUnit;

/** Owns the encoder independently of the editor's lifecycle. */
public class CompressionService extends Service {
    static final String PROGRESS_CHANNEL = "sticker_compression";
    static final String FINISHED_CHANNEL = "sticker_compression_finished";
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private volatile boolean destroyed;
    private volatile long sessionId = -1;
    private String runningId;
    private int latestStartId;
    private PowerManager.WakeLock wakeLock;

    static void channels(Context context) {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            manager.createNotificationChannel(new NotificationChannel(PROGRESS_CHANNEL,
                    "Compressão em andamento", NotificationManager.IMPORTANCE_LOW));
            manager.createNotificationChannel(new NotificationChannel(FINISHED_CHANNEL,
                    "Compressão concluída", NotificationManager.IMPORTANCE_DEFAULT));
        }
    }

    static void start(Context context, CompressionJob job) {
        ContextCompat.startForegroundService(context, new Intent(context, CompressionService.class)
                .putExtra(CompressionJob.EXTRA_ID, job.id));
    }

    static int notificationId(String id) { return id.hashCode() & 0x7fffffff; }

    static String notice(Context context, CompressionJob job) {
        if (!job.running() || !job.data.optBoolean("extraCompression")) return "";
        return job.data.optBoolean("reducedSharpness")
                ? context.getString(R.string.webp_lower_sharpness_warning, job.data.optInt("currentFps", 12))
                : context.getString(R.string.webp_lower_quality_warning);
    }

    @Override public void onCreate() {
        super.onCreate();
        channels(this);
        PowerManager power = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getPackageName() + ":compression");
        wakeLock.setReferenceCounted(false);
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        latestStartId = startId;
        String id = intent == null ? null : intent.getStringExtra(CompressionJob.EXTRA_ID);
        CompressionJob job = CompressionJob.load(this, id);
        if (job == null || !job.running()) {
            if (runningId == null || runningId.equals(id)) stopSelf(startId);
            return START_NOT_STICKY;
        }
        if (id.equals(runningId)) return START_REDELIVER_INTENT;
        runningId = id;
        startForeground(notificationId(id), notification(job, false));
        worker.execute(() -> convert(job));
        return START_REDELIVER_INTENT;
    }

    private Notification notification(CompressionJob job, boolean finished) {
        Intent screen = new Intent(this, CropVideoActivity.class)
                .setData(Uri.parse("sticker-compression://job/" + job.id))
                .putExtra(CompressionJob.EXTRA_ID, job.id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent tap = PendingIntent.getActivity(this, notificationId(job.id), screen,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        String text = job.data.optString("message", "Preparando vídeo…");
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this,
                finished ? FINISHED_CHANNEL : PROGRESS_CHANNEL)
                .setSmallIcon(R.drawable.ic_compression_notification)
                .setContentTitle(finished ? (job.done() ? "Figurinha pronta" : "cancelled".equals(job.data.optString("status"))
                        ? "Compressão cancelada" : "Não foi possível comprimir")
                        : "Criando figurinha")
                .setContentText(text).setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(text))
                .setContentIntent(tap).setOngoing(!finished).setAutoCancel(finished)
                .setOnlyAlertOnce(!finished).setCategory(NotificationCompat.CATEGORY_PROGRESS);
        if (!finished) builder.setProgress(100, job.data.optInt("progress"), job.data.optInt("progress") <= 0);
        return builder.build();
    }

    private void publish(CompressionJob job) throws IOException {
        job.save();
        if (!destroyed) getSystemService(NotificationManager.class)
                .notify(notificationId(job.id), notification(job, false));
    }

    private void runSearch(CompressionJob job) throws Exception {
            checkControl(job);
            // Keep both original media and preview outside cache: reopening Splash clears cache/temp.
            if (!job.data.optBoolean("inputsCopied")) {
                File preview = new File(job.data.getString("preview"));
                File durablePreview = new File(job.directory, "preview.mp4");
                CompressionJob.copy(preview, durablePreview);
                if (job.data.has("source")) {
                    File source = new File(job.data.getString("source"));
                    File durableSource = new File(job.directory, "original.gif");
                    CompressionJob.copy(source, durableSource);
                    job.data.put("source", durableSource.getAbsolutePath());
                }
                job.data.put("preview", durablePreview.getAbsolutePath());
                job.data.put("inputsCopied", true);
                job.save();
            }
            boolean gif = job.data.has("source");
            File source = new File(job.data.getString(gif ? "source" : "preview"));
            int quality = Integer.parseInt(job.data.getString("quality"));
            int fps = Integer.parseInt(job.data.getString("fps"));
            int compression = Integer.parseInt(job.data.getString("compression"));
            double speed = Double.parseDouble(job.data.getString("speed").replace(',', '.'));
            int detailSize = job.data.optInt("detailSize", 512);
            new AdaptiveWebpSearch(quality, fps);
            if (compression < 0 || compression > 6 || !Double.isFinite(speed) || speed <= 0)
                throw new IOException("Use velocidade maior que zero e compressão de 0 a 6.");
            MediaInformation info = FFprobeKit.getMediaInformation(source.getAbsolutePath()).getMediaInformation();
            if (info == null) throw new IOException("Não foi possível ler o vídeo.");
            StreamInformation stream = info.getStreams().stream().filter(s -> "video".equals(s.getType()))
                    .findFirst().orElseThrow(() -> new IOException("Stream de vídeo não encontrado."));
            double sourceDuration = Double.parseDouble(info.getDuration());
            long start = job.data.optLong("start"), end = job.data.optLong("end");
            if (gif && (start < 0 || end <= start || end > Math.round(sourceDuration * 1000)))
                throw new IOException("Intervalo do GIF inválido. Selecione o trecho novamente.");
            double duration = (gif ? (end - start) / 1000.0 : sourceDuration) / speed;
            job.data.put("sourceSeconds", gif ? (end - start) / 1000.0 : sourceDuration);
            if (!Double.isFinite(duration) || duration <= 0 || duration > 10)
                throw new IOException("Selecione um trecho de até 10 segundos na velocidade escolhida.");
            int square = Math.max(stream.getWidth().intValue(), stream.getHeight().intValue());
            android.graphics.Rect crop = job.crop(square);
            int baseX = Math.max(0, crop.left), baseY = Math.max(0, crop.top);
            int baseW = Math.min(square - baseX, crop.width()), baseH = Math.min(square - baseY, crop.height());
            if (baseW < 2 || baseH < 2) throw new IOException("Selecione uma área maior para recortar.");
            int initialFps = gif ? fps : AdaptiveWebpSearch.sourceLimitedFps(fps, stream.getAverageFrameRate(), speed);
            JSONArray results = job.data.getJSONArray("results");
            for (int quadrant = results.length(); quadrant < job.count(); quadrant++) {
                checkControl(job);
                AdaptiveWebpSearch search = new AdaptiveWebpSearch(quality, initialFps, detailSize);
                JSONArray trials = job.data.getJSONArray("trials");
                // Replay only committed trials: an interrupted encode is repeated with the same settings.
                for (int i = 0; i < trials.length(); i++) search.recordSize(trials.getLong(i));
                File best = job.result(quadrant);
                File candidate = new File(job.directory, "candidate.webp");
                while (!search.isDone()) {
                    checkControl(job);
                    int w = job.count() == 1 ? baseW : baseW / 2;
                    int h = job.count() == 1 ? baseH : baseH / 2;
                    int x = baseX + (job.count() == 1 ? 0 : (quadrant % 2) * w);
                    int y = baseY + (job.count() == 1 ? 0 : (quadrant / 2) * h);
                    String timeline = gif ? GifMedia.timelineFilter(start, end, speed, search.getFps())
                            : String.format(Locale.US, "setpts=(PTS-STARTPTS)/%.8f,fps=%d,", speed, search.getFps());
                    String filter = timeline + String.format(Locale.US,
                            "format=rgba,pad=%d:%d:(ow-iw)/2:(oh-ih)/2:color=#00000000,crop=%d:%d:%d:%d,",
                            square, square, w, h, x, y) + GifMedia.outputScaleFilter(search.getDetailSize())
                            + "pad=512:512:(ow-iw)/2:(oh-ih)/2:color=#00000000";
                    String detail = String.format(Locale.US, "Figurinha %d/%d • tentativa %d • %d FPS • qualidade %d",
                            quadrant + 1, job.count(), job.data.optInt("attemptCount") + 1, search.getFps(), search.getQuality());
                    int number = job.data.optInt("attemptCount") + 1;
                    job.data.put("attemptCount", number);
                    job.data.put("currentAttempt", new JSONObject().put("number", number).put("quadrant", quadrant + 1)
                            .put("quality", search.getQuality()).put("fps", search.getFps()).put("compression", compression)
                            .put("speed", speed).put("detailSize", search.getDetailSize()));
                    job.data.put("extraCompression", search.isExtraCompression());
                    job.data.put("reducedSharpness", search.getDetailSize() < 512);
                    job.data.put("currentFps", search.getFps());
                    if (search.isExtraCompression()) {
                        job.data.put("usedExtraCompression", true);
                        detail = (search.getDetailSize() < 512 ? "Reduzindo nitidez • " : "Redução extra de qualidade • ") + detail;
                    }
                    job.data.put("message", detail); job.data.put("progress", 0);
                    publish(job);
                    encode(job, GifMedia.webpArguments(source, gif, filter, search.getQuality(), compression, candidate), duration);
                    checkControl(job);
                    long bytes = candidate.length();
                    if (bytes == 0) throw new IOException("O conversor gerou um arquivo vazio.");
                    job.data.put("lastAttempt", new JSONObject(job.data.getJSONObject("currentAttempt").toString()).put("bytes", bytes));
                    if (search.recordSize(bytes)) CompressionJob.copy(candidate, best);
                    trials.put(bytes);
                    job.save();
                }
                candidate.delete();
                if (!search.hasResult()) throw new IOException("O arquivo excedeu 500 KB mesmo após reduzir qualidade e nitidez ao mínimo. Tente outro trecho.");
                String description = describe(best, search.getBestQuality());
                if (search.getDetailSize() < 512) description += "\nNitidez reduzida para atingir 500 KB";
                results.put(description);
                job.data.put("trials", new JSONArray());
                job.save();
            }
            synchronized (CompressionJob.class) {
            checkControl(job);
            job.data.put("status", "done");
            job.data.put("message", job.data.optBoolean("usedExtraCompression")
                    ? "Compressão concluída com qualidade reduzida. Toque para revisar e salvar."
                    : job.count() == 1 ? "Compressão concluída. Toque para revisar e salvar."
                    : "As 4 figurinhas estão prontas. Toque para revisar e salvar.");
            job.data.put("progress", 100);
            job.save();
            }
    }

    private static class RetryRequested extends Exception { }
    private static class CancelRequested extends Exception { }

    private void checkControl(CompressionJob job) throws Exception {
        ensureAlive();
        JSONObject command = job.control();
        if ("cancel".equals(command.optString("action"))) throw new CancelRequested();
        if (command.optInt("revision") > job.data.optInt("appliedRevision")) throw new RetryRequested();
    }

    private void applyRetry(CompressionJob job) throws Exception {
        // Re-read so cancellation wins over a retry that was queued just before it.
        JSONObject latest = job.control();
        if ("cancel".equals(latest.optString("action"))) throw new CancelRequested();
        CompressionSettings settings = new CompressionSettings(latest.getInt("quality"), latest.getInt("fps"),
                latest.getInt("compression"), latest.getDouble("speed"), latest.getInt("detailSize"));
        CompressionJob.putSettings(job.data, settings);
        job.data.put("appliedRevision", latest.getInt("revision")).put("trials", new JSONArray())
                .put("results", new JSONArray()).put("extraCompression", false).put("usedExtraCompression", false)
                .put("message", "Reiniciando com seus valores…").put("progress", 0);
        job.data.remove("currentAttempt");
        for (int i = 0; i < job.count(); i++) job.result(i).delete();
        job.save();
    }

    private void convert(CompressionJob job) {
        try {
            synchronized (wakeLock) { ensureAlive(); wakeLock.acquire(); }
            while (true) {
                try { runSearch(job); break; }
                catch (RetryRequested request) { applyRetry(job); }
            }
        } catch (CancelRequested cancelled) {
            try {
                job.data.put("status", "cancelled").put("message", "Compressão cancelada. Ajuste os valores para tentar novamente.")
                        .put("progress", 0);
                new File(job.directory, "candidate.webp").delete();
                job.save();
            } catch (Exception e) { android.util.Log.e("CompressionService", "Falha ao salvar cancelamento", e); }
        } catch (Exception e) {
            if (destroyed) return; // Leave the durable record running; redelivery resumes it.
            android.util.Log.e("CompressionService", "Falha na compressão", e);
            try {
                job.data.put("status", "error");
                job.data.put("message", "Não foi possível criar a figurinha: " + e.getMessage());
                job.save();
            } catch (Exception writeError) { android.util.Log.e("CompressionService", "Falha ao salvar estado", writeError); }
        } finally {
            releaseWakeLock();
            main.post(() -> {
                if (destroyed) return;
                boolean lastJob = job.id.equals(runningId);
                if (lastJob) stopForeground(true);
                if (CompressionJob.load(this, job.id) != null) {
                    getSystemService(NotificationManager.class).notify(notificationId(job.id), notification(job, true));
                }
                if (lastJob) stopSelf(latestStartId);
            });
        }
    }

    private void encode(CompressionJob job, String[] arguments, double duration) throws Exception {
        CountDownLatch complete = new CountDownLatch(1);
        AtomicReference<FFmpegSession> result = new AtomicReference<>();
        AtomicInteger progress = new AtomicInteger();
        FFmpegSession session = FFmpegKit.executeWithArgumentsAsync(arguments, s -> {
            result.set(s); complete.countDown();
        }, null, statistics -> {
            progress.set(Math.min(99, Math.max(0, (int) (statistics.getTime() / (duration * 10)))));
        });
        sessionId = session.getSessionId();
        if (destroyed) FFmpegKit.cancel(sessionId);
        int ticks = 0;
        try {
            while (!complete.await(100, TimeUnit.MILLISECONDS)) {
                checkControl(job);
                if (++ticks % 10 == 0) {
                    job.data.put("progress", progress.get());
                    publish(job);
                }
            }
            checkControl(job);
        } catch (Exception control) {
            if ((control instanceof RetryRequested || control instanceof CancelRequested) && complete.getCount() != 0)
                job.data.put("interruptedAttempts", job.data.optInt("interruptedAttempts") + 1);
            FFmpegKit.cancel(session.getSessionId());
            // Do not reuse candidate.webp until the interrupted native encoder has stopped.
            complete.await();
            throw control;
        } finally {
            sessionId = -1;
        }
        ensureAlive();
        if (!ReturnCode.isSuccess(result.get().getReturnCode())) {
            android.util.Log.e("CompressionService", result.get().getAllLogsAsString());
            throw new IOException("Tente outro trecho do vídeo.");
        }
    }

    private void ensureAlive() throws InterruptedException {
        if (destroyed || Thread.currentThread().isInterrupted()) throw new InterruptedException();
    }

    private static String describe(File file, int quality) throws IOException {
        byte[] bytes;
        try (FileInputStream input = new FileInputStream(file); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192]; int n;
            while ((n = input.read(buffer)) != -1) output.write(buffer, 0, n);
            bytes = output.toByteArray();
        }
        WebPImage image = WebPImage.createFromByteArray(bytes, ImageDecodeOptions.defaults());
        try {
            if (bytes.length > AdaptiveWebpSearch.MAX_BYTES || image.getWidth() != 512 || image.getHeight() != 512
                    || image.getDuration() <= 0 || image.getDuration() > StickerPackValidator.ANIMATED_STICKER_TOTAL_DURATION_MAX
                    || image.getFrameCount() < 2) throw new IOException("Resultado fora dos limites de uma figurinha animada.");
            for (int ms : image.getFrameDurations()) {
                if (ms < StickerPackValidator.ANIMATED_STICKER_FRAME_DURATION_MIN) throw new IOException("Quadro curto demais.");
            }
            return String.format(Locale.US, "FPS médio: %.1f\nDuração: %.2fs\nTamanho: %.2f KB\nQualidade: %d",
                    image.getFrameCount() * 1000.0 / image.getDuration(), image.getDuration() / 1000.0,
                    file.length() / 1024.0, quality);
        } finally { image.dispose(); }
    }

    @Override public void onDestroy() {
        destroyed = true;
        if (sessionId >= 0) FFmpegKit.cancel(sessionId);
        worker.shutdownNow();
        releaseWakeLock();
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }

    private void releaseWakeLock() {
        synchronized (wakeLock) {
            if (wakeLock.isHeld()) wakeLock.release();
        }
    }
}
