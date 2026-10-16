package com.example.samplestickerapp;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.util.AtomicFile;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.UUID;

/** Durable, versioned job records. No Activity, View or native session is retained here. */
final class CompressionJob {
    static final String EXTRA_ID = "compression_job_id";
    private static final String PREFS = "compression_jobs";
    final String id;
    final File directory;
    final JSONObject data;

    private CompressionJob(Context context, String id, JSONObject data) {
        if (id == null || !id.matches("[a-f0-9-]{36}")) throw new IllegalArgumentException("Tarefa inválida");
        this.id = id;
        this.directory = new File(new File(context.getFilesDir(), "compression_jobs"), id);
        this.data = data;
    }

    static synchronized CompressionJob create(Context context, Intent launch, Rect crop, int screenWidth,
                                              boolean exodia, String speed, String fps, String quality,
                                              String compression) throws Exception {
        CompressionJob pending = pending(context);
        if (pending != null && pending.running()) return pending;
        JSONObject data = new JSONObject();
        data.put("version", 1);
        data.put("status", "running");
        data.put("message", "Preparando vídeo…");
        data.put("preview", launch.getStringExtra("file_path"));
        data.put("source", launch.getStringExtra(GifMedia.SOURCE));
        data.put("start", launch.getLongExtra(GifMedia.START_MS, 0));
        data.put("end", launch.getLongExtra(GifMedia.END_MS, 0));
        data.put("width", launch.getIntExtra(GifMedia.WIDTH, 0));
        data.put("height", launch.getIntExtra(GifMedia.HEIGHT, 0));
        data.put("crop", new JSONArray(new double[]{crop.left / (double) screenWidth,
                crop.top / (double) screenWidth, crop.right / (double) screenWidth,
                crop.bottom / (double) screenWidth}));
        data.put("exodia", exodia);
        data.put("speed", speed);
        data.put("fps", fps);
        data.put("quality", quality);
        data.put("compression", compression);
        data.put("results", new JSONArray());
        data.put("trials", new JSONArray());
        StickerPack pack = launch.getParcelableExtra("sticker_pack");
        JSONObject p = new JSONObject();
        p.put("identifier", pack.identifier); p.put("name", pack.name); p.put("publisher", pack.publisher);
        p.put("tray", pack.trayImageFile); p.put("email", pack.publisherEmail);
        p.put("website", pack.publisherWebsite); p.put("privacy", pack.privacyPolicyWebsite);
        p.put("license", pack.licenseAgreementWebsite); p.put("version", pack.imageDataVersion);
        p.put("avoidCache", pack.avoidCache); p.put("animated", pack.animatedStickerPack);
        p.put("android", pack.androidPlayStoreLink); p.put("ios", pack.iosAppStoreLink);
        JSONArray stickers = new JSONArray();
        for (Sticker sticker : pack.getStickers()) {
            JSONObject s = new JSONObject();
            s.put("file", sticker.imageFileName);
            s.put("emojis", sticker.emojis == null ? new JSONArray() : new JSONArray(sticker.emojis));
            s.put("text", sticker.accessibilityText); s.put("size", sticker.size);
            stickers.put(s);
        }
        p.put("stickers", stickers); data.put("pack", p);
        CompressionJob job = new CompressionJob(context, UUID.randomUUID().toString(), data);
        job.save();
        context.getSharedPreferences(PREFS, 0).edit().putString("pending", job.id).commit();
        return job;
    }

    static synchronized CompressionJob load(Context context, String id) {
        try {
            CompressionJob job = new CompressionJob(context, id, null);
            AtomicFile file = new AtomicFile(new File(job.directory, "state.json"));
            return new CompressionJob(context, id, new JSONObject(new String(file.readFully(), StandardCharsets.UTF_8)));
        } catch (Exception e) { return null; }
    }

    static CompressionJob pending(Context context) {
        return load(context, context.getSharedPreferences(PREFS, 0).getString("pending", null));
    }

