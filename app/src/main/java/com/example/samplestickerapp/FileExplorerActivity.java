package com.example.samplestickerapp;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.graphics.Movie;
import android.graphics.drawable.AnimatedImageDrawable;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.ReturnCode;
import com.arthenica.ffmpegkit.Session;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

public class FileExplorerActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "file_explorer_prefs";
    private static final String KEY_LAST_DIR = "last_opened_dir";

    private LinearLayout breadcrumbLayout;
    private RecyclerView recyclerView;
    private File currentDir;
    private File rootDir;
    private SharedPreferences prefs;
    private ProgressBar progressBar;

    private static final int MENU_TRIM = 1;
    private static final int MENU_CROP = 2;

    private View contextMenuAnchor;
    private StickerPack stickerPack;
    private File file;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_explorer);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Buscar");
        }

        // 1) Cria âncora invisível e centraliza:
        FrameLayout root = findViewById(android.R.id.content);
        contextMenuAnchor = new View(this);
        contextMenuAnchor.setId(View.generateViewId());
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                0, 0, Gravity.CENTER
        );
        root.addView(contextMenuAnchor, lp);
        registerForContextMenu(contextMenuAnchor);

        stickerPack = getIntent().getParcelableExtra("sticker_pack");
        boolean abrirDownload = getIntent().getBooleanExtra("abrir_download", false);

        breadcrumbLayout = findViewById(R.id.breadcrumbLayout);
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new FileAdapter(new ArrayList<>(), this::onItemClick));

        // Inicializa SharedPreferences
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Define o diretório raiz
        rootDir = Environment.getExternalStorageDirectory();
        if (abrirDownload) {
            File startDir = new File(rootDir, "Download");
            loadDirectory(startDir);
            return;
        }

        // Tenta recuperar o último diretório aberto
        String lastPath = prefs.getString(KEY_LAST_DIR, null);
        File startDir = null;
        if (lastPath != null) {
            File saved = new File(lastPath);
            if (saved.exists() && saved.isDirectory()) {
                startDir = saved;
            }
        }
        if (startDir == null) {
            startDir = rootDir;
        }

        // Carrega o diretório inicial
        loadDirectory(startDir);
    }

    private void loadDirectory(File dir) {
        currentDir = dir;

        progressBar.setVisibility(View.VISIBLE);

        // Salva o caminho do diretório atual em SharedPreferences
        prefs.edit()
                .putString(KEY_LAST_DIR, dir.getAbsolutePath())
                .apply();

        updateBreadcrumbs();

        File[] children = dir.listFiles();
        if (children == null) children = new File[0];

        List<File> list = Arrays.stream(children)
                .sorted((a, b) -> {
                    if (a.isDirectory() && !b.isDirectory()) return -1;
                    if (!a.isDirectory() && b.isDirectory()) return 1;
                    return a.getName().compareToIgnoreCase(b.getName());
                })
                .collect(Collectors.toList());

        ((FileAdapter) recyclerView.getAdapter()).updateList(list);

        progressBar.setVisibility(View.GONE);
    }

    private void updateBreadcrumbs() {
        breadcrumbLayout.removeAllViews();
        String rootPath = rootDir.getAbsolutePath();
        String path = currentDir.getAbsolutePath();
        String[] segments = path.split("/");
        StringBuilder acc = new StringBuilder();

        for (String segment : segments) {
            if (segment.isEmpty()) continue;
            acc.append("/").append(segment);
            final File segDir = new File(acc.toString());

            TextView tv = new TextView(this);
            tv.setText(segment);
            tv.setPadding(8, 0, 8, 0);
            tv.setOnClickListener(v -> {
                if (segDir.getAbsolutePath().startsWith(rootPath)) {
                    loadDirectory(segDir);
                } else {
                    Toast.makeText(this, "Diretório não disponível", Toast.LENGTH_SHORT).show();
                }
            });
            breadcrumbLayout.addView(tv);

            if (!acc.toString().equals(path)) {
                TextView sep = new TextView(this);
                sep.setText("›");
                sep.setPadding(4, 0, 4, 0);
                breadcrumbLayout.addView(sep);
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (!currentDir.equals(rootDir) && currentDir.getParentFile() != null) {
            loadDirectory(currentDir.getParentFile());
        } else {
            super.onBackPressed();
        }
    }

    private void onItemClick(File file) {
        if (file.isDirectory()) {
            loadDirectory(file);
        } else {
            String name = file.getName().toLowerCase();

            List<String> extensaoImagens = Arrays.asList(
                    ".gif", ".webp");
            List<String> extensaoVideos = Arrays.asList(
                    ".mp4", ".webm", ".mkv", ".avi", ".mov", ".flv", ".wmv", ".3gp", ".ts");

            // TODO: Usar esse arquivo na seguintes telas
            if (extensaoImagens.stream().anyMatch(name::endsWith) ||
                    extensaoVideos.stream().anyMatch(name::endsWith)) {
                this.file = file;
                openContextMenu(contextMenuAnchor);
            } else {
                Toast.makeText(this, "Arquivo não é um vídeo", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ContentsJsonHelper.stickerAlteradoTelaCriar != null) {
            finish();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.setHeaderTitle("Opções");
        menu.add(0, MENU_TRIM, 0, "Aparar(Trim)");
        menu.add(0, MENU_CROP, 1, "Cortar(Crop)");
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case MENU_TRIM:
                extracted(CustomVideoRangeActivity.class);
                return true;
            case MENU_CROP:
                extracted(CropVideoActivity.class);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    private void extracted(Class classs) {
        String inputPath = file.getAbsolutePath();

        File outputFile = new File(Environment.getExternalStorageDirectory(), "00-Figurinhas/temp/video_original_reduzido.mp4");
        String outputPath = outputFile.getAbsolutePath();
        if (outputFile.exists()) {
            outputFile.delete();
            outputFile = new File(Environment.getExternalStorageDirectory(), "00-Figurinhas/temp/video_original_reduzido.mp4");
        }

        List<String> extensaoImagens = Arrays.asList(
                ".gif", ".webp");

        int width = 0;
        long durationMs = 0;

        if (extensaoImagens.stream().anyMatch(inputPath::endsWith)) {
            // Tratamento de GIF
            if (inputPath.toLowerCase(Locale.US).endsWith(".gif")) {
                try (FileInputStream fis = new FileInputStream(inputPath)) {
                    Movie movie = Movie.decodeStream(fis);
                    width = movie.width();            // largura do GIF :contentReference[oaicite:4]{index=4}
                    durationMs = movie.duration();    // duração em ms do GIF :contentReference[oaicite:5]{index=5}
                } catch (IOException e) {
                    e.printStackTrace();
                }

                File tempDir = new File(Environment.getExternalStorageDirectory(), "00-Figurinhas/temp");
                File tempMp4File = new File(tempDir, "video_original_gif.mp4");
                if (tempMp4File.exists()) {
                    tempMp4File.delete();
                    tempMp4File = new File(tempDir, "video_original_gif.mp4");
                }

                String ffmpegCommand = "-i " + inputPath
                        + " -vf \"scale=trunc(iw/2)*2:trunc(ih/2)*2\""
                        + " -c:v libx264 -crf 23 -preset fast -pix_fmt yuv420p -movflags +faststart -an "
                        + tempMp4File.getAbsolutePath();

                Session session = FFmpegKit.execute(ffmpegCommand);

                if (ReturnCode.isSuccess(session.getReturnCode())) {
                    inputPath = tempMp4File.getAbsolutePath();

                    if (durationMs <= 4000) {
                        Intent intent = new Intent(this, classs);
                        intent.putExtra("sticker_pack", stickerPack);
                        intent.putExtra("file_path", inputPath);
                        startActivity(intent);
                        return;
                    }
                } else {
                    Toast.makeText(this,
                            "Falha na conversão de gif para mp4. Log: " + session.getAllLogsAsString(),
                            Toast.LENGTH_LONG).show();
                    Log.e("FFmpeg", "Falha na conversão. Log: " + session.getAllLogsAsString());
                    return;
                }

            }
            // Tratamento de WebP
            else if (inputPath.toLowerCase(Locale.US).endsWith(".webp")) {
                // API 28+ suporta ImageDecoder com AnimatedImageDrawable
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    try {
                        ImageDecoder.Source src = ImageDecoder.createSource(new File(inputPath));
                        Drawable drawable = ImageDecoder.decodeDrawable(src);
                        width = drawable.getIntrinsicWidth();  // largura do WebP :contentReference[oaicite:6]{index=6}
                        if (drawable instanceof AnimatedImageDrawable) {
                            // AnimatedImageDrawable não expõe duração total
                            durationMs = 0;  // workaround; API não fornece getDuration() :contentReference[oaicite:7]{index=7}
                        } else {
                            // WebP estático
                            durationMs = 0;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                // Fallback API < 28: trata como estático
                else {
                    BitmapFactory.Options opts = new BitmapFactory.Options();
                    opts.inJustDecodeBounds = true;
                    BitmapFactory.decodeFile(inputPath, opts);
                    width = opts.outWidth;  // largura do WebP estático :contentReference[oaicite:8]{index=8}
                    durationMs = 0;
                }
            }

        } else {
            // Recupera dimensões originais
            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(inputPath);
            width = Integer.parseInt(
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
            int height = Integer.parseInt(
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
            durationMs = Long.parseLong(
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
            retriever.release();
        }


        // Build scale filter if needed
        String videoFilter = "";
        if (width > 512) {
            videoFilter = "-vf \"scale=512:-2,fps=20\"";
        } else {
            videoFilter = "-vf \"fps=20\"";
        }

        // Prepare FFmpeg command
        String ffmpegCommand = String.format(Locale.US,
                "-y -i \"%s\" %s -c:v libx264 -preset veryslow -b:v 500k -crf 28 -an \"%s\"",
                inputPath, videoFilter, outputPath
        );

        // Show non-cancelable progress dialog
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Processando o vídeo");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setIndeterminate(false);
        progressDialog.setMax(100);
        progressDialog.setCancelable(false);
        progressDialog.show();

        final long durationMsFinal = durationMs;

        // Execute FFmpegKit asynchronously with progress callback
        FFmpegKit.executeAsync(ffmpegCommand,
                session -> {
                    // Dismiss dialog and handle completion on UI thread
                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        if (ReturnCode.isSuccess(session.getReturnCode())) {
                            Intent intent = new Intent(this, classs);
                            intent.putExtra("sticker_pack", stickerPack);
                            intent.putExtra("file_path", outputPath);
                            startActivity(intent);
                        } else {
                            Toast.makeText(this,
                                    "Falha ao processar vídeo. Veja o log para detalhes.",
                                    Toast.LENGTH_LONG).show();
                            Log.e("FFmpegKit",
                                    session.getAllLogsAsString());
                        }
                    });
                },
                session -> { /* no-op log callback */ },
                statistics -> {
                    // Update progress
                    double timeMs = statistics.getTime();
                    int percent = (int) ((timeMs / (float) durationMsFinal) * 100);
                    runOnUiThread(() -> progressDialog.setProgress(percent));
                }
        );
    }
}
