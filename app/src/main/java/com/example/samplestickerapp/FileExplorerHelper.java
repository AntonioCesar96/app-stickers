package com.example.samplestickerapp;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Movie;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

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
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class FileExplorerHelper {

    private AppCompatActivity activity;
    private StickerPack stickerPack;

    public FileExplorerHelper(AppCompatActivity activity, StickerPack stickerPack) {
        this.activity = activity;
        this.stickerPack = stickerPack;
    }

    public void extracted(File file, Class classs) {
        if (file.getName().contains(".mp4")) {
            AlertDialogHelper.showAlertDialog(activity, "",
                    "Vídeos muito grandes ou com qualidade muito alta precisam ser comprimidos para que a figurinha seja criada " +
                            "do jeito que o whatsapp aceita(até 500kb). \n" +
                            "Gostaria de passar esse video em nosso processamento ou usa-lo como está?", "Usar como está", "Processar",
                    () -> {
                        try {
                            MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                            retriever.setDataSource(file.getAbsolutePath());
                            long durationMs = Long.parseLong(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
                            retriever.release();

                            Intent intent = new Intent(activity, classs);
                            if (durationMs > 8000)
                                intent = new Intent(activity, CustomVideoRangeActivity.class);

                            intent.putExtra("sticker_pack", stickerPack);
                            intent.putExtra("file_path", file.getAbsolutePath());
                            activity.startActivity(intent);
                            return;
                        } catch (Exception e) {

                        }
                        extracted2(file, classs);
                    }, () -> {
                        extracted2(file, classs);
                    });
            return;
        }

        extracted2(file, classs);
    }

    public void extracted2(File file, Class classs) {
        LayoutInflater inflater = LayoutInflater.from(activity);
        View customTitleView = inflater.inflate(R.layout.custom_dialog_title, null);
        ImageButton closeButton = customTitleView.findViewById(R.id.closeButton);

        View dialogView = inflater.inflate(R.layout.dialog_with_spinner, null);

        Spinner spinner = dialogView.findViewById(R.id.dialog_spinner);

        Integer[] options = {512, 450, 400, 350, 300, 250, 200, 150, 100, 50};
        ArrayAdapter<Integer> adapter = new ArrayAdapter<>(
                activity,
                android.R.layout.simple_spinner_item,
                options
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setCustomTitle(customTitleView)
                .setCancelable(false)
                .setView(dialogView)
                .setPositiveButton("Processar", (dialogInterface, which) -> {

                    Integer resolucao = (Integer) spinner.getSelectedItem();

                    extractedComResolucao(file, classs, resolucao);
                })
                .setNegativeButton("Fechar", (dialogInterface, which) -> {
                    dialogInterface.dismiss();
                })
                .create();

        closeButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.BLACK);
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.BLACK);
    }

    private void extractedComResolucao(File file, Class classs, Integer resolucao) {
        String inputPath = file.getAbsolutePath();

        String[] split = inputPath.split("/");
        String nomeArquivoOriginalTemp = split[split.length - 1].replace(".", "_") + "_" + resolucao + ".mp4";

        File outputFile = new File(FilesHelper.getMp4Dir(), nomeArquivoOriginalTemp);
        String outputPath = outputFile.getAbsolutePath();
        if (outputFile.exists()) {
            try {
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                retriever.setDataSource(outputPath);
                long durationMs = Long.parseLong(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
                retriever.release();

                AlertDialogHelper.showAlertDialog(activity, "",
                        "Temos esse video já processado, deseja reutiliza-lo ou processa-lo novamente?", "Reutilizar", "Processar",
                        () -> {
                            processar(file, classs, resolucao);
                        }, () -> {
                            outputFile.delete();
                            processar(file, classs, resolucao);
                        });
                return;
            } catch (Exception e) {
            }
        }

        processar(file, classs, resolucao);
    }

    private void processar(File file, Class classs, Integer resolucao) {
        String inputPath = file.getAbsolutePath();

        String[] split = inputPath.split("/");
        String nomeArquivoOriginalTemp = split[split.length - 1].replace(".", "_") + "_" + resolucao + ".mp4";

        File outputFile = new File(FilesHelper.getMp4Dir(), nomeArquivoOriginalTemp);
        String outputPath = outputFile.getAbsolutePath();
        if (outputFile.exists()) {
            try {
                MediaMetadataRetriever retriever = new MediaMetadataRetriever();
                retriever.setDataSource(outputPath);
                long durationMs = Long.parseLong(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
                retriever.release();

                Intent intent = new Intent(activity, classs);
                if (durationMs > 8000)
                    intent = new Intent(activity, CustomVideoRangeActivity.class);

                intent.putExtra("sticker_pack", stickerPack);
                intent.putExtra("file_path", outputPath);
                activity.startActivity(intent);
                return;
            } catch (Exception e) {
                outputFile.delete();
                outputFile = new File(FilesHelper.getMp4Dir(), nomeArquivoOriginalTemp);
            }
        }

        List<String> extensaoImagens = Arrays.asList(
                ".gif", ".webp");

        int width = 0;
        long durationMs = 0;

        if (extensaoImagens.stream().anyMatch(inputPath::endsWith)) {
            // Tratamento de GIF
            if (inputPath.toLowerCase().endsWith(".gif")) {
                try (FileInputStream fis = new FileInputStream(inputPath)) {
                    Movie movie = Movie.decodeStream(fis);
                    width = movie.width();
                    durationMs = movie.duration();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                File tempMp4File = new File(FilesHelper.getMp4Dir(), split[split.length - 1].replace(".", "_") + "_gif.mp4");

                String ffmpegCommand = "-i \"" + inputPath + "\""
                        + " -vf \"scale=trunc(iw/2)*2:trunc(ih/2)*2\""
                        + " -c:v libx264 -crf 23 -preset fast -pix_fmt yuv420p -movflags +faststart -an "
                        + tempMp4File.getAbsolutePath();

                Session session = FFmpegKit.execute(ffmpegCommand);

                if (ReturnCode.isSuccess(session.getReturnCode())) {
                    inputPath = tempMp4File.getAbsolutePath();

                    if (durationMs <= 4000) {
                        Intent intent = new Intent(activity, classs);
                        intent.putExtra("sticker_pack", stickerPack);
                        intent.putExtra("file_path", inputPath);
                        activity.startActivity(intent);
                        return;
                    }
                } else {
                    Toast.makeText(activity,
                            "Falha na conversão de gif para mp4. Log: " + session.getAllLogsAsString(),
                            Toast.LENGTH_LONG).show();
                    Log.e("FFmpeg", "Falha na conversão. Log: " + session.getAllLogsAsString());
                    return;
                }

            }
            // Tratamento de WebP
            else if (inputPath.toLowerCase().endsWith(".webp")) {
                extractWebPFrames(nomeArquivoOriginalTemp, inputPath, activity, classs);
                return;
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

        String videoFilter = "-vf \"scale=" + resolucao + ":-2,fps=24\"";

        // Prepare FFmpeg command
        String ffmpegCommand = String.format(Locale.US,
                "-y -i \"%s\" %s -c:v libx264 -preset veryslow -b:v 500k -crf 28 -an \"%s\"",
                inputPath, videoFilter, outputPath
        );

        // Show non-cancelable progress dialog
        ProgressDialog progressDialog = new ProgressDialog(activity);
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
                    activity.runOnUiThread(() -> {
                        progressDialog.dismiss();
                        if (ReturnCode.isSuccess(session.getReturnCode())) {

                            Intent intent = new Intent(activity, classs);
                            if (durationMsFinal > 8000)
                                intent = new Intent(activity, CustomVideoRangeActivity.class);

                            intent.putExtra("sticker_pack", stickerPack);
                            intent.putExtra("file_path", outputPath);
                            activity.startActivity(intent);

                        } else {
                            Toast.makeText(activity,
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
                    activity.runOnUiThread(() -> progressDialog.setProgress(percent));
                }
        );
    }

    public void extractWebPFrames(String nomeArquivoOriginalTemp, String inputFilePath, Context context, Class classs) {
        // 2. Cria e configura o ProgressDialog
        ProgressDialog progressDialog = new ProgressDialog(context);
        progressDialog.setTitle("Processando o vídeo");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setCancelable(false);
        progressDialog.setMax(100);
        progressDialog.setProgress(0);
        progressDialog.show();

        new Thread(() -> {
            File outputDir = new File(FilesHelper.getTempDir(), "frames");
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            } else {
                ContentsJsonHelper.deleteRecursive(outputDir);
                outputDir = new File(FilesHelper.getTempDir(), "frames");
                outputDir.mkdirs();
            }

            // 2. Prepara o request com decode de todos os frames
            Uri uri = Uri.fromFile(new File(inputFilePath));
            ImageDecodeOptions decodeOptions = ImageDecodeOptions.newBuilder()
                    .setDecodeAllFrames(true)
                    .build();

            ImageRequest request = ImageRequestBuilder.newBuilderWithSource(uri)
                    .setImageDecodeOptions(decodeOptions)
                    .build();

            // 3. Submete à pipeline e registra um subscriber
            DataSource<CloseableReference<CloseableImage>> dataSource =
                    Fresco.getImagePipeline().fetchDecodedImage(request, /* callerContext */ null);

            File finalOutputDir = outputDir;
            dataSource.subscribe(new BaseDataSubscriber<CloseableReference<CloseableImage>>() {
                @Override
                protected void onNewResultImpl(DataSource<CloseableReference<CloseableImage>> ds) {
                    if (!ds.isFinished()) {
                        return;
                    }
                    CloseableReference<CloseableImage> imageRef = ds.getResult();
                    if (imageRef == null) {
                        return;
                    }
                    try {
                        CloseableImage ci = imageRef.get();
                        if (!(ci instanceof CloseableAnimatedImage)) {
                            // Não é um WebP animado
                            return;
                        }
                        CloseableAnimatedImage cai = (CloseableAnimatedImage) ci;
                        AnimatedImageResult animatedResult = cai.getImageResult();

                        // 4. Conta quantos frames existem
                        AnimatedImage image = animatedResult.getImage();
                        int frameCount = image.getFrameCount();
                        int totalDurationMs = image.getDuration();
                        int[] frameDurations = image.getFrameDurations();

                        // 5. Itera e salva cada frame
                        for (int i = 0; i < frameCount; i++) {
                            // getDecodedFrame só funciona se decodeAllFrames=true
                            CloseableReference<Bitmap> frameRef = animatedResult.getDecodedFrame(i);
                            if (frameRef != null) {
                                Bitmap frameBitmap = frameRef.get();
                                File outFile = new File(finalOutputDir, String.format("frame_%03d.png", i));
                                saveBitmapAsPng(frameBitmap, outFile);
                                CloseableReference.closeSafely(frameRef);
                            }

                            // calcula percentual e atualiza ProgressDialog na UI
                            int percent = (int) (((i + 1) / (float) frameCount) * 100);
                            activity.runOnUiThread(() -> {
                                progressDialog.setProgress(percent);
                            });
                        }

                        File frameTxtFile = new File(finalOutputDir, "frames.txt");
                        try (BufferedWriter bw = new BufferedWriter(new FileWriter(frameTxtFile))) {
                            for (int i = 0; i < frameCount; i++) {
                                String name = String.format("frame_%03d.png", i);
                                float durSec = frameDurations[i] / 1000f;
                                bw.write("file '" + name + "'\n");
                                bw.write("duration " + durSec + "\n");
                            }
                            // repetir o último frame sem duração para o FFmpeg não truncar
                            bw.write("file '" + String.format("frame_%03d.png", frameCount - 1) + "'\n");
                        } catch (IOException e) {
                            activity.runOnUiThread(() -> {
                                Toast.makeText(context, "Falha ao criar frames.txt: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                progressDialog.dismiss();
                            });
                        }

                        activity.runOnUiThread(() -> {
                            generateMp4FromFrames(nomeArquivoOriginalTemp, context, frameTxtFile, classs, totalDurationMs, frameCount);
                        });
                    } finally {
                        imageRef.close();

                        // Após finalizar, fecha o diálogo
                        activity.runOnUiThread(() -> {
                            progressDialog.dismiss();
                        });
                    }
                }

                @Override
                protected void onFailureImpl(DataSource<CloseableReference<CloseableImage>> ds) {
                    activity.runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(context, "Falha ao extrair frames", Toast.LENGTH_SHORT).show();
                    });
                }
            }, CallerThreadExecutor.getInstance());
        }).start();
    }

    private void saveBitmapAsPng(Bitmap bitmap, File outFile) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(outFile);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    public void generateMp4FromFrames(String nomeArquivoOriginalTemp, Context context,
                                      File frameTxtFile, Class classs, int totalDurationMs, int frameCount) {
        File outputFile = new File(FilesHelper.getMp4Dir(), nomeArquivoOriginalTemp);

        ProgressDialog progressDialog = new ProgressDialog(context);
        progressDialog.setTitle("Processando o vídeo");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setMax(100);
        progressDialog.setCancelable(false);
        progressDialog.show();

        String cmd = String.format(
                "-y -f concat -safe 0 -i \"%s\" " +
                        "-vf \"scale=512:-2\" " +
                        "-movflags +faststart " +
                        "-c:v libx264 -preset veryslow " +
                        "-pix_fmt yuv420p " +
                        "\"%s\"",
                frameTxtFile.getAbsolutePath(),
                outputFile.getAbsolutePath()
        );

        File finalOutputFile = outputFile;
        FFmpegKit.executeAsync(cmd,
                session -> {
                    ReturnCode code = session.getReturnCode();
                    activity.runOnUiThread(() -> {
                        progressDialog.dismiss();
                        if (ReturnCode.isSuccess(session.getReturnCode())) {
                            Intent intent = new Intent(activity, classs);
                            intent.putExtra("sticker_pack", stickerPack);
                            intent.putExtra("file_path", finalOutputFile.getAbsolutePath());
                            activity.startActivity(intent);
                        } else {
                            Toast.makeText(activity,
                                    "Falha ao processar vídeo. Veja o log para detalhes.",
                                    Toast.LENGTH_LONG).show();
                            Log.e("FFmpegKit",
                                    session.getAllLogsAsString());
                        }
                    });
                },
                session -> {
                },
                stats -> {
                    double elapsedMs = stats.getTime();
                    double percent = Math.min(100,
                            (elapsedMs / totalDurationMs) * 100);
                    activity.runOnUiThread(() -> progressDialog.setProgress((int) percent));
                }
        );
    }
}
