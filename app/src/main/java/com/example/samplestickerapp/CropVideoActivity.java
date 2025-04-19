package com.example.samplestickerapp;

import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;

import androidx.appcompat.app.AppCompatActivity;

import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.FrameLayout;

import android.widget.Toast;
import android.widget.VideoView;

import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.ReturnCode;

import java.io.File;
import java.util.Locale;

public class CropVideoActivity extends AppCompatActivity {
    private VideoView videoView;
    private CropOverlayView cropOverlay;
    private StickerPack stickerPack;
    private int videoOriginalWidth, videoOriginalHeight;
    private int videoDisplayedWidth, videoDisplayedHeight;
    File videoFile;
    Button btnPause;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.WHITE);

        FrameLayout videoContainer = new FrameLayout(this);
        int marginPx = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 5, getResources().getDisplayMetrics());
        FrameLayout.LayoutParams videoParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        videoParams.setMargins(marginPx, marginPx, marginPx, marginPx);
        videoContainer.setLayoutParams(videoParams);

        videoView = new VideoView(this);
        videoView.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        ));
        videoView.setZOrderMediaOverlay(true);

        cropOverlay = new CropOverlayView(this);
        cropOverlay.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        videoContainer.addView(videoView);
        videoContainer.addView(cropOverlay);

        root.addView(videoContainer);

        Button btnCrop = new Button(this);
        btnCrop.setText("Cropar");
        FrameLayout.LayoutParams btnParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL  // gravidade bottom + center :contentReference[oaicite:0]{index=0}
        );

        int bottomMargin = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());
        btnParams.setMargins(0, 0, 0, bottomMargin);
        btnCrop.setLayoutParams(btnParams);

        btnCrop.setOnClickListener(v -> performCrop());

        root.addView(btnCrop);

        // dentro de onCreate(), logo após btnCrop:
        btnPause = new Button(this);
        btnPause.setText("Pausar");
        FrameLayout.LayoutParams pauseParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.LEFT
        );
        int leftMargin = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 16, getResources().getDisplayMetrics());
        pauseParams.setMargins(leftMargin, 0, 0, bottomMargin);
        btnPause.setLayoutParams(pauseParams);

        btnPause.setOnClickListener(v -> {
            if (videoView.isPlaying()) {
                videoView.pause();
                btnPause.setText("Retomar");
            } else {
                videoView.start();
                btnPause.setText("Pausar");
            }
        });

        root.addView(btnPause);

        setContentView(root);

        // Carrega vídeo
        stickerPack = getIntent().getParcelableExtra("sticker_pack");

        videoFile = new File(Environment.getExternalStorageDirectory(),"00-Figurinhas/temp/video_original.mp4");
        videoView.setVideoPath(videoFile.getAbsolutePath());
        videoView.setOnPreparedListener(mp -> {
            mp.setLooping(true);
            // salva resolução original do vídeo
            videoOriginalWidth  = mp.getVideoWidth();
            videoOriginalHeight = mp.getVideoHeight();
        });
        videoView.start();

        videoView.getViewTreeObserver().addOnPreDrawListener(
                new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        videoView.getViewTreeObserver().removeOnPreDrawListener(this);

                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            videoDisplayedWidth  = videoView.getWidth();
                            videoDisplayedHeight = videoView.getHeight();

                            FrameLayout.LayoutParams newParams = new FrameLayout.LayoutParams(
                                    videoDisplayedWidth,
                                    videoDisplayedHeight
                            );
                            newParams.setMargins(marginPx, marginPx, marginPx, marginPx);
                            videoContainer.setLayoutParams(newParams);

                            cropOverlay.setMaxCropSize(videoView.getWidth());
                            cropOverlay.centerInitialCrop(videoDisplayedWidth, videoDisplayedHeight);

                        }, 100);
                        return true;
                    }
                }
        );
    }

    @Override
    protected void onResume() {
        super.onResume();

        videoView.start();
        btnPause.setText("Pausar");
    }

    private void performCrop() {
        // medidas da View na tela
        videoDisplayedWidth  = videoView.getWidth();
        videoDisplayedHeight = videoView.getHeight();

        // 1. Retângulo de crop em coordenadas de View
        Rect rect = cropOverlay.getCropRect();
        int xOnScreen = rect.left;
        int yOnScreen = rect.top;
        int wOnScreen = rect.width();
        int hOnScreen = rect.height();

        // 2. Fatores de escala
        float scaleX = (float) videoOriginalWidth  / videoDisplayedWidth;
        float scaleY = (float) videoOriginalHeight / videoDisplayedHeight;

        // 3. Coordenadas para o vídeo original
        int cropX = Math.round(xOnScreen * scaleX);
        int cropY = Math.round(yOnScreen * scaleY);
        int cropW = Math.round(wOnScreen * scaleX);
        int cropH = Math.round(hOnScreen * scaleY);

        // 4. Monta filtro FFmpeg
        String cropFilter = String.format(Locale.US,
                "crop=%d:%d:%d:%d",
                cropW, cropH, cropX, cropY
        );

        String inputPath  = videoFile.getAbsolutePath();

        File fileCropped = new File(Environment.getExternalStorageDirectory(), "00-Figurinhas/temp/output_cropped.mp4");
        String outputPath = fileCropped.getAbsolutePath();
        if(fileCropped.exists()) {
            fileCropped.delete();
            fileCropped = new File(Environment.getExternalStorageDirectory(), "00-Figurinhas/temp/output_cropped.mp4");
        }

        // 4. Montar comando FFmpeg completo
        String ffmpegCommand = String.join(" ",
                "-i", "\"" + inputPath + "\"",
                "-filter:v", "\"" + cropFilter + "\"",
                "-an",
                "\"" + outputPath + "\""
        );

        // 5. Executar via FFmpegKit em background com callback :contentReference[oaicite:6]{index=6}
        FFmpegKit.executeAsync(ffmpegCommand, session -> {
            ReturnCode returnCode = session.getReturnCode();
            if (ReturnCode.isSuccess(returnCode)) {
                // Crop realizado com sucesso
                runOnUiThread(() -> Toast.makeText(this,
                        "Vídeo cortado salvo em: " + outputPath,
                        Toast.LENGTH_LONG).show());
            } else {
                // Erro no ffmpeg
                String failStack = session.getFailStackTrace();
                runOnUiThread(() -> Toast.makeText(this,
                        "Erro ao cortar vídeo: " + failStack,
                        Toast.LENGTH_LONG).show());
            }
        });
    }
}