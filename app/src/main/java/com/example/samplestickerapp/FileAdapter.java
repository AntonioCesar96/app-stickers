package com.example.samplestickerapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.MediaMetadataRetriever;
import android.media.ThumbnailUtils;
import android.os.Build;
import android.provider.MediaStore;
import android.text.format.Formatter;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class FileAdapter extends RecyclerView.Adapter<FileAdapter.ViewHolder> {

    private final List<File> items;
    private final OnClickListener onClickListener;
    private final ExecutorService executor;
    private final LruCache<String, Bitmap> thumbnailCache;
    private final Map<String, VideoInfo> infoCache;

    private boolean videosRecentes;
    private OnSelectionListener onSelectionListener;
    private boolean selectionMode = false;
    private final Set<Integer> selectedPositions = new HashSet<>();

    public interface OnSelectionListener {
        void onSelectionModeChanged(boolean active);

        void onSelectionCountChanged(int count);
    }

    public FileAdapter(List<File> items, boolean videosRecentes, OnClickListener onClickListener, OnSelectionListener onSelectionListener) {
        this.items = items;
        this.videosRecentes = videosRecentes;
        this.onClickListener = onClickListener;
        this.onSelectionListener = onSelectionListener;
        // Thread pool para carregar dados em background
        this.executor = Executors.newFixedThreadPool(4);

        // Cache de thumbnails: usa até 1/8 da memória disponível
        final int maxMem = (int) (Runtime.getRuntime().maxMemory() / 1024);
        final int cacheSize = maxMem / 8;
        this.thumbnailCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bmp) {
                return bmp.getByteCount() / 1024;
            }
        };

        // Cache simples de metadados
        this.infoCache = new ConcurrentHashMap<>();
    }

    public void updateList(List<File> newItems) {
        items.clear();
        items.addAll(newItems);
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
        String path = file.getAbsolutePath();

        if (!videosRecentes) {
            h.selectionOverlay.setVisibility(
                    selectedPositions.contains(pos) ? View.VISIBLE : View.GONE
            );

            h.container.setOnLongClickListener(v -> {
                if (!selectionMode) {
                    // entrar em modo seleção
                    selectionMode = true;
                    selectedPositions.add(pos);
                    notifyItemChanged(pos);
                    onSelectionListener.onSelectionModeChanged(true);
                    onSelectionListener.onSelectionCountChanged(selectedPositions.size());
                } else {
                    toggleSelection(pos);
                }
                return true;
            });

            h.container.setOnClickListener(v -> {
                if (selectionMode) {
                    toggleSelection(pos);
                } else {
                    onClickListener.onClickListener(file);
                }
            });
        } else {
            h.container.setOnClickListener(v -> {
                onClickListener.onClickListener(file);
            });
        }

        h.name.setText(getDisplayName(file));

        // Primeiro tenta recuperar metadados do cache
        VideoInfo cachedInfo = infoCache.get(path);
        if (cachedInfo != null) {
            applyInfoToHolder(h, cachedInfo);
        } else {
            // coloca placeholders
            h.textDuracao.setText("…");
            h.textSize.setText("…");
            h.textFormat.setText("…");
            h.textResolucao.setText("…");
            h.textDate.setText("…");
            // carrega em background
            executor.execute(() -> {
                VideoInfo info = extractFileInfo(h, file);
                infoCache.put(path, info);
                // atualiza UI na thread principal
                h.container.post(() -> applyInfoToHolder(h, info));
            });
        }

        // Depois, o thumbnail
        Bitmap thumb = thumbnailCache.get(path);
        if (thumb != null) {
            h.thumb.setImageBitmap(thumb);
        } else if (file.isDirectory()) {
            h.thumb.setImageResource(R.drawable.folder_xml);
        } else {
            // placeholder genérico
            h.thumb.setImageResource(R.drawable.icon_unknown);
            executor.execute(() -> {
                Bitmap bm = createThumbnail(file);
                if (bm != null) {
                    thumbnailCache.put(path, bm);
                    h.container.post(() -> h.thumb.setImageBitmap(bm));
                }
            });
        }
    }


    private void toggleSelection(int position) {
        if (selectedPositions.contains(position)) {
            selectedPositions.remove(position);
        } else {
            selectedPositions.add(position);
        }
        notifyItemChanged(position);
        onSelectionListener.onSelectionCountChanged(selectedPositions.size());
        if (selectedPositions.isEmpty()) {
            // sair do modo seleção
            selectionMode = false;
            onSelectionListener.onSelectionModeChanged(false);
        }
    }

    public void handleDelete(Context context) {
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("Confirmar exclusão")
                .setMessage("Tem certeza que deseja excluir " + selectedPositions.size() + " arquivo(s)?")
                .setPositiveButton("Sim", (d, which) -> {
                    deleteSelectedItems(context);
                })
                .setNegativeButton("Não", null)
                .show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.RED);
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.GRAY);
    }

    private void deleteSelectedItems(Context context) {
        if (selectedPositions.isEmpty()) return;
        List<Integer> toRemove = new ArrayList<>(selectedPositions);
        Collections.sort(toRemove, Collections.reverseOrder());

        for (int pos : toRemove) {
            File file = items.remove(pos);
            file.delete();
            notifyItemRemoved(pos);
            notifyItemRangeChanged(pos, items.size() - pos);
        }

        // reset selection
        selectedPositions.clear();
        selectionMode = false;
        onSelectionListener.onSelectionCountChanged(0);
        onSelectionListener.onSelectionModeChanged(false);
    }


    private String getDisplayName(File file) {
        String name = file.getName();
        try {
            name = name.split("\\.")[0]
                    .replace("mp4_", "")
                    .replace("_", " ");
        } catch (Exception ignored) {
        }
        return name;
    }

    private void applyInfoToHolder(ViewHolder h, VideoInfo info) {
        h.textDuracao.setText(info.durationSec + "s");
        h.textSize.setText(info.sizeStr);
        h.textFormat.setText(info.format);
        h.textResolucao.setText(info.resolution);
        h.textDate.setText(info.dateStr);
    }

    private VideoInfo extractFileInfo(ViewHolder holder, File file) {
        VideoInfo info = new VideoInfo();
        String path = file.getAbsolutePath();

        // Determina o tipo pelo sufixo do arquivo
        String lower = path.toLowerCase(Locale.getDefault());
        boolean isImage = lower.endsWith(".png") || lower.endsWith(".jpg")
                || lower.endsWith(".jpeg") || lower.endsWith(".gif")
                || lower.endsWith(".bmp") || lower.endsWith(".webp");
        try {
            if (isImage) {
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(path, opts);

                int width = opts.outWidth;
                int height = opts.outHeight;

                info.durationSec = 0;
                info.sizeStr = Formatter.formatShortFileSize(
                        holder.thumb.getContext(), file.length());

                String ext = "N/A";
                int dot = path.lastIndexOf('.');
                if (dot >= 0 && dot < path.length() - 1) {
                    ext = path.substring(dot + 1).toUpperCase(Locale.getDefault());
                }
                info.format = ext;
                info.resolution = width + "×" + height;
                info.dateStr = new SimpleDateFormat("dd/MM/yyyy",
                        Locale.getDefault()).format(new Date(file.lastModified()));
            } else {
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                try {
                    retriever.setDataSource(path);
                    long durMs = Long.parseLong(
                            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
                    info.durationSec = (int) (durMs / 1000);

                    info.sizeStr = Formatter.formatShortFileSize(
                            holder.thumb.getContext(), file.length());

                    String mime = retriever.extractMetadata(
                            MediaMetadataRetriever.METADATA_KEY_MIMETYPE);
                    info.format = (mime != null && mime.contains("/"))
                            ? mime.substring(mime.indexOf('/') + 1).toUpperCase(Locale.getDefault())
                            : "N/A";

                    String w = retriever.extractMetadata(
                            MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
                    String h = retriever.extractMetadata(
                            MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
                    info.resolution = w + "×" + h;

                    info.dateStr = new SimpleDateFormat("dd/MM/yyyy",
                            Locale.getDefault()).format(new Date(file.lastModified()));

                } catch (Exception e) {
                    // valores padrão em caso de erro
                    info.durationSec = 0;
                    info.sizeStr = "—";
                    info.format = "—";
                    info.resolution = "—";
                    info.dateStr = "—";
                } finally {
                    retriever.release();
                }
            }
        } catch (Exception e) {
            // valores padrão em caso de erro
            info.durationSec = 0;
            info.sizeStr = "—";
            info.format = "—";
            info.resolution = "—";
            info.dateStr = "—";
        }
        return info;
    }

    private Bitmap createThumbnail(File file) {
        String name = file.getName().toLowerCase();
        // decide tipo apenas por extensão curta
        if (name.endsWith(".jpg") || name.endsWith(".png")) {
            try {
                return ThumbnailUtils.createImageThumbnail(
                        file.getAbsolutePath(),
                        MediaStore.Images.Thumbnails.MINI_KIND);
            } catch (Exception ignored) {
            }
        } else if (name.endsWith(".mp4") || name.endsWith(".mkv")) {
            try {
                return ThumbnailUtils.createVideoThumbnail(
                        file.getAbsolutePath(),
                        MediaStore.Video.Thumbnails.MINI_KIND);
            } catch (Exception ignored) {
            }
        }
        // outros tipos podem usar ícones fixos
        return null;
    }

    public interface OnClickListener {
        void onClickListener(File file);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final View container;
        final View selectionOverlay;
        final ImageView thumb;
        final TextView name, textSize, textFormat, textResolucao,
                textDate, textDuracao;

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
            selectionOverlay = itemView.findViewById(R.id.selectionOverlay);
        }
    }

    // Classe auxiliar para armazenar metadados
    private static class VideoInfo {
        int durationSec;
        String sizeStr, format, resolution, dateStr;
    }
}
