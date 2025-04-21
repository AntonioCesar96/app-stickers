package com.example.samplestickerapp;

import static com.example.samplestickerapp.StickerPackValidator.ANIMATED_STICKER_FILE_LIMIT_KB;
import static com.example.samplestickerapp.StickerPackValidator.ANIMATED_STICKER_FRAME_DURATION_MIN;
import static com.example.samplestickerapp.StickerPackValidator.ANIMATED_STICKER_TOTAL_DURATION_MAX;
import static com.example.samplestickerapp.StickerPackValidator.IMAGE_HEIGHT;
import static com.example.samplestickerapp.StickerPackValidator.IMAGE_WIDTH;
import static com.example.samplestickerapp.StickerPackValidator.KB_IN_BYTES;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
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
import android.widget.Toast;
import android.widget.VideoView;

import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.FFprobeKit;
import com.arthenica.ffmpegkit.MediaInformationSession;
import com.arthenica.ffmpegkit.ReturnCode;
import com.arthenica.ffmpegkit.Session;
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
    private VideoView videoView, videoViewPreview;
    private SimpleDraweeView expandedStickerPreview;
    private FrameLayout videoContainer;
    private ProgressBar progressBarPreview, progressBarVideoView;
    private CropOverlayView cropOverlay;
    private StickerPack stickerPack;
    private String extensaoArquivoOriginal;
    private int videoOriginalWidth, videoOriginalHeight;
    private int videoDisplayedWidth, videoDisplayedHeight;
    File videoFile, fileCropped;
    ImageButton btnPause, btnSalvar, btnCrop, btnOpcoes;
    Spinner spinnerPixFmt;
    EditText inputVelocidade, inputVelocidadeFps, inputScale, inputStartTime, inputDuration, inputQuality;
    String velocidade = "1", velocidadeFps = "30", pixFmt = "yuv420p";
    String scale = "512:-1", startTime = "00:00:00", duration = "0", quality = "15";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop_video);

        videoView = findViewById(R.id.video_view);
        videoView.setZOrderMediaOverlay(true);

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
            performCrop();
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
        extensaoArquivoOriginal = getIntent().getStringExtra("extensao_arquivo");

        videoFile = new File(Environment.getExternalStorageDirectory(), "00-Figurinhas/temp/video_original_trimmed.mp4");
        if (!videoFile.exists())
            videoFile = new File(Environment.getExternalStorageDirectory(), "00-Figurinhas/temp/video_original.mp4");

        videoView.setVideoPath(videoFile.getAbsolutePath());
        videoView.setOnPreparedListener(mp -> {
            mp.setLooping(true);
            videoOriginalWidth = mp.getVideoWidth();
            videoOriginalHeight = mp.getVideoHeight();
        });
        videoView.start();

        progressBarVideoView.setVisibility(View.VISIBLE);
        videoContainer.setVisibility(View.INVISIBLE);

        videoView.getViewTreeObserver().addOnPreDrawListener(
                new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        videoView.getViewTreeObserver().removeOnPreDrawListener(this);

                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            videoDisplayedWidth = videoView.getWidth();
                            videoDisplayedHeight = videoView.getHeight();

                            LinearLayout.LayoutParams newParams = new LinearLayout.LayoutParams(
                                    videoDisplayedWidth,
                                    videoDisplayedHeight
                            );
                            int marginPx = (int) TypedValue.applyDimension(
                                    TypedValue.COMPLEX_UNIT_DIP, 5, getResources().getDisplayMetrics());
                            newParams.setMargins(marginPx, marginPx, marginPx, marginPx);
                            videoContainer.setLayoutParams(newParams);

                            cropOverlay.setMaxCropSize(videoView.getWidth());
                            //cropOverlay.centerInitialCrop(videoDisplayedWidth, videoDisplayedHeight);

                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                progressBarPreview.setVisibility(View.INVISIBLE);
                                performCrop();
                            }, 100);

                        }, 1000);
                        return true;
                    }
                }
        );

        if (extensaoArquivoOriginal != null && extensaoArquivoOriginal.equals("gif")) {
            velocidadeFps = "20";
            pixFmt = "yuv420p";
            quality = "30";
        }

        // TODO: fazer a tela de detalhes e listagem inicial mostrar as figurinhas ANIMADAS
    }

    private void previewFigurinha() {
        fileCropped = getFileCropped();

        // TODO: alterar para pegar o valor das variaveis agora
        int velocidadeFpsValue = velocidadeFps.equals("0") ? 30 : (Math.min(Integer.parseInt(velocidadeFps), 45));
        double adjustedValue = velocidade.equals("0") ? 1 : 1 / Double.parseDouble(velocidade);

        String ffmpegCommand = "-i " + videoFile.getAbsolutePath();
        ffmpegCommand += " -filter:v \"setpts=" + adjustedValue + "*PTS,fps=" + velocidadeFpsValue + ",scale=" + scale + "\"";
        ffmpegCommand += " -ss " + startTime;
        if (!duration.equals("0"))
            ffmpegCommand += " -t " + duration;
        ffmpegCommand += " -loop 0";
        ffmpegCommand += " -crf " + quality;
        ffmpegCommand += " -preset default -pix_fmt " + pixFmt + " -an -lossless 0 " + fileCropped.getAbsolutePath();

        FFmpegKitExecuteAsync(ffmpegCommand);
    }


    @Override
    protected void onResume() {
        super.onResume();

        videoView.start();
        videoViewPreview.start();
        btnPause.setImageResource(R.drawable.ic_pause);
    }

    private void performCrop() {
        // medidas da View na tela
        videoDisplayedWidth = videoView.getWidth();
        videoDisplayedHeight = videoView.getHeight();

        // 1. Retângulo de crop em coordenadas de View
        Rect rect = cropOverlay.getCropRect();
        int xOnScreen = rect.left;
        int yOnScreen = rect.top;
        int wOnScreen = rect.width();
        int hOnScreen = rect.height();

        // 2. Fatores de escala
        float scaleX = (float) videoOriginalWidth / videoDisplayedWidth;
        float scaleY = (float) videoOriginalHeight / videoDisplayedHeight;

        // 3. Coordenadas para o vídeo original
        int cropX = Math.round(xOnScreen * scaleX);
        int cropY = Math.round(yOnScreen * scaleY);
        int cropW = Math.round(wOnScreen * scaleX);
        int cropH = Math.round(hOnScreen * scaleY);


        // 4. Monta filtro FFmpeg
        String cropFilter = String.format(Locale.US, "crop=%d:%d:%d:%d", cropW, cropH, cropX, cropY);

        fileCropped = getFileCropped();

        int velocidadeFpsValue = velocidadeFps.equals("0") ? 30 : (Math.min(Integer.parseInt(velocidadeFps), 45));
        double adjustedValue = velocidade.equals("0") ? 1 : 1 / Double.parseDouble(velocidade);

        String ffmpegCommand = "-i " + videoFile.getAbsolutePath();
        ffmpegCommand += " -filter:v \"" + cropFilter + ",setpts=" + adjustedValue + "*PTS,fps=" + velocidadeFpsValue + ",scale=" + scale + "\"";
        ffmpegCommand += " -ss " + startTime;
        if (!duration.equals("0"))
            ffmpegCommand += " -t " + duration;
        ffmpegCommand += " -loop 0";
        ffmpegCommand += " -crf " + quality;
        ffmpegCommand += " -preset default -pix_fmt " + pixFmt + " -an -lossless 0 " + fileCropped.getAbsolutePath();

        FFmpegKitExecuteAsync(ffmpegCommand);
    }

    public void convertMp4ToWebpAdaptive(
            float xOnScreen,
            float yOnScreen,
            float wOnScreen,
            float hOnScreen,
            float scaleX,
            float scaleY,
            int initialQuality,
            int compressionLvl,
            int initialFps
    ) throws Exception {
        // 1) Pega metadados
        MediaInformationSession probe = FFprobeKit.getMediaInformation(videoFile.getAbsolutePath());
        StreamInformation vStream = probe.getMediaInformation()
                .getStreams()
                .stream()
                .filter(s -> "video".equals(s.getType()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Stream de vídeo não encontrado"));
        int origW = Integer.parseInt(vStream.getWidth().toString());
        int origH = Integer.parseInt(vStream.getHeight().toString());

        final long MAX_SIZE = 500 * 1024;   // 500 KB
        File webpFile = new File(Environment.getExternalStorageDirectory(), "00-Figurinhas/temp/output_cropped.webp");

        // parâmetros adaptativos
        int targetW = origW;
        int targetH = origH;
        int quality = initialQuality;
        int compression = compressionLvl;
        int fps = initialFps;
        final float SCALE_STEP = 0.9f;      // reduz 10% a cada iteração
        final int FPS_STEP = 1;             // reduz 2 fps por iteração
        final int MIN_FPS = 10;

        while (true) {
            // coords de crop
            int cropX = Math.round(xOnScreen * scaleX);
            int cropY = Math.round(yOnScreen * scaleY);
            int cropW = Math.round(wOnScreen * scaleX);
            int cropH = Math.round(hOnScreen * scaleY);
            String cropFilter = String.format(Locale.US, "crop=%d:%d:%d:%d", cropW, cropH, cropX, cropY);

            // monta filtro completo: crop, scale adaptativo, pad, fps
            String vf = String.format(Locale.US,
                    "%s,scale=%d:%d,scale=512:512:force_original_aspect_ratio=decrease,pad=512:512:(ow-iw)/2:(oh-ih)/2:color=black,fps=%d",
                    cropFilter, targetW, targetH, fps
            );

            String cmd = String.format(Locale.US,
                    "-y -i \"%s\" -vf \"%s\" -c:v libwebp -lossless 0 -q:v %d -compression_level %d -preset default -loop 0 -an -vsync 0 \"%s\"",
                    videoFile.getAbsolutePath(), vf, quality, compression, webpFile.getAbsolutePath()
            );

            Session session = FFmpegKit.execute(cmd);
            if (!ReturnCode.isSuccess(session.getReturnCode())) {
                throw new RuntimeException("FFmpeg falhou: " + session.getFailStackTrace());
            }

            long size = webpFile.length();
            if (size <= MAX_SIZE) {


                runOnUiThread(() -> {

                    byte[] bytes = getBytes(webpFile);
                    final WebPImage webPImage = WebPImage.createFromByteArray(bytes, ImageDecodeOptions.defaults());

                    String infos = "Altura: " + webPImage.getHeight();
                    infos += "\nLargura: " + webPImage.getWidth();
                    infos += "\nQtd Frames: " + webPImage.getFrameCount();
                    infos += "\nDuração: " + webPImage.getDuration() / 1000 + " segundos";
                    infos += "\nTamanho: " + String.format(new Locale("pt", "BR"), "%.2f", (double) webPImage.getSizeInBytes() / 1024.0) + " KB";

                    final Uri stickerAssetUri = Uri.fromFile(webpFile)
                            .buildUpon()
                            .appendQueryParameter("t", String.valueOf(System.currentTimeMillis()))
                            .build();
                    DraweeController controller = Fresco.newDraweeControllerBuilder()
                            .setUri(stickerAssetUri)
                            .setAutoPlayAnimations(true)
                            .build();

                    expandedStickerPreview.setImageResource(R.drawable.sticker_error);
                    expandedStickerPreview.setController(controller);
                    videoView.seekTo(0);

                    expandedStickerPreview.setVisibility(View.VISIBLE);
                    progressBarPreview.setVisibility(View.INVISIBLE);

                    videoContainer.setVisibility(View.VISIBLE);
                    progressBarVideoView.setVisibility(View.GONE);

                    String finalInfos = infos;
                    expandedStickerPreview.setOnClickListener(view -> {
                        Toast.makeText(this,
                                finalInfos,
                                Toast.LENGTH_LONG).show();
                    });
                });




                break;  // sucesso
            }

            // sem limites: sempre ajusta qualidade, resolução e fps
            if (quality > 50) {
                quality = Math.max(0, quality - 10);
            }
            if (fps > MIN_FPS) {
                fps = Math.max(MIN_FPS, fps - FPS_STEP);
            }
            targetW = Math.max(1, Math.round(targetW * SCALE_STEP));
            targetH = Math.max(1, Math.round(targetH * SCALE_STEP));
        }
    }

    public static void convertMp4ToWebpWithCropAndSizeLimit2(
            Context context,
            String inputMp4,
            String outputWebP,
            float xOnScreen,
            float yOnScreen,
            float wOnScreen,
            float hOnScreen,
            float scaleX,
            float scaleY
    ) throws Exception {
        // 1) Obtém resolução original do MP4
        MediaInformationSession probe = FFprobeKit.getMediaInformation(inputMp4);
        StreamInformation videoStream = probe.getMediaInformation()
                .getStreams()
                .stream()
                .filter(s -> "video".equals(s.getType()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Stream de vídeo não encontrado"));

        int origWidth = Integer.parseInt(videoStream.getWidth().toString());
        int origHeight = Integer.parseInt(videoStream.getHeight().toString());

        // 2) Parâmetros iniciais
        int decrement = 0;                     // quanto reduzir da resolução a cada iteração
        final int STEP = 100;                  // decremento em px
        final long MAX_SIZE = 500 * 1024;      // 500 KB

        File webpFile = new File(outputWebP);

        while (true) {
            // 3) Cálculo das coordenadas de crop no vídeo original
            int cropX = Math.round(xOnScreen * scaleX);
            int cropY = Math.round(yOnScreen * scaleY);
            int cropW = Math.round(wOnScreen * scaleX);
            int cropH = Math.round(hOnScreen * scaleY);

            String cropFilter = String.format(
                    Locale.US,
                    "crop=%d:%d:%d:%d",
                    cropW, cropH, cropX, cropY
            );

            // 4) Calcula nova resolução de throughput (antes do crop)
            int targetW = Math.max(STEP, origWidth - decrement);
            int targetH = Math.max(STEP, origHeight - decrement);

            // 5) Monta comando FFmpeg: aplica resize, crop, depois scale+pad para 512×512
            String vf = String.format(
                    Locale.US,
                    "scale=%d:%d,%s,scale=512:512:force_original_aspect_ratio=decrease,pad=512:512:(ow-iw)/2:(oh-ih)/2:color=black,fps=15",
                    targetW, targetH, cropFilter
            );

            String ffmpegCmd = String.format(
                    "-y -i \"%s\" -vf \"%s\" -c:v libwebp -lossless 0 -q:v 75 -preset default -loop 0 -an -vsync 0 \"%s\"",
                    inputMp4, vf, outputWebP
            );

            // 6) Executa FFmpeg
            Session session = FFmpegKit.execute(ffmpegCmd);
            if (!ReturnCode.isSuccess(session.getReturnCode())) {
                throw new RuntimeException("FFmpeg falhou: " + session.getFailStackTrace());
            }

            // 7) Verifica tamanho do WebP
            long size = webpFile.length();
            if (size <= MAX_SIZE) {
                // Conseguiu ficar abaixo de 500 KB
                break;
            }

            // 8) Se ainda grande, aumenta decremento e tenta de novo
            decrement += STEP;
            if (decrement >= Math.min(origWidth, origHeight)) {
                throw new RuntimeException(
                        "Não foi possível reduzir abaixo de 500 KB mesmo com resolução mínima"
                );
            }
        }
    }


    private File getFileCropped() {
        File fileCropped2 = new File(Environment.getExternalStorageDirectory(), "00-Figurinhas/temp/output_cropped.webp");
        if (fileCropped2.exists()) {
            fileCropped2.delete();
            fileCropped2 = new File(Environment.getExternalStorageDirectory(), "00-Figurinhas/temp/output_cropped.webp");
        }

        return fileCropped2;
    }

    private void FFmpegKitExecuteAsync(String ffmpegCommand) {
        FFmpegKit.executeAsync(ffmpegCommand, session -> {
            ReturnCode returnCode = session.getReturnCode();
            if (ReturnCode.isSuccess(returnCode)) {
                runOnUiThread(() -> {

                    byte[] bytes = getBytes(fileCropped);
                    final WebPImage webPImage = WebPImage.createFromByteArray(bytes, ImageDecodeOptions.defaults());

                    String infos = "Altura: " + webPImage.getHeight();
                    infos += "\nLargura: " + webPImage.getWidth();
                    infos += "\nQtd Frames: " + webPImage.getFrameCount();
                    infos += "\nDuração: " + webPImage.getDuration() / 1000 + " segundos";
                    infos += "\nTamanho: " + String.format(new Locale("pt", "BR"), "%.2f", (double) webPImage.getSizeInBytes() / 1024.0) + " KB";

                    final Uri stickerAssetUri = Uri.fromFile(fileCropped)
                            .buildUpon()
                            .appendQueryParameter("t", String.valueOf(System.currentTimeMillis()))
                            .build();
                    DraweeController controller = Fresco.newDraweeControllerBuilder()
                            .setUri(stickerAssetUri)
                            .setAutoPlayAnimations(true)
                            .build();

                    expandedStickerPreview.setImageResource(R.drawable.sticker_error);
                    expandedStickerPreview.setController(controller);
                    videoView.seekTo(0);

                    expandedStickerPreview.setVisibility(View.VISIBLE);
                    progressBarPreview.setVisibility(View.INVISIBLE);

                    videoContainer.setVisibility(View.VISIBLE);
                    progressBarVideoView.setVisibility(View.GONE);

                    String finalInfos = infos;
                    expandedStickerPreview.setOnClickListener(view -> {
                        Toast.makeText(this,
                                finalInfos,
                                Toast.LENGTH_LONG).show();
                    });
                });
            } else {
                // Erro no ffmpeg
                String failStack = session.getFailStackTrace();
                runOnUiThread(() -> Toast.makeText(this,
                        "Erro ao cortar vídeo: " + failStack,
                        Toast.LENGTH_LONG).show());
            }
        });
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
        inputScale = dialogView.findViewById(R.id.input_scale);
        inputStartTime = dialogView.findViewById(R.id.input_start_time);
        inputDuration = dialogView.findViewById(R.id.input_duration);
        inputQuality = dialogView.findViewById(R.id.input_quality);
        spinnerPixFmt = dialogView.findViewById(R.id.spinner_pix_fmt);

        ArrayAdapter<CharSequence> pixFmtAdapter = ArrayAdapter.createFromResource(
                this, R.array.pix_fmt_array, android.R.layout.simple_spinner_item);
        pixFmtAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPixFmt.setAdapter(pixFmtAdapter);

        resetInputs();

        builder.setPositiveButton("OK", (dialog, which) -> {
            velocidade = inputVelocidade.getText().toString().trim();
            velocidadeFps = inputVelocidadeFps.getText().toString().trim();
            scale = inputScale.getText().toString().trim();
            startTime = inputStartTime.getText().toString().trim();
            duration = inputDuration.getText().toString().trim();
            quality = inputQuality.getText().toString().trim();
            pixFmt = spinnerPixFmt.getSelectedItem().toString();

            if (velocidade.isEmpty() || velocidadeFps.isEmpty() || scale.isEmpty()
                    || startTime.isEmpty() || duration.isEmpty() || quality.isEmpty()) {

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
        inputScale.setText(scale);
        inputStartTime.setText(startTime);
        inputDuration.setText(duration);
        inputQuality.setText(quality);
        //spinnerPixFmt.setSelection(0);
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