package com.example.samplestickerapp;

import static com.example.samplestickerapp.StickerPackValidator.ANIMATED_STICKER_FILE_LIMIT_KB;
import static com.example.samplestickerapp.StickerPackValidator.ANIMATED_STICKER_FRAME_DURATION_MIN;
import static com.example.samplestickerapp.StickerPackValidator.ANIMATED_STICKER_TOTAL_DURATION_MAX;
import static com.example.samplestickerapp.StickerPackValidator.IMAGE_HEIGHT;
import static com.example.samplestickerapp.StickerPackValidator.IMAGE_WIDTH;
import static com.example.samplestickerapp.StickerPackValidator.KB_IN_BYTES;

import android.graphics.Color;
import android.content.Intent;
import android.app.NotificationManager;
import android.provider.Settings;
import androidx.core.app.NotificationManagerCompat;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Gravity;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;

import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

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
import java.util.stream.Collectors;

public class CropVideoActivity extends AppCompatActivity {
    private LockableScrollView lockableScrollView;
    private VideoView videoView;
    private SimpleDraweeView expandedStickerPreview;
    private FrameLayout videoContainer;
    private LinearLayout bottomButtons;
    private ProgressBar progressBarVideoView;
    private CropOverlayView cropOverlay;
    private StickerPack stickerPack;
    private String extensaoArquivoOriginal;
    private int videoOriginalWidth, videoOriginalHeight;
    File videoFile, fileCropped;
    ImageButton btnPause, btnSalvar, btnCrop, btnExodia, btnOpcoes;
    EditText inputVelocidade, inputVelocidadeFps, inputCompressao, inputQuality, inputDuracao;
    String velocidade = "1", velocidadeFps = "30", compression = "6", quality = "75", duracao = "0";
    AlertDialog progressDialog;
    FrameLayout stickerContainerNaoExodia;
    LinearLayout stickerContainerExodia;
    boolean exodia = false;
    private boolean converting;
    private String jobId;
    private boolean jobRendered;
    private boolean progressHidden;
    private String[] retryDraft;
    private android.widget.PopupWindow compressionInfo;
    private String compressionControlError = "";
    private final Handler jobHandler = new Handler(Looper.getMainLooper());
    private final Runnable pollJob = new Runnable() {
        @Override public void run() {
            refreshJob();
            jobHandler.postDelayed(this, 750);
        }
    };
    private boolean gifSource;
    private long gifStartMs, gifEndMs;
    private boolean cropInitialized;
    private final Handler gifPreviewHandler = new Handler(Looper.getMainLooper());
    private final Runnable loopGifPreview = new Runnable() {
        @Override public void run() {
            if (videoView.isPlaying() && videoView.getCurrentPosition() >= gifEndMs) {
                videoView.seekTo((int) gifStartMs);
            }
            gifPreviewHandler.postDelayed(this, 30);
        }
    };
    List<ControleSticker> controleStickers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) retryDraft = savedInstanceState.getStringArray("compressionDraft");
        jobId = savedInstanceState == null ? getIntent().getStringExtra(CompressionJob.EXTRA_ID)
                : savedInstanceState.getString(CompressionJob.EXTRA_ID);
        if (jobId != null) {
            CompressionJob restored = CompressionJob.load(this, jobId);
            if (restored == null) { finish(); return; }
            try {
                setIntent(restored.screen(this));
                exodia = restored.data.optBoolean("exodia");
                velocidade = restored.data.getString("speed");
                velocidadeFps = restored.data.getString("fps");
                quality = restored.data.getString("quality");
                compression = restored.data.getString("compression");
                if (restored.running()) CompressionService.start(this, restored);
            } catch (Exception e) {
                Toast.makeText(this, "Não foi possível recuperar a tarefa.", Toast.LENGTH_LONG).show();
                finish(); return;
            }
        }
        CompressionService.channels(this);
        setContentView(R.layout.activity_crop_video);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Cortar(Crop)");
        }

        FilesHelper.setContext(getApplicationContext());
        videoView = findViewById(R.id.video_view);
        videoView.setZOrderMediaOverlay(true);

        lockableScrollView = findViewById(R.id.lockableScrollView);
        progressBarVideoView = findViewById(R.id.sticker_loader_video_view);
        cropOverlay = findViewById(R.id.crop_overlay);
        btnCrop = findViewById(R.id.btn_crop);
        btnExodia = findViewById(R.id.btn_exodia);
        btnPause = findViewById(R.id.btn_pause);
        btnSalvar = findViewById(R.id.btn_salvar);
        btnSalvar.setEnabled(false);
        btnOpcoes = findViewById(R.id.btn_opcoes);
        videoContainer = findViewById(R.id.video_container);
        bottomButtons = findViewById(R.id.bottom_buttons);
        stickerContainerNaoExodia = findViewById(R.id.stickerContainerNaoExodia);
        stickerContainerExodia = findViewById(R.id.stickerContainerExodia);

        progressBarVideoView.setVisibility(View.VISIBLE);
        videoContainer.setVisibility(View.INVISIBLE);
        bottomButtons.setVisibility(View.INVISIBLE);

        btnCrop.setOnClickListener(v -> {
            exodia = false;
            convertMp4ToWebpAdaptive();
        });
        btnExodia.setOnClickListener(v -> {
            exodia = true;
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
        String originalGif = getIntent().getStringExtra(GifMedia.SOURCE);
        gifSource = originalGif != null;
        gifStartMs = getIntent().getLongExtra(GifMedia.START_MS, 0);
        gifEndMs = getIntent().getLongExtra(GifMedia.END_MS, 0);

        videoView.setVideoPath(videoFile.getAbsolutePath());
        videoView.setOnPreparedListener(mp -> {
            mp.setVolume(0f, 0f);
            mp.setLooping(!gifSource);

            videoOriginalWidth = gifSource ? getIntent().getIntExtra(GifMedia.WIDTH, mp.getVideoWidth()) : mp.getVideoWidth();
            videoOriginalHeight = gifSource ? getIntent().getIntExtra(GifMedia.HEIGHT, mp.getVideoHeight()) : mp.getVideoHeight();

            if (gifSource) videoView.seekTo((int) gifStartMs);
            videoView.start();
            if (!cropInitialized) {
                cropInitialized = true;
                videoView.post(this::setupContainerAndCrop);
            }
        });
        videoView.setOnCompletionListener(mp -> {
            if (gifSource) {
                videoView.seekTo((int) gifStartMs);
                videoView.start();
            }
        });

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

        // Let the user choose the crop before creating a new sticker.
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (jobId == null) {
                exodia = false;
                mostrarVideo();
            } else {
                CompressionJob job = CompressionJob.load(this, jobId);
                if (job != null) {
                    try { cropOverlay.restoreCrop(job.crop(screenW)); }
                    catch (Exception ignored) { }
                }
                mostrarVideo();
                refreshJob();
            }
        }, 100);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (videoView == null) return;
        jobHandler.removeCallbacks(pollJob);
        jobHandler.post(pollJob);
        videoView.start();
        btnPause.setImageResource(R.drawable.ic_pause);
        if (gifSource) {
            gifPreviewHandler.removeCallbacks(loopGifPreview);
            gifPreviewHandler.post(loopGifPreview);
        }
    }

    @Override
    protected void onPause() {
        jobHandler.removeCallbacks(pollJob);
        gifPreviewHandler.removeCallbacks(loopGifPreview);
        if (videoView != null) videoView.pause();
        super.onPause();
    }

    public void convertMp4ToWebpAdaptive() {
        if (converting || isFinishing() || isDestroyed()) return;
        CompressionJob pending = CompressionJob.pending(this);
        if (pending != null && pending.running() && !pending.id.equals(jobId)) {
            try { startActivity(pending.screen(this)); finish(); }
            catch (Exception e) { Toast.makeText(this, "Já existe uma compressão em andamento.", Toast.LENGTH_LONG).show(); }
            return;
        }
        if (exodia && (stickerPack.getStickers().size() + 4) > 30) {
            mostrarVideo();
            Toast.makeText(CropVideoActivity.this,
                    "Não é possível adicionar mais 4 figurinhas nesse pacote, pois em um pacote é permitido " +
                            "30 figurinhas, se gerarmos mais 4 nesse pacote ele ficará com "
                            + (stickerPack.getStickers().size() + 4) + " figurinhas", Toast.LENGTH_LONG).show();
            return;
        }
        if (!exodia && (stickerPack.getStickers().size() + 1) > 30) {
            mostrarVideo();
            Toast.makeText(CropVideoActivity.this,
                    "Não é possível adicionar mais 1 figurinha nesse pacote, pois em um pacote é permitido " +
                            "30 figurinhas, se gerarmos mais 1 nesse pacote ele ficará com "
                            + (stickerPack.getStickers().size() + 1) + " figurinhas", Toast.LENGTH_LONG).show();
            return;
        }

        findViewById(R.id.output_exodia_0_webp_l).setVisibility(View.VISIBLE);
        findViewById(R.id.output_exodia_1_webp_l).setVisibility(View.VISIBLE);
        findViewById(R.id.output_exodia_2_webp_l).setVisibility(View.VISIBLE);
        findViewById(R.id.output_exodia_3_webp_l).setVisibility(View.VISIBLE);

        controleStickers = new ArrayList<>();
        btnSalvar.setEnabled(false);

        try {
            // Validation and probing run inside the service, never on the UI thread.
            CompressionJob job = CompressionJob.create(this, getIntent(), cropOverlay.getCropRect(),
                    getResources().getDisplayMetrics().widthPixels, exodia, velocidade,
                    velocidadeFps, quality, compression);
            jobId = job.id;
            getIntent().putExtra(CompressionJob.EXTRA_ID, jobId);
            jobRendered = false;
            progressHidden = false;
            exodia = job.data.optBoolean("exodia");
            CompressionService.start(this, job);
            refreshJob();
        } catch (Exception e) {
            converting = false;
            mostrarVideo();
            Toast.makeText(this, "Não foi possível iniciar a compressão: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private File outputDirectory() {
        CompressionJob job = CompressionJob.load(this, jobId);
        return job == null ? FilesHelper.getTempDir() : job.directory;
    }

    private void refreshJob() {
        if (jobId == null || isFinishing() || isDestroyed() || btnSalvar == null) return;
        CompressionJob job = CompressionJob.load(this, jobId);
        if (job == null) return;
        TextView status = findViewById(R.id.compression_status);
        status.setVisibility(View.VISIBLE);
        status.setText(job.data.optString("message") + "\nToque para acompanhar.");
        status.setOnClickListener(v -> {
            progressHidden = false;
            if (progressDialog == null) progressDialog = createProgressDialog();
            refreshJob();
        });
        converting = job.running();
        btnCrop.setEnabled(!converting);
        btnExodia.setEnabled(!converting);
        btnOpcoes.setEnabled(!converting);
        cropOverlay.setEnabled(!converting);
        if (converting || progressDialog != null) {
            if (!progressHidden) {
                if (progressDialog == null) progressDialog = createProgressDialog();
                if (!progressDialog.isShowing()) progressDialog.show();
                ProgressBar progress = progressDialog.findViewById(R.id.progress_bar);
                int value = job.data.optInt("progress");
                progress.setIndeterminate(value <= 0);
                if (!converting) progress.setIndeterminate(false);
                progress.setMax(100);
                progress.setProgress(value);
                TextView attempts = progressDialog.findViewById(R.id.attempts_tv);
                attempts.setText(job.data.optString("message"));
                ((TextView) progressDialog.findViewById(R.id.compression_title)).setText(job.running() ? "Criando figurinha"
                        : job.done() ? "Figurinha pronta" : "cancelled".equals(job.data.optString("status")) ? "Compressão cancelada" : "Falha na compressão");
                TextView details = progressDialog.findViewById(R.id.webp_info_tv);
                org.json.JSONObject last = job.data.optJSONObject("lastAttempt");
                org.json.JSONObject current = job.data.optJSONObject("currentAttempt");
                details.setText((current == null ? "" : (converting ? "Tentativa atual: " : "Última iniciada: ") + attemptSettings(current) + "\n\n")
                        + (last == null ? "Ainda não há uma tentativa concluída."
                        : String.format(Locale.getDefault(), "Última concluída (#%d): %.2f KB / 500 KB\n%s",
                        last.optInt("number"), last.optLong("bytes") / 1024.0, attemptSettings(last)))
                        + (value > 0 && converting ? "\nProgresso atual: " + value + "%" : ""));
                try {
                    org.json.JSONObject control = job.control();
                    boolean waiting = job.running() && control.optInt("revision") > job.data.optInt("appliedRevision");
                    boolean cancelling = waiting && "cancel".equals(control.optString("action"));
                    progressDialog.findViewById(R.id.retry_compression_button).setEnabled(!waiting);
                    progressDialog.findViewById(R.id.cancel_compression).setEnabled(job.running() && !cancelling);
                    TextView action = progressDialog.findViewById(R.id.compression_action_status);
                    action.setText(waiting ? cancelling ? "Cancelando a tentativa atual…" : "Interrompendo para reiniciar com seus valores…"
                            : !compressionControlError.isEmpty() ? compressionControlError
                            : job.running() ? "" : job.data.optString("message"));
                } catch (Exception e) { android.util.Log.w("CompressionJob", "Leitura dos controles", e); }
            }
        }
        if (converting) return;
        if (jobRendered) return;
        jobRendered = true;
        mostrarVideo();
        if (job.done()) {
            exodia = job.data.optBoolean("exodia");
            controleStickers.clear();
            org.json.JSONArray results = job.data.optJSONArray("results");
            for (int i = 0; i < results.length(); i++) showResult(job.result(i), results.optString(i));
            btnSalvar.setEnabled(true);
        } else if (!"cancelled".equals(job.data.optString("status"))) {
            Toast.makeText(this, job.data.optString("message"), Toast.LENGTH_LONG).show();
        }
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        state.putString(CompressionJob.EXTRA_ID, jobId);
        if (progressDialog != null) state.putStringArray("compressionDraft", readRetryDraft());
        super.onSaveInstanceState(state);
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // A notification can target a different job; rebuild all editor fields from its record.
        setIntent(intent);
        jobId = intent.getStringExtra(CompressionJob.EXTRA_ID);
        recreate();
    }

    @Override protected void onDestroy() {
        jobHandler.removeCallbacks(pollJob);
        gifPreviewHandler.removeCallbacks(loopGifPreview);
        if (progressDialog != null) progressDialog.dismiss();
        if (compressionInfo != null) compressionInfo.dismiss();
        super.onDestroy();
    }

    private AlertDialog createProgressDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_compression, null);
        fillRetryFields(dialogView);
        if (retryDraft != null) {
            int[] ids = retryFieldIds();
            for (int i = 0; i < ids.length && i < retryDraft.length; i++)
                ((EditText) dialogView.findViewById(ids[i])).setText(retryDraft[i]);
        }
        dialogView.findViewById(R.id.compression_info).setOnClickListener(this::showCompressionInfo);
        dialogView.findViewById(R.id.use_current_settings).setOnClickListener(v -> fillRetryFields(dialogView));
        dialogView.findViewById(R.id.retry_compression_button).setOnClickListener(v -> retryCompression(dialogView));
        dialogView.findViewById(R.id.cancel_compression).setOnClickListener(v -> {
            try {
                CompressionJob job = CompressionJob.load(this, jobId);
                if (job != null) job.request("cancel", null);
                compressionControlError = "";
                refreshJob();
            } catch (Exception e) { showControlError(dialogView, e.getMessage()); }
        });
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogCustom)
                .setView(dialogView)
                .setCancelable(false)
                .setPositiveButton("Minimizar", (d, which) -> moveTaskToBack(true))
                .setNegativeButton("Fechar", (d, which) -> {
                    progressHidden = true;
                    mostrarVideo();
                });
        if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            builder.setNeutralButton("Ativar notificações", (d, which) -> startActivity(
                    new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName())));
        }
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.DKGRAY);
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.DKGRAY);
            android.util.DisplayMetrics metrics = getResources().getDisplayMetrics();
            if (dialog.getWindow() != null) {
                dialog.getWindow().setLayout((int) (metrics.widthPixels * .95), (int) (metrics.heightPixels * .85));
                dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            }
        });
        return dialog;
    }

    private static int[] retryFieldIds() {
        return new int[]{R.id.retry_quality, R.id.retry_fps, R.id.retry_compression, R.id.retry_speed, R.id.retry_detail};
    }

    private String[] readRetryDraft() {
        int[] ids = retryFieldIds();
        String[] values = new String[ids.length];
        for (int i = 0; i < ids.length; i++) values[i] = ((EditText) progressDialog.findViewById(ids[i])).getText().toString();
        return values;
    }

    private void fillRetryFields(View view) {
        CompressionJob job = CompressionJob.load(this, jobId);
        if (job == null) return;
        org.json.JSONObject values = job.data.optJSONObject("currentAttempt");
        if (values == null) values = job.data;
        String[] keys = {"quality", "fps", "compression", "speed", "detailSize"};
        String[] defaults = {"75", "30", "6", "1", "512"};
        int[] ids = retryFieldIds();
        for (int i = 0; i < ids.length; i++) ((EditText) view.findViewById(ids[i])).setText(values.optString(keys[i], defaults[i]));
    }

    private static String attemptSettings(org.json.JSONObject settings) {
        return String.format(Locale.getDefault(), "Qualidade %s • %s FPS • compressão %s\nVelocidade %s× • nitidez %s",
                settings.optString("quality"), settings.optString("fps"), settings.optString("compression"),
                settings.optString("speed"), settings.optString("detailSize", "512"));
    }

    private void showControlError(View view, String message) {
        compressionControlError = message == null ? "Não foi possível aplicar a ação." : message;
        ((TextView) view.findViewById(R.id.compression_action_status)).setText(compressionControlError);
    }

    private void retryCompression(View view) {
        try {
            String[] values = readRetryDraft();
            CompressionSettings settings = new CompressionSettings(Integer.parseInt(values[0].trim()),
                    Integer.parseInt(values[1].trim()), Integer.parseInt(values[2].trim()),
                    Double.parseDouble(values[3].trim().replace(',', '.')), Integer.parseInt(values[4].trim()));
            CompressionJob job = CompressionJob.load(this, jobId);
            if (job == null) throw new IllegalStateException("Tarefa não encontrada.");
            if (job.data.has("sourceSeconds")) settings.validateDuration(job.data.optDouble("sourceSeconds"));
            if (job.running()) job.request("retry", settings);
            else {
                job = CompressionJob.restart(this, job, settings);
                jobId = job.id;
                getIntent().putExtra(CompressionJob.EXTRA_ID, jobId);
                jobRendered = false;
                controleStickers.clear();
                btnSalvar.setEnabled(false);
                CompressionService.start(this, job);
            }
            velocidade = String.valueOf(settings.speed); velocidadeFps = String.valueOf(settings.fps);
            compressionControlError = "";
            quality = String.valueOf(settings.quality); compression = String.valueOf(settings.compression);
            ((android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(view.getWindowToken(), 0);
            view.clearFocus();
            refreshJob();
        } catch (NumberFormatException e) { showControlError(view, "Preencha todos os campos com números válidos."); }
        catch (Exception e) { showControlError(view, e.getMessage()); }
    }

    private void showCompressionInfo(View anchor) {
        if (compressionInfo != null && compressionInfo.isShowing()) { compressionInfo.dismiss(); return; }
        CompressionJob job = CompressionJob.load(this, jobId);
        String warning = job == null ? "" : CompressionService.notice(this, job);
        TextView text = new TextView(this);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        text.setPadding(padding, padding, padding, padding);
        text.setTextColor(Color.BLACK);
        text.setText((warning.isEmpty() ? "" : warning + "\n\n")
                + "Você pode minimizar o app. A compressão continua em segundo plano.\n\n"
                + "Reiniciar interrompe a tentativa atual e reinicia a busca automática com os valores dos campos. "
                + "No modo de quatro figurinhas, todas são refeitas. Cancelar para o processo e mantém este diálogo aberto.\n\n"
                + "Qualidade e FPS menores podem reduzir o tamanho. Nitidez menor reduz detalhes, mantendo 512×512. "
                + "Compressão maior exige mais trabalho do conversor e pode demorar mais."
                + (NotificationManagerCompat.from(this).areNotificationsEnabled() ? "" : "\n\nAtive as notificações para acompanhar fora do app."));
        android.graphics.drawable.GradientDrawable background = new android.graphics.drawable.GradientDrawable();
        background.setColor(Color.WHITE); background.setCornerRadius(padding / 2f);
        android.widget.ScrollView scroll = new android.widget.ScrollView(this); scroll.addView(text);
        int width = Math.min(getResources().getDisplayMetrics().widthPixels - padding * 2, padding * 23);
        compressionInfo = new android.widget.PopupWindow(scroll, width,
                Math.min(getResources().getDisplayMetrics().heightPixels / 2, padding * 28), true);
        compressionInfo.setBackgroundDrawable(background); compressionInfo.setElevation(padding / 2f);
        compressionInfo.setOutsideTouchable(true);
        compressionInfo.showAsDropDown(anchor, anchor.getWidth() - width, 0);
    }

    private void showResult(File webpFile, String webpInfos) {
        stickerContainerExodia.setVisibility(exodia ? View.VISIBLE : View.GONE);
        stickerContainerNaoExodia.setVisibility(exodia ? View.GONE : View.VISIBLE);

        ControleSticker controleSticker = new ControleSticker();
        controleSticker.nomeArquivo = webpFile.getName();
        controleSticker.gerouCerto = true;
        controleSticker.webpInfos = webpInfos;
        controleStickers.add(controleSticker);

        if (!exodia) {
            SimpleDraweeView simpleDraweeView = findViewById(R.id.output_cropped_webp_s);
            ProgressBar progressBar = findViewById(R.id.output_cropped_webp_l);

            setupController(simpleDraweeView, new File(outputDirectory(), "output_cropped.webp"));

            simpleDraweeView.setOnClickListener(view2 ->
                    Toast.makeText(this, webpInfos, Toast.LENGTH_SHORT).show());

            simpleDraweeView.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
        } else {
            for (int i = 0; i < controleStickers.size(); i++) {
                final ControleSticker cs = controleStickers.get(i);
                String nomeId = cs.nomeArquivo.replace(".", "_");
                int simpleDraweeViewId = getResources().getIdentifier(nomeId + "_s", "id", getPackageName());
                int progressBarId = getResources().getIdentifier(nomeId + "_l", "id", getPackageName());
                SimpleDraweeView simpleDraweeView = findViewById(simpleDraweeViewId);
                ProgressBar progressBar = findViewById(progressBarId);

                setupController(simpleDraweeView, new File(outputDirectory(), cs.nomeArquivo));

                simpleDraweeView.setOnClickListener(view2 ->
                        Toast.makeText(this, cs.webpInfos, Toast.LENGTH_SHORT).show()
                );

                simpleDraweeView.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
            }
        }

        mostrarVideo();

        if ((exodia && controleStickers.size() == 4) || (!exodia && controleStickers.size() == 1)) {
            runOnUiThread(() -> {
                if (progressDialog != null) progressDialog.dismiss();
            });
        }
    }

    private void mostrarVideo() {
        videoContainer.setVisibility(View.VISIBLE);
        progressBarVideoView.setVisibility(View.GONE);
        bottomButtons.setVisibility(View.VISIBLE);
    }

    // helper para atribuir Uri e animações
    private void setupController(SimpleDraweeView view, File file) {
        Uri uri = Uri.fromFile(file)
                .buildUpon()
                .appendQueryParameter("t", String.valueOf(System.currentTimeMillis()))
                .build();
        DraweeController controller = Fresco.newDraweeControllerBuilder()
                .setUri(uri)
                .setAutoPlayAnimations(true)
                .build();
        view.setController(controller);
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
        File stickerPackDir = new File(FilesHelper.getAssetsDir(), stickerPack.identifier);
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
        // A pack may have been edited while this job was running in the background.
        try {
            for (StickerPack current : StickerPackLoader.fetchStickerPacks(this)) {
                if (current.identifier.equals(stickerPack.identifier)) {
                    stickerPack = current;
                    break;
                }
            }
        } catch (Exception e) { android.util.Log.w("CropVideoActivity", "Atualização do pacote", e); }
        if (exodia && (stickerPack.getStickers().size() + 4) > 30) {
            mostrarVideo();
            String msg = "Não é possível adicionar mais 4 figurinhas nesse pacote, pois em um pacote é permitido " +
                    "30, se criarmos mais 4 nesse pacote ele ficará com "
                    + (stickerPack.getStickers().size() + 4);

            AlertDialogHelper.showAlertDialog(msg, CropVideoActivity.this);
            return;
        }
        if (!exodia && (stickerPack.getStickers().size() + 1) > 30) {
            mostrarVideo();
            String msg = "Não é possível adicionar mais 1 figurinha nesse pacote, pois em um pacote é permitido " +
                    "30, se criarmos mais 1 nesse pacote ele ficará com "
                    + (stickerPack.getStickers().size() + 1);

            AlertDialogHelper.showAlertDialog(msg, CropVideoActivity.this);
            return;
        }

        // Cria lista de arquivos temporários (1 ou 4) para envio
        List<File> tempFiles = new ArrayList<>();
        if (!exodia) {
            tempFiles.add(new File(outputDirectory(), "output_cropped.webp"));
        } else {
            for (int i = 0; i < 4; i++) {
                tempFiles.add(new File(outputDirectory(), String.format("output_exodia_%d.webp", i)));
            }
        }

        for (int i = 0; i < tempFiles.size(); i++) {
            if (!tempFiles.get(i).exists()) {
                Toast.makeText(this, "Uma figurinha não existe", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        byte[] bytes = getBytes(tempFiles.get(0));
        WebPImage webPImage = WebPImage.createFromByteArray(bytes, ImageDecodeOptions.defaults());
        boolean ehAnimado = webPImage.getFrameCount() > 1;

        if ((stickerPack.animatedStickerPack && ehAnimado) ||
                (!stickerPack.animatedStickerPack && !ehAnimado)) {

            moverFigurinhasParaPasta(tempFiles, stickerPack);

            return;
        }

        List<StickerPack> stickerPacks = StickerPackLoader.fetchStickerPacks(this).stream()
                .filter(x -> x.animatedStickerPack == ehAnimado)
                .collect(Collectors.toList());

        String texto1 = ehAnimado ? "animada" : "estática";
        String texto2 = ehAnimado ? "animadas" : "estáticas";

        if (stickerPacks.isEmpty()) {
            String msg = "Figurinhas " + texto2 + " só podem ser criadas em pacotes de figurinhas " + texto2 + ".\n\n" +
                    "Não existem pacotes de figurinhas " + texto2 + " para você selecionar. " +
                    "Volte à tela inicial e crie um pacote de figurinhas " + texto2 + ".";
            AlertDialogHelper.showAlertDialog(msg, CropVideoActivity.this);
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.dialog_whatsapp, null);
        Spinner spinner = dialogView.findViewById(R.id.spinnerContatos);

        ContatoSpinnerAdapter adapter = new ContatoSpinnerAdapter(this, stickerPacks);
        spinner.setAdapter(adapter);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Atenção");
        builder.setMessage("Figurinhas " + texto2 + " só podem ser criadas em pacotes de figurinhas " + texto2 + "." +
                "\n\nSelecione um pacote de figurinha " + texto1 + " para colocar essa figurinha.");
        builder.setView(dialogView);
        builder.setPositiveButton("Salvar", (dialog, which) -> {
            StickerPack escolhido = (StickerPack) spinner.getSelectedItem();

            moverFigurinhasParaPasta(tempFiles, escolhido);
        });
        builder.setNegativeButton("Cancelar", (dialog, which) -> {
            dialog.dismiss();
        });

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> {
            // 5. Change button colors to red
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                    .setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark));
        });
        dialog.show();
    }

    private void moverFigurinhasParaPasta(List<File> tempFiles, StickerPack escolhido) {
        ArrayList<Sticker> stickers = new ArrayList<>();
        boolean naoDeuErro = true;
        for (int i = 0; i < tempFiles.size(); i++) {
            File fileTemp = tempFiles.get(i);

            byte[] bytes = getBytes(fileTemp);
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

            File outputFileInAssets = new File(FilesHelper.getAssetsDir(), escolhido.identifier + "/"
                    + getNextStickerPrefix() + "_" + System.currentTimeMillis() + ".webp");

            try {
                FileInputStream fis = new FileInputStream(fileTemp);
                FileOutputStream fos = new FileOutputStream(outputFileInAssets);

                byte[] buffer = new byte[1024];
                int length;
                while ((length = fis.read(buffer)) > 0) {
                    fos.write(buffer, 0, length);
                }

                fis.close();
                fos.close();

                stickers.add(new Sticker(outputFileInAssets.getName(), Arrays.asList("😂", "🎉"), ""));
            } catch (Exception e) {
                naoDeuErro = false;
                e.printStackTrace();
                Toast.makeText(this, e.getMessage() + "\n" + Objects.requireNonNull(e.getCause()).getMessage(), Toast.LENGTH_SHORT).show();
            }
        }

        if (naoDeuErro) {
            for (int i = 0; i < stickers.size(); i++) {
                escolhido.getStickers().add(stickers.get(i));
            }

            ContentsJsonHelper.atualizaContentsJsonAndContentProvider(this);
            ContentsJsonHelper.stickerPackAlterado = escolhido;
            ContentsJsonHelper.stickersAlterados = stickers;
            CompressionJob.consume(this, jobId);
            if (jobId != null) getSystemService(NotificationManager.class)
                    .cancel(CompressionService.notificationId(jobId));

            finish();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override public void onBackPressed() {
        CompressionJob job = CompressionJob.load(this, jobId);
        if (job != null) {
            try { job.dismissEditor(this); }
            catch (Exception e) { android.util.Log.w("CompressionJob", "Fechar editor", e); }
        }
        super.onBackPressed();
    }
}
