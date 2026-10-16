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

import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.FFprobeKit;
import com.arthenica.ffmpegkit.MediaInformation;
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
import java.io.IOException;
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
    private long activeConversionId = -1;
    private File exportSource;
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
        setContentView(R.layout.activity_crop_video);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Cortar(Crop)");
        }

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
        exportSource = gifSource ? new File(originalGif) : videoFile;
        gifStartMs = getIntent().getLongExtra(GifMedia.START_MS, 0);
        gifEndMs = getIntent().getLongExtra(GifMedia.END_MS, 0);

        videoView.setVideoPath(videoFile.getAbsolutePath());
        videoView.setOnPreparedListener(mp -> {
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

        // inicia conversão (exemplo)
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            exodia = false;
            convertMp4ToWebpAdaptive();
        }, 100);
    }

    @Override
    protected void onResume() {
        super.onResume();

        videoView.start();
        btnPause.setImageResource(R.drawable.ic_pause);
        if (gifSource) {
            gifPreviewHandler.removeCallbacks(loopGifPreview);
            gifPreviewHandler.post(loopGifPreview);
        }
    }

    @Override
    protected void onPause() {
        gifPreviewHandler.removeCallbacks(loopGifPreview);
        if (gifSource) videoView.pause();
        super.onPause();
    }

    public void convertMp4ToWebpAdaptive() {
        if (converting || isFinishing() || isDestroyed()) return;
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

        final int initialQuality;
        final int compressionLvl;
        final int requestedFps;
        final double speed;
        final MediaInformation info;
        final StreamInformation vStream;
        try {
            initialQuality = Integer.parseInt(quality);
            compressionLvl = Integer.parseInt(compression);
            requestedFps = Integer.parseInt(velocidadeFps);
            speed = Double.parseDouble(velocidade.replace(',', '.'));
            new AdaptiveWebpSearch(initialQuality, requestedFps);
            if (compressionLvl < 0 || compressionLvl > 6 || !Double.isFinite(speed) || speed <= 0) {
                throw new IllegalArgumentException("Use velocidade maior que zero e compressão de 0 a 6.");
            }
            MediaInformationSession probe = FFprobeKit.getMediaInformation(exportSource.getAbsolutePath());
            info = probe.getMediaInformation();
            if (info == null) throw new IllegalArgumentException("Não foi possível ler o vídeo.");
            vStream = info.getStreams().stream()
                    .filter(s -> "video".equals(s.getType())).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Stream de vídeo não encontrado."));
            double sourceDuration = Double.parseDouble(info.getDuration());
            if (gifSource && (gifStartMs < 0 || gifEndMs <= gifStartMs
                    || gifEndMs > Math.round(sourceDuration * 1000))) {
                throw new IllegalArgumentException("Intervalo do GIF inválido. Selecione o trecho novamente.");
            }
            double duration = (gifSource ? (gifEndMs - gifStartMs) / 1000.0 : sourceDuration) / speed;
            if (!Double.isFinite(duration) || duration <= 0 || duration > 10) {
                throw new IllegalArgumentException("Selecione um trecho de até 10 segundos na velocidade escolhida.");
            }
        } catch (Exception e) {
            mostrarVideo();
            Toast.makeText(this, "Configuração inválida: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }

        final int origSquare = Math.max(vStream.getWidth().intValue(), vStream.getHeight().intValue());
        // A GIF's average rate hides short frames between long holds; don't cap by it.
        final int initialFps = gifSource ? requestedFps : AdaptiveWebpSearch.sourceLimitedFps(
                requestedFps, vStream.getAverageFrameRate(), speed);
        final Rect crop = new Rect(cropOverlay.getCropRect());
        final float mapFactor = (float) origSquare / getResources().getDisplayMetrics().widthPixels;
        final int baseX = Math.max(0, Math.round(crop.left * mapFactor));
        final int baseY = Math.max(0, Math.round(crop.top * mapFactor));
        final int baseW = Math.min(origSquare - baseX, Math.round(crop.width() * mapFactor));
        final int baseH = Math.min(origSquare - baseY, Math.round(crop.height() * mapFactor));
        if (baseW < 2 || baseH < 2) {
            Toast.makeText(this, "Selecione uma área maior para recortar.", Toast.LENGTH_LONG).show();
            return;
        }
        final int count = exodia ? 4 : 1;
        final List<File> results = new ArrayList<>();
        final List<String> descriptions = new ArrayList<>();
        converting = true;
        btnSalvar.setEnabled(false);
        progressDialog = createProgressDialog();
        progressDialog.show();

        // One independent search per quadrant; never overwrite a running encoder's output.
        class Conversion {
            int quadrant;
            int attempt;
            AdaptiveWebpSearch search = new AdaptiveWebpSearch(initialQuality, initialFps);
            File candidate;
            File best;

            void runAttempt() {
                if (isFinishing() || isDestroyed()) return;
                attempt++;
                String name = count == 1 ? "output_cropped.webp"
                        : String.format(Locale.US, "output_exodia_%d.webp", quadrant);
                best = new File(FilesHelper.getTempDir(), name);
                candidate = new File(FilesHelper.getTempDir(), "candidate_" + name);
                int w = count == 1 ? baseW : baseW / 2;
                int h = count == 1 ? baseH : baseH / 2;
                int x = baseX + (count == 1 ? 0 : (quadrant % 2) * w);
                int y = baseY + (count == 1 ? 0 : (quadrant / 2) * h);
                String timeline = gifSource ? GifMedia.timelineFilter(gifStartMs, gifEndMs, speed, search.getFps())
                        : String.format(Locale.US, "setpts=(PTS-STARTPTS)/%.8f,fps=%d,", speed, search.getFps());
                String filter = timeline + String.format(Locale.US,
                        "format=rgba,pad=%d:%d:(ow-iw)/2:(oh-ih)/2:color=#00000000," +
                        "crop=%d:%d:%d:%d," +
                        "scale=512:512:force_original_aspect_ratio=decrease:flags=lanczos," +
                        "pad=512:512:(ow-iw)/2:(oh-ih)/2:color=#00000000",
                        origSquare, origSquare, w, h, x, y);
                resetProgressUI(name, attempt);
                TextView details = progressDialog.findViewById(R.id.webp_info_tv);
                details.setText(String.format(Locale.US,
                        "Figurinha %d/%d • %d FPS • qualidade %d", quadrant + 1, count,
                        search.getFps(), search.getQuality()));
                // Explicit presets in FFmpeg 6 override compression_level. Use no preset.
                String[] args = GifMedia.webpArguments(exportSource, gifSource, filter,
                        search.getQuality(), compressionLvl, candidate);
                activeConversionId = FFmpegKit.executeWithArgumentsAsync(args, session ->
                        runOnUiThread(() -> complete(session))).getSessionId();
            }

            void fail(String message) {
                converting = false;
                progressDialog.dismiss();
                if (candidate != null) candidate.delete();
                mostrarVideo();
                Toast.makeText(CropVideoActivity.this, message, Toast.LENGTH_LONG).show();
            }

            void complete(Session session) {
                activeConversionId = -1;
                if (isFinishing() || isDestroyed()) return;
                if (!ReturnCode.isSuccess(session.getReturnCode())) {
                    android.util.Log.e("WebpConversion", session.getAllLogsAsString());
                    fail("Não foi possível converter o vídeo. Tente outro trecho.");
                    return;
                }
                try {
                    long bytes = candidate.length();
                    if (bytes == 0) throw new IOException("O conversor gerou um arquivo vazio.");
                    android.util.Log.i("WebpConversion", String.format(Locale.US,
                            "part=%d attempt=%d fps=%d quality=%d bytes=%d",
                            quadrant, attempt, search.getFps(), search.getQuality(), bytes));
                    if (search.recordSize(bytes)) copyWebp(candidate, best);
                    if (!search.isDone()) {
                        runAttempt();
                        return;
                    }
                    candidate.delete();
                    if (!search.hasResult()) {
                        fail("Este trecho precisa de muita compressão. Encurte a duração ou recorte uma área menor para manter a fluidez em até 500 KB.");
                        return;
                    }
                    String description = describeWebp(best, search.getBestQuality());
                    results.add(best);
                    descriptions.add(description);
                    if (++quadrant < count) {
                        search = new AdaptiveWebpSearch(initialQuality, initialFps);
                        attempt = 0;
                        runAttempt();
                    } else {
                        converting = false;
                        for (int i = 0; i < results.size(); i++) showResult(results.get(i), descriptions.get(i));
                        btnSalvar.setEnabled(true);
                    }
                } catch (Exception e) {
                    fail("Não foi possível criar a figurinha: " + e.getMessage());
                }
            }
        }
        new Conversion().runAttempt();
    }

    private static void copyWebp(File source, File target) throws IOException {
        try (FileInputStream input = new FileInputStream(source);
             FileOutputStream output = new FileOutputStream(target)) {
            byte[] buffer = new byte[8192];
            int length;
            while ((length = input.read(buffer)) != -1) output.write(buffer, 0, length);
        }
    }

    private String describeWebp(File file, int qualityValue) throws IOException {
        WebPImage image = WebPImage.createFromByteArray(getBytes(file), ImageDecodeOptions.defaults());
        try {
            if (file.length() > AdaptiveWebpSearch.MAX_BYTES || image.getWidth() != 512 || image.getHeight() != 512
                    || image.getDuration() <= 0 || image.getDuration() > ANIMATED_STICKER_TOTAL_DURATION_MAX
                    || image.getFrameCount() < 2) {
                throw new IOException("O resultado não atende aos limites de uma figurinha animada.");
            }
            for (int duration : image.getFrameDurations()) {
                if (duration < ANIMATED_STICKER_FRAME_DURATION_MIN) {
                    throw new IOException("O resultado contém um quadro curto demais.");
                }
            }
            return String.format(Locale.US,
                    "FPS médio: %.1f\nDuração: %.2fs\nTamanho: %.2f KB\nQualidade: %d",
                    image.getFrameCount() * 1000.0 / image.getDuration(), image.getDuration() / 1000.0,
                    file.length() / 1024.0, qualityValue);
        } finally {
            image.dispose();
        }
    }

    @Override
    protected void onDestroy() {
        gifPreviewHandler.removeCallbacks(loopGifPreview);
        if (activeConversionId >= 0) FFmpegKit.cancel(activeConversionId);
        if (progressDialog != null) progressDialog.dismiss();
        super.onDestroy();
    }

    private AlertDialog createProgressDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_progress, null);
        ProgressBar progressBar = dialogView.findViewById(R.id.progress_bar);
        TextView attemptsTv = dialogView.findViewById(R.id.attempts_tv);
        progressBar.setMax(100);
        attemptsTv.setText("Reduzindo a figurinha até 500Kb \nTentativa: 0");
        return new AlertDialog.Builder(this)
                .setTitle("Criando figurinha")
                .setView(dialogView)
                .setCancelable(false)
                .create();
    }

    private void resetProgressUI(String nomeArquivo, int attempt) {
        runOnUiThread(() -> {
            ProgressBar pb = progressDialog.findViewById(R.id.progress_bar);
            TextView tv = progressDialog.findViewById(R.id.attempts_tv);
            pb.setProgress(0);
            tv.setText("Reduzindo a figurinha até 500Kb \nTentativa: " + attempt);
        });
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

            setupController(simpleDraweeView, new File(FilesHelper.getTempDir(), "output_cropped.webp"));

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

                setupController(simpleDraweeView, new File(FilesHelper.getTempDir(), cs.nomeArquivo));

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
                progressDialog.dismiss();
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
            tempFiles.add(new File(FilesHelper.getTempDir(), "output_cropped.webp"));
        } else {
            for (int i = 0; i < 4; i++) {
                tempFiles.add(new File(FilesHelper.getTempDir(), String.format("output_exodia_%d.webp", i)));
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
}