    static void consume(Context context, String id) {
        if (id != null && id.equals(context.getSharedPreferences(PREFS, 0).getString("pending", null))) {
            context.getSharedPreferences(PREFS, 0).edit().remove("pending").commit();
        }
        CompressionJob job = load(context, id);
        if (job != null && !job.running()) {
            // Only this UUID's flat, private job directory; the stickers already saved in
            // shared storage are independent copies and must never be removed here.
            File[] files = job.directory.listFiles();
            if (files != null) for (File file : files) if (file.isFile()) file.delete();
            job.directory.delete();
        }
    }

    synchronized void save() throws IOException {
        synchronized (CompressionJob.class) {
            if (!directory.isDirectory() && !directory.mkdirs()) throw new IOException("Sem espaço para a tarefa");
            AtomicFile file = new AtomicFile(new File(directory, "state.json"));
            FileOutputStream output = null;
            try {
                output = file.startWrite();
                output.write(data.toString().getBytes(StandardCharsets.UTF_8));
                file.finishWrite(output);
            } catch (IOException e) { file.failWrite(output); throw e; }
        }
    }

    boolean running() { return "running".equals(data.optString("status")); }
    boolean done() { return "done".equals(data.optString("status")); }
    boolean shouldRestoreAutomatically(Context context) {
        return !id.equals(context.getSharedPreferences(PREFS, 0).getString("dismissedEditor", null))
                && (running() || done());
    }

    void dismissEditor(Context context) {
        // Separate from service checkpoints so a concurrent encode cannot overwrite this choice.
        context.getSharedPreferences(PREFS, 0).edit().putString("dismissedEditor", id).commit();
    }
    int count() { return data.optBoolean("exodia") ? 4 : 1; }
    File result(int index) {
        return new File(directory, count() == 1 ? "output_cropped.webp" : "output_exodia_" + index + ".webp");
    }

    Rect crop(int width) throws JSONException {
        JSONArray c = data.getJSONArray("crop");
        return new Rect((int) Math.round(c.getDouble(0) * width), (int) Math.round(c.getDouble(1) * width),
                (int) Math.round(c.getDouble(2) * width), (int) Math.round(c.getDouble(3) * width));
    }

    Intent screen(Context context) throws JSONException {
        JSONObject p = data.getJSONObject("pack");
        StickerPack pack = new StickerPack(p.getString("identifier"), p.getString("name"), p.getString("publisher"),
                p.getString("tray"), p.optString("email"), p.optString("website"), p.optString("privacy"),
                p.optString("license"), p.optString("version"), p.optBoolean("avoidCache"), p.optBoolean("animated"));
        ArrayList<Sticker> stickers = new ArrayList<>();
        JSONArray array = p.getJSONArray("stickers");
        for (int i = 0; i < array.length(); i++) {
            JSONObject s = array.getJSONObject(i);
            ArrayList<String> emojis = new ArrayList<>();
            JSONArray emojiArray = s.getJSONArray("emojis");
            for (int j = 0; j < emojiArray.length(); j++) emojis.add(emojiArray.getString(j));
            stickers.add(new Sticker(s.getString("file"), emojis, s.optString("text"), s.optLong("size")));
        }
        pack.setStickers(stickers);
        pack.androidPlayStoreLink = p.optString("android"); pack.iosAppStoreLink = p.optString("ios");
        Intent intent = new Intent(context, CropVideoActivity.class).putExtra(EXTRA_ID, id)
                .putExtra("file_path", data.getString("preview")).putExtra("sticker_pack", pack)
                .putExtra(GifMedia.START_MS, data.optLong("start")).putExtra(GifMedia.END_MS, data.optLong("end"))
                .putExtra(GifMedia.WIDTH, data.optInt("width")).putExtra(GifMedia.HEIGHT, data.optInt("height"));
        if (data.has("source")) intent.putExtra(GifMedia.SOURCE, data.getString("source"));
        return intent;
    }

    static void copy(File source, File target) throws IOException {
        File partial = new File(target.getPath() + ".partial");
        try (FileInputStream input = new FileInputStream(source); FileOutputStream output = new FileOutputStream(partial)) {
            byte[] buffer = new byte[65536]; int n;
            while ((n = input.read(buffer)) != -1) output.write(buffer, 0, n);
            output.getFD().sync();
        }
        if (!partial.renameTo(target)) throw new IOException("Não foi possível salvar " + target.getName());
    }
}
