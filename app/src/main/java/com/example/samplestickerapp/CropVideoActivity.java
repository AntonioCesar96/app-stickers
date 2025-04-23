package com.example.samplestickerapp;

import static com.example.samplestickerapp.StickerPackValidator.ANIMATED_STICKER_FILE_LIMIT_KB;
import static com.example.samplestickerapp.StickerPackValidator.ANIMATED_STICKER_FRAME_DURATION_MIN;
import static com.example.samplestickerapp.StickerPackValidator.ANIMATED_STICKER_TOTAL_DURATION_MAX;
import static com.example.samplestickerapp.StickerPackValidator.IMAGE_HEIGHT;
import static com.example.samplestickerapp.StickerPackValidator.IMAGE_WIDTH;
import static com.example.samplestickerapp.StickerPackValidator.KB_IN_BYTES;

import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.widget.ArrayAdapter;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.FrameLayout;

import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.FFprobeKit;
import com.arthenica.ffmpegkit.MediaInformation;
import com.arthenica.ffmpegkit.MediaInformationSession;
import com.arthenica.ffmpegkit.ReturnCode;
import com.arthenica.ffmpegkit.StreamInformation;
import com.facebook.animated.webp.WebPImage;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.common.ImageDecodeOptions;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class CropVideoActivity extends AppCompatActivity {
    private LockableScrollView lockableScrollView;
    private VideoView videoView, videoViewPreview;
    private SimpleDraweeView expandedStickerPreview;
    private FrameLayout videoContainer;
    private ProgressBar progressBarPreview, progressBarVideoView;
    private CropOverlayView cropOverlay;
    private StickerPack stickerPack;
    private String extensaoArquivoOriginal;
    private int videoOriginalWidth, videoOriginalHeight;
    File videoFile, fileCropped;
    ImageButton btnPause, btnSalvar, btnCrop, btnOpcoes;
    EditText inputVelocidade, inputVelocidadeFps, inputCompressao, inputQuality, inputDuracao;
    String velocidade = "1", velocidadeFps = "30", compression = "6", quality = "75", duracao = "0";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop_video);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Cortar(Crop)");
        }

        videoView = findViewById(R.id.video_view);
        videoView.setZOrderMediaOverlay(true);

        lockableScrollView = findViewById(R.id.lockableScrollView);
        videoViewPreview = findViewById(R.id.video_view_preview);
        progressBarPreview = findViewById(R.id.sticker_loader_preview);
        progressBarVideoView = findViewById(R.id.sticker_loader_video_view);
        cropOverlay = findViewById(R.id.crop_overlay);
        btnCrop = findViewById(R.id.btn_crop);
        btnPause = findViewById(R.id.btn_pause);
        btnSalvar = findViewById(R.id.btn_salvar);
        btnOpcoes = findViewById(R.id.btn_opcoes);
        videoContainer = findViewById(R.id.video_container);
        expandedStickerPreview = findViewById(R.id.sticker_details_expanded_sticker_preview);

        btnCrop.setOnClickListener(v -> {
            progressBarPreview.setVisibility(View.VISIBLE);

            convertMp4ToWebpAdaptive();

        });
        btnOpcoes.setOnClickListener(v -> openDialogOpcoesVideo());
        btnSalvar.setOnClickListener(v -> enviarWhatsapp());
        btnPause.setOnClickListener(v -> {
            if (videoView.isPlaying()) {
                videoView.pause();
                btnPause.setImageResource(R.drawable.ic_play);
            } else {
                videoView.start();
                btnPause.setImageResource(R.drawable.ic_pause);
            }
        });

        stickerPack = getIntent().getParcelableExtra("sticker_pack");
        videoFile = new File(Objects.requireNonNull(getIntent().getStringExtra("file_path")));

        videoView.setVideoPath(videoFile.getAbsolutePath());
        videoView.setOnPreparedListener(mp -> {
            mp.setLooping(true);

            videoOriginalWidth = mp.getVideoWidth();
            videoOriginalHeight = mp.getVideoHeight();

            videoView.start();
        });


        progressBarVideoView.setVisibility(View.VISIBLE);
        videoContainer.setVisibility(View.INVISIBLE);

        videoView.getViewTreeObserver().addOnPreDrawListener(
                new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        videoView.getViewTreeObserver().removeOnPreDrawListener(this);

                        // espera o vídeo renderizar...
                        new Handler().postDelayed(() -> setupContainerAndCrop(), 1000);

                        return true;
                    }
                }
        );

        cropOverlay.setOnOutsideCropClickListener((boolean flag) -> {
            lockableScrollView.setScrollingEnabled(flag);
        });
    }

    private void setupContainerAndCrop() {
        // 1) lado do quadrado = largura da tela
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int screenW = dm.widthPixels;

        // 2) calcula escala para manter proporção dentro do quadrado
        float aspect = (float) videoOriginalWidth / (float) videoOriginalHeight;
        int finalW, finalH;
        if (aspect > 1f) {
            finalW = screenW;
            finalH = Math.round(screenW / aspect);
        } else {
            finalH = screenW;
            finalW = Math.round(screenW * aspect);
        }

        // 3) aplica container quadrado
        LinearLayout.LayoutParams containerLp = new LinearLayout.LayoutParams(
                screenW, screenW
        );
        containerLp.gravity = Gravity.CENTER_HORIZONTAL;
        videoContainer.setLayoutParams(containerLp);

        // 4) centraliza VideoView
        FrameLayout.LayoutParams videoLp = new FrameLayout.LayoutParams(
                finalW, finalH
        );
        videoLp.gravity = Gravity.CENTER;
        videoView.setLayoutParams(videoLp);

        // 5) ajusta CropOverlay para todo o quadrado
        cropOverlay.setMaxCropSize(screenW);
        cropOverlay.centerInitialCrop(screenW, screenW);

        // inicia conversão (exemplo)
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            convertMp4ToWebpAdaptive();
        }, 100);
    }

    @Override
    protected void onResume() {
        super.onResume();

        videoView.start();
        videoViewPreview.start();
        btnPause.setImageResource(R.drawable.ic_pause);
    }

    public void convertMp4ToWebpAdaptive() {
        int initialQuality = Integer.parseInt(quality);
        int compressionLvl = Integer.parseInt(compression);
        int initialFps = Integer.parseInt(velocidadeFps);

        // Probe video to get original dimensions & duration
        // 1) Probe
        MediaInformationSession probe = FFprobeKit.getMediaInformation(videoFile.getAbsolutePath());
        MediaInformation info = probe.getMediaInformation();
        StreamInformation vStream = info.getStreams().stream()
                .filter(s -> "video".equals(s.getType()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Stream de vídeo não encontrado"));
        final int origW = Integer.parseInt(vStream.getWidth().toString());
        final int origH = Integer.parseInt(vStream.getHeight().toString());
        final int origSquare = Math.max(origW, origH);
        final long videoDurationMs = (long) Double.parseDouble(info.getDuration()) * 1000;  // milliseconds

        final long MAX_SIZE = 500 * 1024;   // 500 KB
        final File webpFile = new File(Environment.getExternalStorageDirectory(),
                "00-Figurinhas/temp/output_cropped.webp");
        fileCropped = webpFile;

        // Dialog UI
        final AlertDialog progressDialog;
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_progress, null);
        ProgressBar progressBar = dialogView.findViewById(R.id.progress_bar);
        TextView attemptsTv = dialogView.findViewById(R.id.attempts_tv);
        progressBar.setMax(100);
        attemptsTv.setText("Tentativa: 0");

        progressDialog = new AlertDialog.Builder(this)
                .setTitle("Convertendo vídeo")
                .setView(dialogView)
                .setCancelable(false)
                .create();
        progressDialog.show();

        // Recursive retry function
        class Retry {
            int quality = initialQuality;
            int compression = compressionLvl;
            int fps = initialFps;
            double velocity = Double.parseDouble(velocidade);
            int attempt = 0;
            long sizeAnterior = 0;
            int sizeAnteriorTentativas = 0;

            void runAttempt() {
                attempt++;
                // reset UI
                runOnUiThread(() -> {
                    progressBar.setProgress(0);
                    attemptsTv.setText("Tentativa: " + attempt);
                });

                // build crop filter
                // 2) mapeia crop do usuário
                Rect r = cropOverlay.getCropRect();
                int containerSide = getResources().getDisplayMetrics().widthPixels;
                float mapFactor = (float) origSquare / containerSide;
                int cropX = Math.round(r.left * mapFactor);
                int cropY = Math.round(r.top * mapFactor);
                int cropW = Math.round(r.width() * mapFactor);
                int cropH = Math.round(r.height() * mapFactor);

                // full video filter
                String vfFilter = String.format(Locale.US,
                        "format=rgba,pad=%d:%d:(%d-iw)/2:(%d-ih)/2:color=#00000000," +  // pad com transparência :contentReference[oaicite:0]{index=0}
                                "crop=%d:%d:%d:%d," +                                // crop exato
                                "scale=512:512:force_original_aspect_ratio=decrease," +
                                "pad=512:512:(ow-iw)/2:(oh-ih)/2:color=#00000000," + // pad final com transparência :contentReference[oaicite:2]{index=2}
                                (velocity == 0 ? "" : ("setpts=" + (1 / Double.parseDouble(velocidade)) + "*PTS,")) +
                                "fps=%d",
                        origSquare, origSquare, origSquare, origSquare,
                        cropW, cropH, cropX, cropY,
                        fps
                );

                String cmd = String.format(Locale.US,
                        "-y -i \"%s\" -vf \"%s\" -c:v libwebp_anim -lossless 0 " +
                                "-q:v %d -compression_level %d -preset default -loop 0 -an -vsync 0 \"%s\"",
                        videoFile.getAbsolutePath(), vfFilter, quality, compression,
                        webpFile.getAbsolutePath());

                // execute async to get progress callbacks
                FFmpegKit.executeAsync(
                        cmd,
                        session -> {
                            // onComplete
                            if (!ReturnCode.isSuccess(session.getReturnCode())) {
                                runOnUiThread(() -> {
                                    progressDialog.dismiss();

                                    Toast.makeText(CropVideoActivity.this,
                                            "FFmpeg falhou: " + session.getFailStackTrace(),
                                            Toast.LENGTH_LONG).show();
                                });
                                return;
                            }
                            long size = webpFile.length();

                            // EXTRAI E EXIBE AS INFOS DO WebP
                            byte[] bytes = getBytes(webpFile);
                            WebPImage webPImage = WebPImage.createFromByteArray(bytes, ImageDecodeOptions.defaults());
                            String webpInfos = String.format(Locale.US,
                                    "Altura: %d\nLargura: %d\nFrames: %d\nDuração: %.2f s\nTamanho: %.2f KB",
                                    webPImage.getHeight(),
                                    webPImage.getWidth(),
                                    webPImage.getFrameCount(),
                                    webPImage.getDuration() / 1000f,
                                    webPImage.getSizeInBytes() / 1024f
                            );

                            runOnUiThread(() -> {
                                TextView infoTv = progressDialog.findViewById(R.id.webp_info_tv);
                                infoTv.setText(webpInfos);
                            });

                            if (sizeAnterior == 0)
                                sizeAnterior = size;
                            else if (sizeAnterior != size)
                                sizeAnterior = size;
                            else {
                                if (sizeAnteriorTentativas > 2) {
                                    runOnUiThread(() -> {
                                        Toast.makeText(CropVideoActivity.this,
                                                "Processo não consegue reduzir o tamanho da figurinha para igual ou menos de 500kb, ajuste os parametros e tente novamente",
                                                Toast.LENGTH_SHORT).show();

                                        progressDialog.dismiss();
                                        showResult(webpFile);
                                    });
                                    return;
                                }
                                sizeAnteriorTentativas++;
                            }

                            if (size <= MAX_SIZE) {
                                runOnUiThread(() -> {
                                    progressDialog.dismiss();
                                    showResult(webpFile);
                                });
                            } else {
                                // adjust params for next iteration
                                if (quality > 40) quality = Math.max(0, quality - 10);

                                if (fps > 15)
                                    fps = Math.max(15, fps - 2);
                                else if (fps > 1)
                                    fps = fps - 1;

                                // next try
                                runAttempt();
                            }
                        },
                        log -> { /* optional: log ffmpeg output */ },
                        statistics -> {
                            // update progress (% of total duration)
                            double time = statistics.getTime();
                            final int percent = (int) (100f * time / videoDurationMs);
                            runOnUiThread(() -> progressBar.setProgress(percent));
                        }
                );
            }
        }

        // kick off first attempt
        new Retry().runAttempt();
    }

    // call this on success to decode WebP and update UI
    private void showResult(File webpFile) {
        byte[] bytes = getBytes(webpFile);
        WebPImage webPImage = WebPImage.createFromByteArray(bytes, ImageDecodeOptions.defaults());
        String infos = String.format(Locale.US,
                "Altura: %d\nLargura: %d\nFrames: %d\nDuração: %.2f s\nTamanho: %.2f KB",
                webPImage.getHeight(),
                webPImage.getWidth(),
                webPImage.getFrameCount(),
                webPImage.getDuration() / 1000f,
                webPImage.getSizeInBytes() / 1024f
        );
        Uri uri = Uri.fromFile(webpFile)
                .buildUpon()
                .appendQueryParameter("t", String.valueOf(System.currentTimeMillis()))
                .build();
        DraweeController controller = Fresco.newDraweeControllerBuilder()
                .setUri(uri)
                .setAutoPlayAnimations(true)
                .build();

        expandedStickerPreview.setImageResource(R.drawable.sticker_error);
        expandedStickerPreview.setController(controller);
        videoView.seekTo(0);

        expandedStickerPreview.setVisibility(View.VISIBLE);
        progressBarPreview.setVisibility(View.GONE);
        videoContainer.setVisibility(View.VISIBLE);
        progressBarVideoView.setVisibility(View.GONE);

        expandedStickerPreview.setOnClickListener(view ->
                Toast.makeText(this, infos, Toast.LENGTH_LONG).show()
        );
    }

    private File getFileCropped() {
        File fileCropped2 = new File(Environment.getExternalStorageDirectory(), "00-Figurinhas/temp/output_cropped.webp");
        if (fileCropped2.exists()) {
            fileCropped2.delete();
            fileCropped2 = new File(Environment.getExternalStorageDirectory(), "00-Figurinhas/temp/output_cropped.webp");
        }

        return fileCropped2;
    }

    private byte[] getBytes(File file) {
        if (file == null) {
            Toast.makeText(this, "Arquivo não existe", Toast.LENGTH_SHORT).show();
            return null;
        }

        try {
            FileInputStream fis = new FileInputStream(file);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                baos.write(buffer, 0, length);
            }

            fis.close();
            baos.close();

            return baos.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, e.getMessage() + "\n" + Objects.requireNonNull(e.getCause()).getMessage(), Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    private void openDialogOpcoesVideo() {
        LayoutInflater inflater = LayoutInflater.from(this); // ou getLayoutInflater() se estiver em uma Activity
        View dialogView = inflater.inflate(R.layout.dialog_custom, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Configurações de Exportação");
        builder.setView(dialogView);

        inputVelocidade = dialogView.findViewById(R.id.input_velocidade);
        inputVelocidadeFps = dialogView.findViewById(R.id.input_velocidade_fps);
        inputCompressao = dialogView.findViewById(R.id.input_compression);
        inputQuality = dialogView.findViewById(R.id.input_quality);

        resetInputs();

        builder.setPositiveButton("OK", (dialog, which) -> {
            velocidade = inputVelocidade.getText().toString().trim();
            velocidadeFps = inputVelocidadeFps.getText().toString().trim();
            compression = inputCompressao.getText().toString().trim();
            quality = inputQuality.getText().toString().trim();

            if (velocidade.isEmpty() || velocidadeFps.isEmpty() || compression.isEmpty() || quality.isEmpty()) {
                Toast.makeText(this, "Preencha todos os campos antes de gerar o comando.", Toast.LENGTH_SHORT).show();
                return;
            }

            dialog.dismiss();
        });

        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.RED);
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.GRAY);
    }

    private void resetInputs() {
        inputVelocidade.setText(velocidade);
        inputVelocidadeFps.setText(velocidadeFps);
        inputCompressao.setText(compression);
        inputQuality.setText(quality);
    }

    public String getNextStickerPrefix() {
        File rootDir = Environment.getExternalStorageDirectory();
        File stickerPackDir = new File(rootDir, "00-Figurinhas/assets/" + stickerPack.identifier);
        File[] files = stickerPackDir.listFiles((dir, name) -> name.endsWith(".webp") && name.contains("_"));
        List<Integer> numbers = new ArrayList<>();

        if (files != null) {
            for (File file : files) {
                try {
                    String fileName = file.getName();
                    String numberPart = fileName.split("_")[0];
                    int number = Integer.parseInt(numberPart);
                    numbers.add(number);
                } catch (NumberFormatException e) {
                }
            }
        }

        int max = numbers.isEmpty() ? 0 : Collections.max(numbers);
        int next = max + 1;

        return String.format("%02d", next);
    }

    private void enviarWhatsapp() {
        if (fileCropped == null) {
            Toast.makeText(this, "Figurinha não existe", Toast.LENGTH_SHORT).show();
            return;
        }

        byte[] bytes = getBytes(fileCropped);
        final WebPImage webPImage = WebPImage.createFromByteArray(bytes, ImageDecodeOptions.defaults());
        if (bytes.length > ANIMATED_STICKER_FILE_LIMIT_KB * KB_IN_BYTES) {
            Toast.makeText(this, "figurinhas animadas tem que ter tamanho menor q "
                    + ANIMATED_STICKER_FILE_LIMIT_KB + "KB, tamanho atual "
                    + String.format(new Locale("pt", "BR"), "%.2f", (double) bytes.length / KB_IN_BYTES) + " KB", Toast.LENGTH_SHORT).show();
            return;
        }
        if (webPImage.getHeight() != IMAGE_HEIGHT) {
            Toast.makeText(this, "a figurinha deve ter " + IMAGE_HEIGHT + " de altura,  altura atual " + webPImage.getHeight(), Toast.LENGTH_SHORT).show();
            return;
        }
        if (webPImage.getWidth() != IMAGE_WIDTH) {
            Toast.makeText(this, "a figurinha deve ter " + IMAGE_WIDTH + "de largura, largura atual " + webPImage.getWidth(), Toast.LENGTH_SHORT).show();
            return;
        }
        if (webPImage.getFrameCount() <= 1) {
            Toast.makeText(this, "a figurinha deve conter pelo menos 2 frame ", Toast.LENGTH_SHORT).show();
            return;
        }
        for (int frameDuration : webPImage.getFrameDurations()) {
            if (frameDuration < ANIMATED_STICKER_FRAME_DURATION_MIN) {
                Toast.makeText(this, "animated sticker frame duration limit is " + ANIMATED_STICKER_FRAME_DURATION_MIN, Toast.LENGTH_SHORT).show();
                return;
            }
        }
        if (webPImage.getDuration() > ANIMATED_STICKER_TOTAL_DURATION_MAX) {
            Toast.makeText(this, "a figurinha deve ter duração máxima de: " + ANIMATED_STICKER_TOTAL_DURATION_MAX + " ms, duração atual: " + webPImage.getDuration() + " ms", Toast.LENGTH_SHORT).show();
            return;
        }

        File rootDir = Environment.getExternalStorageDirectory();
        File outputFileInAssets = new File(rootDir, "00-Figurinhas/assets/" + stickerPack.identifier + "/"
                + getNextStickerPrefix() + "_" + System.currentTimeMillis() + ".webp");

        try {
            FileInputStream fis = new FileInputStream(fileCropped);
            FileOutputStream fos = new FileOutputStream(outputFileInAssets);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }

            fis.close();
            fos.close();

            ContentsJsonHelper.atualizaContentsJsonAndContentProvider(this);
            ContentsJsonHelper.stickerAlteradoTelaCriar = new Sticker(outputFileInAssets.getName(), Arrays.asList("😂", "🎉"), "");

            finish();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, e.getMessage() + "\n" + Objects.requireNonNull(e.getCause()).getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}