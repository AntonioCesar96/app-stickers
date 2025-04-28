package com.example.samplestickerapp;

import android.app.ProgressDialog;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Locale;
import java.util.Objects;

import android.media.MediaPlayer.OnSeekCompleteListener;

import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.ReturnCode;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.ui.PlayerView;

public class CustomVideoRangeActivity extends AppCompatActivity {

    private CustomRangeSeekBar rangeSeekBar;
    private Button showRangeButton;
    private int videoDuration = 0;

    private TextView txtCurrentTime, txtTotalTime;
    private Handler handlerTimeShow = new Handler(Looper.getMainLooper());
    private final int UPDATE_TIME_RUNNABLE = 50;
    private final int UPDATE_INTERVALO_RUNNABLE = 100;
    private Handler handlerIntervalo = new Handler();
    private int currentStartMs = 0, currentEndMs = 8000;

    private StickerPack stickerPack;
    private SimpleExoPlayer player;

    File videoFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_custom_video_range);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Aparar(Trim)");
        }

        stickerPack = getIntent().getParcelableExtra("sticker_pack");
        videoFile = new File(Objects.requireNonNull(getIntent().getStringExtra("file_path")));

        rangeSeekBar = findViewById(R.id.rangeSeekBar);
        showRangeButton = findViewById(R.id.show_range_button);
        txtCurrentTime = findViewById(R.id.txtCurrentTime);
        txtTotalTime = findViewById(R.id.txtTotalTime);

        showRangeButton.setOnClickListener(v -> {
            trimVideoWithFFmpegKit();
        });

        // initializeWithResize();
        initializePlayer();
    }

    private void initializePlayer() {
        if (handlerTimeShow != null)
            handlerTimeShow.removeCallbacks(updateTimeRunnable);
        if (handlerIntervalo != null)
            handlerIntervalo.removeCallbacks(updateVideoPositionRunnable);

        // Cria uma nova instância do ExoPlayer
        player = new SimpleExoPlayer.Builder(this).build();

        // Configura o PlayerView
        PlayerView playerView = findViewById(R.id.player_view);
        playerView.setPlayer(player);

        findViewById(R.id.bottom_buttons).setOnClickListener(view -> {
            if (player != null) {
                if (player.isPlaying()) {
                    player.pause();
                } else {
                    player.play();
                }
            }
        });

        // Defina o caminho do vídeo (ajuste conforme necessário)
        Uri videoUri = Uri.fromFile(videoFile);

        // Cria um MediaItem a partir da URI do vídeo
        MediaItem mediaItem = MediaItem.fromUri(videoUri);

        // Prepara o player com o MediaItem
        player.setMediaItem(mediaItem);
        player.seekTo(currentStartMs);
        player.prepare();

        // Reproduz o vídeo automaticamente quando pronto
        player.setPlayWhenReady(true);

        // Limita a altura do PlayerView
        playerView.post(() -> {
            int maxHeightPx = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, 400, getResources().getDisplayMetrics());

            if (playerView.getHeight() > maxHeightPx) {
                ViewGroup.LayoutParams params = playerView.getLayoutParams();
                params.height = maxHeightPx;
                playerView.setLayoutParams(params);
            }
        });

        // Adiciona o ouvinte de mudanças de estado do player
        player.addListener(new Player.EventListener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_READY) {
                    if (videoDuration != 0) return;

                    // Obtém a duração do vídeo quando estiver pronto
                    videoDuration = (int) player.getDuration();
                    currentEndMs = videoDuration;

                    // Exibe o tempo total do vídeo
                    txtTotalTime.setText(formatTime(videoDuration));

                    // Define a duração do vídeo no rangeSeekBar
                    rangeSeekBar.setVideoDuration(videoDuration);
                }
            }
        });

        rangeSeekBar.setOnRangeChangedListener((startMs, endMs) -> {
            if (currentStartMs != startMs) {
                player.seekTo(startMs);
            }

            currentStartMs = startMs;
            currentEndMs = endMs;
        });

        // Inicia a atualização do tempo do vídeo
        handlerIntervalo.postDelayed(updateVideoPositionRunnable, UPDATE_INTERVALO_RUNNABLE);
        handlerTimeShow.postDelayed(updateTimeRunnable, UPDATE_TIME_RUNNABLE);
    }


    private Runnable updateVideoPositionRunnable = new Runnable() {
        @Override
        public void run() {
            if (player != null) {
                // Se o vídeo ultrapassar o intervalo, ajusta a posição
                int currentPosition = (int) player.getCurrentPosition();
                if (currentPosition >= currentEndMs) {
                    player.seekTo(currentStartMs);
                }
            }

            // Chama o runnable novamente para continuar monitorando
            handlerIntervalo.postDelayed(this, UPDATE_INTERVALO_RUNNABLE);
        }
    };

    private Runnable updateTimeRunnable = new Runnable() {
        @Override
        public void run() {
            if (player != null && player.isPlaying()) {
                int current = (int) player.getCurrentPosition();
                txtCurrentTime.setText(formatTime(current));
            }
            handlerTimeShow.postDelayed(this, UPDATE_TIME_RUNNABLE);
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handlerTimeShow.removeCallbacks(updateTimeRunnable);
        handlerIntervalo.removeCallbacks(updateVideoPositionRunnable);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (player == null) initializePlayer();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!ContentsJsonHelper.stickersAlterados.isEmpty()) {
            finish();
            return;
        }

        if (player == null) {
            initializePlayer();
            return;
        }

        // Retorna o handler quando retomar a atividade
        player.setPlayWhenReady(true);
        handlerIntervalo.postDelayed(updateVideoPositionRunnable, UPDATE_INTERVALO_RUNNABLE);
        handlerTimeShow.postDelayed(updateTimeRunnable, UPDATE_TIME_RUNNABLE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null) {
            player.setPlayWhenReady(false);
        }

        // Para o handler ao pausar
        handlerTimeShow.removeCallbacks(updateTimeRunnable);
        handlerIntervalo.removeCallbacks(updateVideoPositionRunnable);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (player != null) {
            player.release();
            player = null;
        }
    }

    private String formatTime(int millis) {
        int seconds = millis / 1000;
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }


    private void initializeWithResize() {
        String inputPath = videoFile.getAbsolutePath();

        // Video ja vem reduzido
        File outputFile = new File(FilesHelper.getTempDir(), "video_original_reduzido.mp4");
        String outputPath = outputFile.getAbsolutePath();
        if (outputFile.exists()) {
            outputFile.delete();
            outputFile = new File(FilesHelper.getTempDir(), "video_original_reduzido.mp4");
        }

        // Recupera dimensões originais
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(inputPath);
        int width = Integer.parseInt(
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
        int height = Integer.parseInt(
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
        long durationMs = Long.parseLong(
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
        retriever.release();

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
        progressDialog.setTitle("Processing Video");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setIndeterminate(false);
        progressDialog.setMax(100);
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Execute FFmpegKit asynchronously with progress callback
        FFmpegKit.executeAsync(ffmpegCommand,
                session -> {
                    // Dismiss dialog and handle completion on UI thread
                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        if (ReturnCode.isSuccess(session.getReturnCode())) {
                            initializePlayer();
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
                    int percent = (int) ((timeMs / (float) durationMs) * 100);
                    runOnUiThread(() -> progressDialog.setProgress(percent));
                }
        );
    }

    private void trimVideoWithFFmpegKit() {
        String inputPath = videoFile.getAbsolutePath();

        File outputDir = new File(FilesHelper.getTempDir(), "video_original_trimmed.mp4");
        String outputPath = outputDir.getAbsolutePath();
        if (outputDir.exists()) {
            outputDir.delete();
            outputDir = new File(FilesHelper.getTempDir(), "video_original_trimmed.mp4");
        }

        // 3. Converte milissegundos para segundos com precisão de três casas decimais
        double startSec = currentStartMs / 1000.0;
        double durationSec = (currentEndMs - currentStartMs) / 1000.0;

        // 4. Monta comando FFmpeg
        //    -y : sobrescreve sem perguntar
        //    -ss : ponto inicial
        //    -t  : duração a partir de start
        //    -c copy : copia os streams sem re-encode (mais rápido e sem perda)
        String ffmpegCommand = String.format(
                Locale.US,
                "-y -ss %.3f -i \"%s\" -t %.3f -c copy \"%s\"",
                startSec,
                inputPath,
                durationSec,
                outputPath
        );

        FFmpegKit.executeAsync(ffmpegCommand, session -> {
            ReturnCode returnCode = session.getReturnCode();
            if (ReturnCode.isSuccess(returnCode)) {
                runOnUiThread(() -> {
                    Intent intent = new Intent(this, CropVideoActivity.class);
                    intent.putExtra("sticker_pack", stickerPack);
                    intent.putExtra("file_path", outputPath);
                    startActivity(intent);
                });
            } else {
                String failLog = session.getAllLogsAsString();
                Log.e("FFmpegKit", "Erro ao cortar vídeo: " + failLog);
                runOnUiThread(() -> Toast.makeText(CustomVideoRangeActivity.this, "Falha ao cortar vídeo. Veja o log para detalhes.", Toast.LENGTH_LONG).show());
            }
        });
    }
}
