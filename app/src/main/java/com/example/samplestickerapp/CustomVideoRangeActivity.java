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
import android.view.MenuItem;
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
        player.setVolume(0f);

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
                    videoDuration = getIntent().hasExtra(GifMedia.SOURCE)
                            ? (int) getIntent().getLongExtra(GifMedia.END_MS, player.getDuration())
                            : (int) player.getDuration();
                    currentEndMs = Math.min(videoDuration, 8000);

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

    private void trimVideoWithFFmpegKit() {
        if (getIntent().hasExtra(GifMedia.SOURCE)) {
            if (currentStartMs < 0 || currentEndMs <= currentStartMs || currentEndMs > videoDuration) {
                Toast.makeText(this, "Selecione um intervalo válido do GIF.", Toast.LENGTH_LONG).show();
                return;
            }
            // Keep the original GIF and its preview; export applies this range only once.
            Intent intent = new Intent(this, CropVideoActivity.class);
            intent.putExtras(getIntent());
            intent.putExtra(GifMedia.START_MS, (long) currentStartMs);
            intent.putExtra(GifMedia.END_MS, (long) currentEndMs);
            startActivity(intent);
            return;
        }
        String inputPath = videoFile.getAbsolutePath();
        String[] split = inputPath.split("/");
        String nomeArquivoOriginalTemp = split[split.length - 1].replace(".", "_") + "_cortado.mp4";

        File outputDir = new File(FilesHelper.getMp4Dir(), nomeArquivoOriginalTemp);
        String outputPath = outputDir.getAbsolutePath();
        if (outputDir.exists()) {
            outputDir.delete();
            outputDir = new File(FilesHelper.getMp4Dir(), nomeArquivoOriginalTemp);
        }

        if (currentStartMs < 0 || currentEndMs <= currentStartMs || currentEndMs > videoDuration) {
            Toast.makeText(this, "Selecione um intervalo válido do vídeo.", Toast.LENGTH_LONG).show();
            return;
        }
        showRangeButton.setEnabled(false);
        showRangeButton.setText("Preparando vídeo…");
        // Re-encode to discard keyframe preroll and give the looping preview a zero-based timeline.
        FFmpegKit.executeWithArgumentsAsync(VideoTrim.arguments(videoFile, outputDir,
                currentStartMs, currentEndMs), session -> {
            ReturnCode returnCode = session.getReturnCode();
            if (ReturnCode.isSuccess(returnCode)) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    showRangeButton.setEnabled(true);
                    showRangeButton.setText(R.string.trim_video_button);
                    Intent intent = new Intent(this, CropVideoActivity.class);
                    intent.putExtra("sticker_pack", stickerPack);
                    intent.putExtra("file_path", outputPath);
                    startActivity(intent);
                });
            } else {
                String failLog = session.getAllLogsAsString();
                Log.e("FFmpegKit", "Erro ao cortar vídeo: " + failLog);
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    showRangeButton.setEnabled(true);
                    showRangeButton.setText(R.string.trim_video_button);
                    Toast.makeText(CustomVideoRangeActivity.this, "Falha ao cortar vídeo. Veja o log para detalhes.", Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
