package com.example.samplestickerapp;

import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.os.Build;
import android.provider.MediaStore;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.function.Consumer;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.ViewHolder> {

    private List<File> items;
    private OnClickListener onClickListener;

    public FileAdapter(List<File> items, OnClickListener onClickListener) {
        this.items = items;
        this.onClickListener = onClickListener;
    }

    public void updateList(List<File> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_file, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder h, int pos) {
        File file = items.get(pos);

        h.container.setOnClickListener(v -> onClickListener.onClickListener(file));

        try {
            String nome = file.getName().split("\\.")[0]
                    .replace("mp4_", "").replace("_", " ");
            h.name.setText(nome);
        } catch (Exception e) {
            h.name.setText(file.getName());
        }
        try {
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(file.getAbsolutePath());
            long durationMs = Long.parseLong(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
            h.textDuracao.setText((durationMs / 1000) + "s");

            String sizeStr = Formatter.formatShortFileSize(h.container.getContext(), file.length());
            h.textSize.setText(sizeStr);

            String mime = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE);
            String formatStr = mime != null && mime.contains("/") ? mime.substring(mime.indexOf('/') + 1).toUpperCase() : "N/A";
            h.textFormat.setText(formatStr);
            //h.textFormat.setVisibility(View.GONE);

            String width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
            String height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
            h.textResolucao.setText(width + "×" + height);

            String isoDate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE);
            if (isoDate != null) {
                SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
                parser.setTimeZone(TimeZone.getTimeZone("GMT"));
                Date date = parser.parse(isoDate);
                String dateStr = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date);
                h.textDate.setText(dateStr);
            } else {
                h.textDate.setText("—");
            }
            retriever.release();

            return;
        } catch (Exception e) {
        }

        if (file.isDirectory()) {
            h.thumb.setImageResource(R.drawable.folder_xml);
        } else {
            String name = file.getName().toLowerCase();

            // Listas de extensões
            List<String> extensaoImagens = Arrays.asList(
                    ".jpg", ".jpeg", ".png", ".bmp", ".gif", ".webp", ".svg");
            List<String> extensaoAudios = Arrays.asList(
                    ".mp3", ".aac", ".wav", ".flac", ".ogg", ".m4a", ".wma");
            List<String> extensaoVideos = Arrays.asList(
                    ".mp4", ".webm", ".mkv", ".avi", ".mov", ".flv", ".wmv", ".3gp", ".ts");
            List<String> extensaoDocumentos = Arrays.asList(
                    ".pdf", ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx", ".txt", ".rtf");
            List<String> extensaoCompactados = Arrays.asList(
                    ".zip", ".rar", ".7z", ".tar", ".gz", ".bz2", ".xz");
            List<String> extensaoApk = Collections.singletonList(
                    ".apk");
            List<String> extensaoCodigo = Arrays.asList(
                    ".java", ".kt", ".xml", ".html", ".css", ".js", ".json", ".csv", ".md");

            if (extensaoImagens.stream().anyMatch(name::endsWith)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    try {
                        Bitmap bm = ThumbnailUtils.createImageThumbnail(
                                file.getAbsolutePath(),
                                MediaStore.Images.Thumbnails.MINI_KIND
                        );
                        h.thumb.setImageBitmap(bm);
                    } catch (Exception e) {
                        h.thumb.setImageResource(R.drawable.icon_image);
                    }
                } else {
                    h.thumb.setImageResource(R.drawable.icon_image);
                }
            } else if (extensaoVideos.stream().anyMatch(name::endsWith)) {
                try {
                    Bitmap bm = ThumbnailUtils.createVideoThumbnail(
                            file.getAbsolutePath(),
                            MediaStore.Video.Thumbnails.MINI_KIND
                    );
                    h.thumb.setImageBitmap(bm);
                } catch (Exception e) {
                    h.thumb.setImageResource(R.drawable.icon_video);
                }
            } else if (extensaoAudios.stream().anyMatch(name::endsWith)) {
                h.thumb.setImageResource(R.drawable.icon_music);
            } else if (extensaoDocumentos.stream().anyMatch(name::endsWith)) {
                h.thumb.setImageResource(R.drawable.icon_document);
            } else if (extensaoCompactados.stream().anyMatch(name::endsWith)) {
                h.thumb.setImageResource(R.drawable.icon_archive);
            } else if (extensaoApk.stream().anyMatch(name::endsWith)) {
                h.thumb.setImageResource(R.drawable.icon_apk);
            } else if (extensaoCodigo.stream().anyMatch(name::endsWith)) {
                h.thumb.setImageResource(R.drawable.icon_code);
            } else {
                h.thumb.setImageResource(R.drawable.icon_unknown);
            }
        }

    }

    public interface OnClickListener {
        void onClickListener(File file);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        View container;
        ImageView thumb;
        TextView name;
        TextView textSize;
        TextView textFormat;
        TextView textResolucao;
        TextView textDate;
        TextView textDuracao;

        ViewHolder(View itemView) {
            super(itemView);
            container = itemView;
            thumb = itemView.findViewById(R.id.image_thumbnail);
            name = itemView.findViewById(R.id.text_filename);
            textSize = itemView.findViewById(R.id.text_size);
            textFormat = itemView.findViewById(R.id.text_format);
            textResolucao = itemView.findViewById(R.id.text_resolucao);
            textDate = itemView.findViewById(R.id.text_date);
            textDuracao = itemView.findViewById(R.id.text_duracao);
        }

        void bind(File file) {
            itemView.setTag(file);
        }
    }
}
