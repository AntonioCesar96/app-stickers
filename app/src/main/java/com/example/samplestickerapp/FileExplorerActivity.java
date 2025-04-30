package com.example.samplestickerapp;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Movie;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
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


import com.facebook.common.executors.CallerThreadExecutor;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.BaseDataSubscriber;
import com.facebook.datasource.DataSource;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.animated.base.AnimatedImage;
import com.facebook.imagepipeline.animated.base.AnimatedImageResult;
import com.facebook.imagepipeline.common.ImageDecodeOptions;
import com.facebook.imagepipeline.image.CloseableAnimatedImage;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileExplorerActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "file_explorer_prefs";
    private static final String KEY_LAST_DIR = "last_opened_dir";

    private LinearLayout breadcrumbLayout;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;

    private static final int MENU_TRIM = 1;
    private static final int MENU_CROP = 2;

    private View contextMenuAnchor;
    private StickerPack stickerPack;
    private File file;
    boolean videosUsados;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_explorer);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Buscar");
        }

        // 1) Cria âncora invisível e centraliza:
//        FrameLayout root = findViewById(android.R.id.content);
//        contextMenuAnchor = new View(this);
//        contextMenuAnchor.setId(View.generateViewId());
//        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
//                0, 0, Gravity.CENTER
//        );
//        root.addView(contextMenuAnchor, lp);
//        registerForContextMenu(contextMenuAnchor);

        stickerPack = getIntent().getParcelableExtra("sticker_pack");
        videosUsados = getIntent().getBooleanExtra("videos_usados", false);

        breadcrumbLayout = findViewById(R.id.breadcrumbLayout);
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new FileAdapter(new ArrayList<>(), this::onItemClick));

        loadDirectory();
    }

    private void loadDirectory() {
        progressBar.setVisibility(View.VISIBLE);

        updateBreadcrumbs();

        File[] children;
        if (videosUsados) {
            RecentFileDao dao = new RecentFileDao(this);
            List<String> recents = dao.buscarTodos();

            children = recents.stream()
                    .map(File::new)
                    .toArray(File[]::new);
        } else {
            children = FilesHelper.getMp4Dir().listFiles();
            if (children == null) children = new File[0];
        }

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

        TextView sep = new TextView(this);
        sep.setText("Vídeos já usados por esse aplicativo");
        sep.setPadding(4, 0, 4, 0);
        breadcrumbLayout.addView(sep);
    }

    private void onItemClick(File file) {
        String name = file.getName().toLowerCase();

        // TODO: quando .webp avaliar se é estatico ou nao para saber qual tela abrir
        List<String> extensaoVideos = Arrays.asList(
                ".mp4", ".webm", ".mkv", ".avi", ".mov", ".flv", ".wmv", ".3gp", ".ts", ".gif", ".webp");
        if (extensaoVideos.stream().anyMatch(name::endsWith)) {
            this.file = file;
            FileExplorerHelper fileExplorerHelper = new FileExplorerHelper(this, stickerPack);
            AlertDialogHelper.showAlertDialog(this, "",
                    "O que deseja fazer?",
                    "Aparar(Trim)", "Cortar(Crop)",
                    () -> {
                        fileExplorerHelper.extracted(file, CustomVideoRangeActivity.class);
                    }, () -> {
                        fileExplorerHelper.extracted(file, CropVideoActivity.class);
                    });
            
            //openContextMenu(contextMenuAnchor);
            return;
        }

        List<String> extensaoImagens = Arrays.asList(
                ".jpg", ".jpeg", ".png", ".bmp", ".svg");
        if (extensaoImagens.stream().anyMatch(name::endsWith)) {
            this.file = file;
            Intent intent = new Intent(this, CropImageActivity.class);
            intent.putExtra("sticker_pack", stickerPack);
            intent.putExtra("file_path", file.getAbsolutePath());
            startActivity(intent);
            return;
        }

        Toast.makeText(this, "Não é possível criar uma figurinha a partir desse arquivo", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!ContentsJsonHelper.stickersAlterados.isEmpty()) {
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

//
//    @Override
//    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
//        super.onCreateContextMenu(menu, v, menuInfo);
//        menu.setHeaderTitle("Opções");
//        menu.add(0, MENU_TRIM, 0, "Aparar(Trim)");
//        menu.add(0, MENU_CROP, 1, "Cortar(Crop)");
//    }
//
//    @Override
//    public boolean onContextItemSelected(MenuItem item) {
//
//        switch (item.getItemId()) {
//            case MENU_TRIM:
//                extracted(CustomVideoRangeActivity.class);
//                return true;
//            case MENU_CROP:
//                extracted(CropVideoActivity.class);
//                return true;
//            default:
//                return super.onContextItemSelected(item);
//        }
//    }
//
//    private void extracted(Class classs) {
//        String inputPath = file.getAbsolutePath();
//
//        File outputFile = new File(FilesHelper.getTempDir(), "video_original_reduzido.mp4");
//        String outputPath = outputFile.getAbsolutePath();
//        if (outputFile.exists()) {
//            outputFile.delete();
//            outputFile = new File(FilesHelper.getTempDir(), "video_original_reduzido.mp4");
//        }
//
//        List<String> extensaoImagens = Arrays.asList(
//                ".gif", ".webp");
//
//        int width = 0;
//        long durationMs = 0;
//
//        if (extensaoImagens.stream().anyMatch(inputPath::endsWith)) {
//            // Tratamento de GIF
//            if (inputPath.toLowerCase().endsWith(".gif")) {
//                try (FileInputStream fis = new FileInputStream(inputPath)) {
//                    Movie movie = Movie.decodeStream(fis);
//                    width = movie.width();            // largura do GIF :contentReference[oaicite:4]{index=4}
//                    durationMs = movie.duration();    // duração em ms do GIF :contentReference[oaicite:5]{index=5}
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//
//                File tempMp4File = new File(FilesHelper.getTempDir(), "video_original_gif.mp4");
//                if (tempMp4File.exists()) {
//                    tempMp4File.delete();
//                    tempMp4File = new File(FilesHelper.getTempDir(), "video_original_gif.mp4");
//                }
//
//                String ffmpegCommand = "-i \"" + inputPath + "\""
//                        + " -vf \"scale=trunc(iw/2)*2:trunc(ih/2)*2\""
//                        + " -c:v libx264 -crf 23 -preset fast -pix_fmt yuv420p -movflags +faststart -an "
//                        + tempMp4File.getAbsolutePath();
//
//                Session session = FFmpegKit.execute(ffmpegCommand);
//
//                if (ReturnCode.isSuccess(session.getReturnCode())) {
//                    inputPath = tempMp4File.getAbsolutePath();
//
//                    if (durationMs <= 4000) {
//                        Intent intent = new Intent(this, classs);
//                        intent.putExtra("sticker_pack", stickerPack);
//                        intent.putExtra("file_path", inputPath);
//                        startActivity(intent);
//                        return;
//                    }
//                } else {
//                    Toast.makeText(this,
//                            "Falha na conversão de gif para mp4. Log: " + session.getAllLogsAsString(),
//                            Toast.LENGTH_LONG).show();
//                    Log.e("FFmpeg", "Falha na conversão. Log: " + session.getAllLogsAsString());
//                    return;
//                }
//
//            }
//            // Tratamento de WebP
//            else if (inputPath.toLowerCase().endsWith(".webp")) {
//                extractWebPFrames(inputPath, this, classs);
//                return;
//            }
//
//        } else {
//            // Recupera dimensões originais
//            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
//            retriever.setDataSource(inputPath);
//            width = Integer.parseInt(
//                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
//            int height = Integer.parseInt(
//                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
//            durationMs = Long.parseLong(
//                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
//            retriever.release();
//        }
//
//        String videoFilter = "-vf \"scale=512:-2,fps=24\"";
//
//        // Prepare FFmpeg command
//        String ffmpegCommand = String.format(Locale.US,
//                "-y -i \"%s\" %s -c:v libx264 -preset veryslow -b:v 500k -crf 28 -an \"%s\"",
//                inputPath, videoFilter, outputPath
//        );
//
//        // Show non-cancelable progress dialog
//        ProgressDialog progressDialog = new ProgressDialog(this);
//        progressDialog.setTitle("Processando o vídeo");
//        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
//        progressDialog.setIndeterminate(false);
//        progressDialog.setMax(100);
//        progressDialog.setCancelable(false);
//        progressDialog.show();
//
//        final long durationMsFinal = durationMs;
//
//        // Execute FFmpegKit asynchronously with progress callback
//        FFmpegKit.executeAsync(ffmpegCommand,
//                session -> {
//                    // Dismiss dialog and handle completion on UI thread
//                    runOnUiThread(() -> {
//                        progressDialog.dismiss();
//                        if (ReturnCode.isSuccess(session.getReturnCode())) {
//                            Intent intent = new Intent(this, classs);
//                            intent.putExtra("sticker_pack", stickerPack);
//                            intent.putExtra("file_path", outputPath);
//                            startActivity(intent);
//                        } else {
//                            Toast.makeText(this,
//                                    "Falha ao processar vídeo. Veja o log para detalhes.",
//                                    Toast.LENGTH_LONG).show();
//                            Log.e("FFmpegKit",
//                                    session.getAllLogsAsString());
//                        }
//                    });
//                },
//                session -> { /* no-op log callback */ },
//                statistics -> {
//                    // Update progress
//                    double timeMs = statistics.getTime();
//                    int percent = (int) ((timeMs / (float) durationMsFinal) * 100);
//                    runOnUiThread(() -> progressDialog.setProgress(percent));
//                }
//        );
//    }
//
//    public void extractWebPFrames(String inputFilePath, Context context, Class classs) {
//        // 2. Cria e configura o ProgressDialog
//        ProgressDialog progressDialog = new ProgressDialog(context);
//        progressDialog.setTitle("Processando o vídeo");
//        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
//        progressDialog.setCancelable(false);
//        progressDialog.setMax(100);
//        progressDialog.setProgress(0);
//        progressDialog.show();
//
//        new Thread(() -> {
//            File outputDir = new File(FilesHelper.getTempDir(), "frames");
//            if (!outputDir.exists()) {
//                outputDir.mkdirs();
//            } else {
//                ContentsJsonHelper.deleteRecursive(outputDir);
//                outputDir = new File(FilesHelper.getTempDir(), "frames");
//                outputDir.mkdirs();
//            }
//
//            // 2. Prepara o request com decode de todos os frames
//            Uri uri = Uri.fromFile(new File(inputFilePath));
//            ImageDecodeOptions decodeOptions = ImageDecodeOptions.newBuilder()
//                    .setDecodeAllFrames(true)
//                    .build();
//
//            ImageRequest request = ImageRequestBuilder.newBuilderWithSource(uri)
//                    .setImageDecodeOptions(decodeOptions)
//                    .build();
//
//            // 3. Submete à pipeline e registra um subscriber
//            DataSource<CloseableReference<CloseableImage>> dataSource =
//                    Fresco.getImagePipeline().fetchDecodedImage(request, /* callerContext */ null);
//
//            File finalOutputDir = outputDir;
//            dataSource.subscribe(new BaseDataSubscriber<CloseableReference<CloseableImage>>() {
//                @Override
//                protected void onNewResultImpl(DataSource<CloseableReference<CloseableImage>> ds) {
//                    if (!ds.isFinished()) {
//                        return;
//                    }
//                    CloseableReference<CloseableImage> imageRef = ds.getResult();
//                    if (imageRef == null) {
//                        return;
//                    }
//                    try {
//                        CloseableImage ci = imageRef.get();
//                        if (!(ci instanceof CloseableAnimatedImage)) {
//                            // Não é um WebP animado
//                            return;
//                        }
//                        CloseableAnimatedImage cai = (CloseableAnimatedImage) ci;
//                        AnimatedImageResult animatedResult = cai.getImageResult();
//
//                        // 4. Conta quantos frames existem
//                        AnimatedImage image = animatedResult.getImage();
//                        int frameCount = image.getFrameCount();
//                        int totalDurationMs = image.getDuration();
//                        int[] frameDurations = image.getFrameDurations();
//
//                        // 5. Itera e salva cada frame
//                        for (int i = 0; i < frameCount; i++) {
//                            // getDecodedFrame só funciona se decodeAllFrames=true
//                            CloseableReference<Bitmap> frameRef = animatedResult.getDecodedFrame(i);
//                            if (frameRef != null) {
//                                Bitmap frameBitmap = frameRef.get();
//                                File outFile = new File(finalOutputDir, String.format("frame_%03d.png", i));
//                                saveBitmapAsPng(frameBitmap, outFile);
//                                CloseableReference.closeSafely(frameRef);
//                            }
//
//                            // calcula percentual e atualiza ProgressDialog na UI
//                            int percent = (int) (((i + 1) / (float) frameCount) * 100);
//                            runOnUiThread(() -> {
//                                progressDialog.setProgress(percent);
//                            });
//                        }
//
//                        File frameTxtFile = new File(finalOutputDir, "frames.txt");
//                        try (BufferedWriter bw = new BufferedWriter(new FileWriter(frameTxtFile))) {
//                            for (int i = 0; i < frameCount; i++) {
//                                String name = String.format("frame_%03d.png", i);
//                                float durSec = frameDurations[i] / 1000f;
//                                bw.write("file '" + name + "'\n");
//                                bw.write("duration " + durSec + "\n");
//                            }
//                            // repetir o último frame sem duração para o FFmpeg não truncar
//                            bw.write("file '" + String.format("frame_%03d.png", frameCount - 1) + "'\n");
//                        } catch (IOException e) {
//                            runOnUiThread(() -> {
//                                Toast.makeText(context, "Falha ao criar frames.txt: " + e.getMessage(), Toast.LENGTH_SHORT).show();
//                                progressDialog.dismiss();
//                            });
//                        }
//
//                        runOnUiThread(() -> {
//                            generateMp4FromFrames(context, frameTxtFile, classs, totalDurationMs, frameCount);
//                        });
//                    } finally {
//                        imageRef.close();
//
//                        // Após finalizar, fecha o diálogo
//                        runOnUiThread(() -> {
//                            progressDialog.dismiss();
//                        });
//                    }
//                }
//
//                @Override
//                protected void onFailureImpl(DataSource<CloseableReference<CloseableImage>> ds) {
//                    runOnUiThread(() -> {
//                        progressDialog.dismiss();
//                        Toast.makeText(context, "Falha ao extrair frames", Toast.LENGTH_SHORT).show();
//                    });
//                }
//            }, CallerThreadExecutor.getInstance());
//        }).start();
//    }
//
//    private void saveBitmapAsPng(Bitmap bitmap, File outFile) {
//        FileOutputStream fos = null;
//        try {
//            fos = new FileOutputStream(outFile);
//            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
//        } catch (IOException e) {
//            e.printStackTrace();
//        } finally {
//            if (fos != null) {
//                try {
//                    fos.close();
//                } catch (IOException ignored) {
//                }
//            }
//        }
//    }
//
//    public void generateMp4FromFrames(Context context, File frameTxtFile, Class classs, int totalDurationMs, int frameCount) {
//        File outputFile = new File(FilesHelper.getTempDir(), "video_original_webp.mp4");
//        if (outputFile.exists()) {
//            outputFile.delete();
//            outputFile = new File(FilesHelper.getTempDir(), "video_original_webp.mp4");
//        }
//
//        ProgressDialog progressDialog = new ProgressDialog(context);
//        progressDialog.setTitle("Processando o vídeo");
//        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
//        progressDialog.setMax(100);
//        progressDialog.setCancelable(false);
//        progressDialog.show();
//
//        String cmd = String.format(
//                "-y -f concat -safe 0 -i \"%s\" " +
//                        "-vf \"scale=512:-2\" " +
//                        "-movflags +faststart " +
//                        "-c:v libx264 -preset veryslow " +
//                        "-pix_fmt yuv420p " +
//                        "\"%s\"",
//                frameTxtFile.getAbsolutePath(),
//                outputFile.getAbsolutePath()
//        );
//
//        File finalOutputFile = outputFile;
//        FFmpegKit.executeAsync(cmd,
//                session -> {
//                    ReturnCode code = session.getReturnCode();
//                    runOnUiThread(() -> {
//                        progressDialog.dismiss();
//                        if (ReturnCode.isSuccess(session.getReturnCode())) {
//                            Intent intent = new Intent(this, classs);
//                            intent.putExtra("sticker_pack", stickerPack);
//                            intent.putExtra("file_path", finalOutputFile.getAbsolutePath());
//                            startActivity(intent);
//                        } else {
//                            Toast.makeText(this,
//                                    "Falha ao processar vídeo. Veja o log para detalhes.",
//                                    Toast.LENGTH_LONG).show();
//                            Log.e("FFmpegKit",
//                                    session.getAllLogsAsString());
//                        }
//                    });
//                },
//                session -> {
//                },
//                stats -> {
//                    double elapsedMs = stats.getTime();
//                    double percent = Math.min(100,
//                            (elapsedMs / totalDurationMs) * 100);
//                    runOnUiThread(() -> progressDialog.setProgress((int) percent));
//                }
//        );
//    }
}
