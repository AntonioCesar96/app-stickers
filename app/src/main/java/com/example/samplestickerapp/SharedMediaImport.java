package com.example.samplestickerapp;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.util.UUID;

/** Copies a granted stream; never attempts to resolve a provider URI to a filesystem path. */
final class SharedMediaImport {
    final File file;
    final String kind, name;

    SharedMediaImport(File file, String kind, String name) {
        this.file = file; this.kind = kind; this.name = name;
    }

    static SharedMediaImport receive(Context context, Intent intent) throws IOException {
        if (!Intent.ACTION_SEND.equals(intent.getAction())) throw new IOException("Compartilhe uma foto ou um vídeo por vez.");
        Uri uri;
        try { uri = intent.getParcelableExtra(Intent.EXTRA_STREAM); }
        catch (RuntimeException invalid) { throw new IOException("O compartilhamento não contém um arquivo válido.", invalid); }
        if (uri == null && intent.getClipData() != null && intent.getClipData().getItemCount() == 1)
            uri = intent.getClipData().getItemAt(0).getUri();
        if (uri == null || !"content".equals(uri.getScheme()))
            throw new IOException("Compartilhe o arquivo de mídia, em vez de um link ou caminho.");
        String mime = context.getContentResolver().getType(uri);
        if (mime == null || (!mime.startsWith("image/") && !mime.startsWith("video/"))) mime = intent.getType();
        if (mime == null || (!mime.startsWith("image/") && !mime.startsWith("video/")))
            throw new IOException("Este compartilhamento não é uma foto ou um vídeo.");
        String name = "Mídia compartilhada";
        try (Cursor cursor = context.getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) name = cursor.getString(0);
        } catch (RuntimeException ignored) { /* The display name is optional; access to the bytes is not. */ }
        File directory = new File(context.getFilesDir(), "shared_imports");
        if (!directory.isDirectory() && !directory.mkdirs()) throw new IOException("Não foi possível preparar o arquivo compartilhado.");
        String suffix = mime.startsWith("video/") ? ".mp4" : "image/jpeg".equals(mime) ? ".jpg" : ".png";
        File file = new File(directory, UUID.randomUUID() + suffix);
        boolean success = false;
        try {
            try (InputStream input = context.getContentResolver().openInputStream(uri);
                 FileOutputStream output = new FileOutputStream(file)) {
                if (input == null) throw new IOException("O aplicativo de origem não disponibilizou o arquivo.");
                byte[] buffer = new byte[65536]; int count;
                while ((count = input.read(buffer)) != -1) {
                    if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("Importação interrompida.");
                    output.write(buffer, 0, count);
                }
                output.getFD().sync();
            }
            if (file.length() == 0) throw new IOException("O arquivo compartilhado está vazio.");
            byte[] signature = new byte[6];
            int read;
            try (FileInputStream input = new FileInputStream(file)) { read = input.read(signature); }
            boolean gif = read == 6 && signature[0] == 'G' && signature[1] == 'I' && signature[2] == 'F'
                    && signature[3] == '8' && (signature[4] == '7' || signature[4] == '9') && signature[5] == 'a';
            if (gif) {
                File renamed = new File(directory, UUID.randomUUID() + ".gif");
                if (!file.renameTo(renamed)) throw new IOException("Não foi possível preparar o GIF.");
                file = renamed;
            }
            success = true;
            return new SharedMediaImport(file, gif ? "gif" : mime.startsWith("video/") ? "video" : "image", name);
        } finally { if (!success) file.delete(); }
    }
}
