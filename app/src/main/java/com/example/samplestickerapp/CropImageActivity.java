package com.example.samplestickerapp;

import static com.example.samplestickerapp.StickerPackValidator.IMAGE_HEIGHT;
import static com.example.samplestickerapp.StickerPackValidator.IMAGE_WIDTH;
import static com.example.samplestickerapp.StickerPackValidator.KB_IN_BYTES;
import static com.example.samplestickerapp.StickerPackValidator.STATIC_STICKER_FILE_LIMIT_KB;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.ReturnCode;
import com.arthenica.ffmpegkit.Session;
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

public class CropImageActivity extends AppCompatActivity {

    private LockableScrollView lockableScrollView;
    private ImageView imageView;
    private FrameLayout videoContainer;
    private LinearLayout bottomButtons;
    private ProgressBar progressBarVideoView;
    private CropOverlayView cropOverlay;
    private FrameLayout stickerContainerNaoExodia;
    private LinearLayout stickerContainerExodia;
    private int videoOriginalWidth, videoOriginalHeight; // TODO: Renomear
    ImageButton btnSalvar, btnCrop, btnExodia;
    private StickerPack stickerPack;
    private AlertDialog progressDialog;
    boolean exodia;
    private List<ControleSticker> controleStickers = new ArrayList<>();
    private File imageFile;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_display);

        imageView = findViewById(R.id.imageView_1);
        lockableScrollView = findViewById(R.id.lockableScrollView_1);
        progressBarVideoView = findViewById(R.id.sticker_loader_video_view_1);
        cropOverlay = findViewById(R.id.crop_overlay_1);
        videoContainer = findViewById(R.id.video_container_1);
        bottomButtons = findViewById(R.id.bottom_buttons_1);
        btnCrop = findViewById(R.id.btn_crop_1);
        btnExodia = findViewById(R.id.btn_exodia_1);
        btnSalvar = findViewById(R.id.btn_salvar_1);
        stickerContainerNaoExodia = findViewById(R.id.stickerContainerNaoExodia_1);
        stickerContainerExodia = findViewById(R.id.stickerContainerExodia_1);

        cropOverlay.setOnOutsideCropClickListener((boolean flag) -> {
            lockableScrollView.setScrollingEnabled(flag);
        });

        btnCrop.setOnClickListener(v -> {
            exodia = false;
            criarFigurinha();
        });
        btnExodia.setOnClickListener(v -> {
            exodia = true;
            criarFigurinha();
        });
        btnSalvar.setOnClickListener(v -> enviarWhatsapp());

        stickerPack = getIntent().getParcelableExtra("sticker_pack");
        String path = getIntent().getStringExtra("file_path");
        imageFile = new File(Objects.requireNonNull(path));

        if (imageFile.exists()) {
            DisplayMetrics metrics = getResources().getDisplayMetrics();
            int screenW = metrics.widthPixels;

            imageView.setAdjustViewBounds(true);
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            imageView.setMaxHeight(screenW);

            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(imageFile.getAbsolutePath(), opts);
            videoOriginalWidth = opts.outWidth;
            videoOriginalHeight = opts.outHeight;

            // calcula nova altura proporcional
            float aspect = (float) videoOriginalWidth / (float) videoOriginalHeight;
            int finalW, finalH;
            if (aspect > 1f) {
                finalW = screenW;
                finalH = Math.round(screenW / aspect);
            } else {
                finalH = screenW;
                finalW = Math.round(screenW * aspect);
            }

            // decodifica Bitmap real
            opts.inJustDecodeBounds = false;
            Bitmap original = BitmapFactory.decodeFile(imageFile.getAbsolutePath(), opts);
            Bitmap scaled = Bitmap.createScaledBitmap(original, finalW, finalH, true);

            // 5. Exibe no ImageView
            imageView.setImageBitmap(scaled);

            FrameLayout.LayoutParams videoLp = new FrameLayout.LayoutParams(finalW, finalH);
            videoLp.gravity = Gravity.CENTER;
            imageView.setLayoutParams(videoLp);

            LinearLayout.LayoutParams containerLp = new LinearLayout.LayoutParams(screenW, screenW);
            containerLp.gravity = Gravity.CENTER_HORIZONTAL;
            videoContainer.setLayoutParams(containerLp);

            cropOverlay.setMaxCropSize(screenW);
            cropOverlay.centerInitialCrop(screenW, screenW);

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                exodia = false;
                criarFigurinha();
            }, 100);
        }
    }

    public void criarFigurinha() {
        if (exodia && (stickerPack.getStickers().size() + 4) > 30) {
            mostrarVideo();
            Toast.makeText(CropImageActivity.this,
                    "Não é possível adicionar mais 4 figurinhas nesse pacote, pois em um pacote é permitido " +
                            "30 figurinhas, se gerarmos mais 4 nesse pacote ele ficará com "
                            + (stickerPack.getStickers().size() + 4) + " figurinhas", Toast.LENGTH_LONG).show();
            return;
        }
        if (!exodia && (stickerPack.getStickers().size() + 1) > 30) {
            mostrarVideo();
            Toast.makeText(CropImageActivity.this,
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

        // Probe video to get original dimensions & duration
        final int origSquare = Math.max(videoOriginalWidth, videoOriginalHeight);

        final long MAX_SIZE = 100 * 1024;
        progressDialog = createProgressDialog();
        progressDialog.show();

        class Retry {
            int quality = 100;
            int compression = 4;
            int attempt = 0;
            long sizeAnterior = 0;
            int sizeAnteriorTentativas = 0;

            void runAttempt() {
                progressDialog.show();
                attempt++;
                Rect r = cropOverlay.getCropRect();
                int containerSide = getResources().getDisplayMetrics().widthPixels;
                float mapFactor = (float) origSquare / containerSide;
                int baseCropX = Math.round(r.left * mapFactor);
                int baseCropY = Math.round(r.top * mapFactor);
                int baseCropW = Math.round(r.width() * mapFactor);
                int baseCropH = Math.round(r.height() * mapFactor);

                if (!exodia) {
                    File out = new File(FilesHelper.getTempDir(), "output_cropped.webp");
                    resetProgressUI(out.getName(), attempt);
                    processQuadrant(baseCropX, baseCropY, baseCropW, baseCropH, out, this);
                } else {
                    int halfW = baseCropW / 2;
                    int halfH = baseCropH / 2;
                    for (int i = 0; i < 4; i++) {
                        int offsetX = baseCropX + (i % 2) * halfW;
                        int offsetY = baseCropY + (i / 2) * halfH;
                        File out = new File(FilesHelper.getTempDir(), String.format("output_exodia_%d.webp", i));
                        resetProgressUI(out.getName(), attempt);
                        processQuadrant(offsetX, offsetY, halfW, halfH, out, this);
                    }
                }
            }

            void processQuadrant(int cropX, int cropY, int cropW, int cropH,
                                 File webpFile, Retry ctx) {


                String vfFilter = String.format(Locale.US,
                        "format=rgba,pad=%d:%d:(%d-iw)/2:(%d-ih)/2:color=#00000000," +
                                "crop=%d:%d:%d:%d," +
                                "scale=512:512:force_original_aspect_ratio=decrease," +
                                "pad=512:512:(ow-iw)/2:(oh-ih)/2:color=#00000000",
                        origSquare, origSquare, origSquare, origSquare,
                        cropW, cropH, cropX, cropY
                );
                String cmd = String.format(Locale.US,
                        "-y -i \"%s\" -vf \"%s\" -frames:v 1 -c:v libwebp -lossless 0 " +
                                "-q:v %d -compression_level %d -preset default \"%s\"",
                        imageFile.getAbsolutePath(), vfFilter, ctx.quality, ctx.compression,
                        webpFile.getAbsolutePath());

                FFmpegKit.executeAsync(cmd,
                        session -> {
                            runOnUiThread(() -> {
                                onComplete(session, webpFile, ctx);
                            });
                        },
                        log -> {
                        },
                        statistics -> updateProgress(webpFile.getName(), attempt)
                );

            }

            void onComplete(Session session, File webpFile, Retry ctx) {
                if (!ReturnCode.isSuccess(session.getReturnCode())) {
                    showError(session);
                    return;
                }
                String webpInfos = "";
                try {
                    // EXTRAI E EXIBE AS INFOS DO WebP
                    byte[] bytes = getBytes(webpFile);
                    webpInfos = String.format(Locale.US, "Tamanho: %.2fKB", bytes.length / 1024f
                    );
                } catch (Exception e) {
                    e.printStackTrace();
                }
                String finalWebpInfos = webpInfos;

                runOnUiThread(() -> {
                    TextView infoTv = progressDialog.findViewById(R.id.webp_info_tv);
                    infoTv.setText(finalWebpInfos);
                });

                long size = webpFile.length();
                if (ctx.sizeAnterior == 0) ctx.sizeAnterior = size;
                else if (ctx.sizeAnterior != size) ctx.sizeAnterior = size;
                else {
                    if (ctx.sizeAnteriorTentativas > 3) {
                        runOnUiThread(() -> {
                            Toast.makeText(CropImageActivity.this,
                                    "Não foi possível reduzir abaixo de 100KB, ajuste e tente novamente", Toast.LENGTH_SHORT).show();
                            showResult(webpFile, finalWebpInfos);
                        });
                        return;
                    }
                    ctx.sizeAnteriorTentativas++;
                }
                if (size <= MAX_SIZE) {
                    runOnUiThread(() -> {
                        showResult(webpFile, finalWebpInfos);
                    });
                } else {
                    if (ctx.quality > 10) ctx.quality = Math.max(10, ctx.quality - 1);
                    ctx.runAttempt();
                }
            }
        }

        new Retry().runAttempt();
    }

    private AlertDialog createProgressDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_progress, null);
        ProgressBar progressBar = dialogView.findViewById(R.id.progress_bar);
        TextView attemptsTv = dialogView.findViewById(R.id.attempts_tv);
        progressBar.setMax(100);
        attemptsTv.setText("Reduzindo a figurinha até 100Kb \nTentativa: 0");
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
            tv.setText("Reduzindo a figurinha até 100Kb \nTentativa: " + attempt);
        });
    }

    private void updateProgress(String nomeArquivo, int attempt) {
        runOnUiThread(() -> {
            TextView tv = progressDialog.findViewById(R.id.attempts_tv);
            tv.setText("Reduzindo a figurinha até 100Kb \nTentativa: " + attempt);
        });
    }

    private void showError(Session session) {
        runOnUiThread(() -> {
            progressDialog.dismiss();
            Toast.makeText(this, "FFmpeg falhou: " + session.getFailStackTrace(), Toast.LENGTH_LONG).show();
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
                //.setAutoPlayAnimations(true)
                .build();
        view.setController(controller);
    }

    private void enviarWhatsapp() {
        if (exodia && (stickerPack.getStickers().size() + 4) > 30) {
            mostrarVideo();
            String msg = "Não é possível adicionar mais 4 figurinhas nesse pacote, pois em um pacote é permitido " +
                    "30, se criarmos mais 4 nesse pacote ele ficará com "
                    + (stickerPack.getStickers().size() + 4);

            AlertDialogHelper.showAlertDialog(msg, CropImageActivity.this);
            return;
        }
        if (!exodia && (stickerPack.getStickers().size() + 1) > 30) {
            mostrarVideo();
            String msg = "Não é possível adicionar mais 1 figurinha nesse pacote, pois em um pacote é permitido " +
                    "30, se criarmos mais 1 nesse pacote ele ficará com "
                    + (stickerPack.getStickers().size() + 1);

            AlertDialogHelper.showAlertDialog(msg, CropImageActivity.this);
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
            AlertDialogHelper.showAlertDialog(msg, CropImageActivity.this);
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
            WebPImage webPImage = WebPImage.createFromByteArray(bytes, ImageDecodeOptions.defaults());

            if (bytes.length > STATIC_STICKER_FILE_LIMIT_KB * KB_IN_BYTES) {
                Toast.makeText(this, "figurinhas estáticas tem que ter tamanho menor q "
                        + STATIC_STICKER_FILE_LIMIT_KB + "KB, tamanho atual "
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
}
