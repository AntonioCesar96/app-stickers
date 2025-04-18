/*
 * Copyright (c) WhatsApp Inc. and its affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.example.samplestickerapp;

import android.graphics.ImageDecoder;
import android.graphics.drawable.Animatable2;
import android.graphics.drawable.AnimatedImageDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.ReturnCode;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class CriarFigurinhaActivity extends BaseActivity {
    public static final String EXTRA_STICKER_PACK_DATA = "sticker_pack_01";

    private StickerPack stickerPack;
    private EditText etVideoUrl;
    private TextView tvErro;
    private Button btnBaixar;
    private Button btnEnviarWhats;
    private SimpleDraweeView expandedStickerPreview;
    private ProgressBar stickerLoader;
    private VideoView vvOriginal;
    //private PlayerView playerView;
    private File videoOriginalFile;
    private File outputFile;

    //private String url = "https://external.fcgr3-1.fna.fbcdn.net/emg1/v/t13/8567978231481450529?stp=dst-src&url=https%3A%2F%2Fmedia2.giphy.com%2Fmedia%2Fv1.Y2lkPTA1NzQyMTNjNGE0YWdibDJtbW0xd2tob3NybzdpZHlpczU2a3NjZjFmZXV6bGpuNyZlcD12MV9naWZzJmN0PWc%2F3xz2BLBOt13X9AgjEA%2F200.gif&utld=giphy.com&_nc_gid=EuCz5oeQvE21QdSKISiwCA&_nc_oc=AdkjMkY0P_2Wj889eNd92izUKXiO9aoEOHGSUCIQc2JA-qyfS-DOd0QuUYuntygS9ShJKOt5sWEFBP6eDgCP55Ge&ccb=13-1&oh=06_Q39-yEcZCMRP-60jvOheo1UVIGiPo8vjIDvCuBCk6Wn4_L4&oe=67FD9B00&_nc_sid=1d65fc"
    //private String url = "https://media.tenor.com/QA_IqSKoWTcAAAPs/the-rock.webm"
    private String url = "https://media.tenor.com/QA_IqSKoWTcAAAPo/the-rock.mp4";

    //private ExoPlayer exoPlayer;
    private long playbackPosition = 0L;
    private boolean playWhenReady = true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_criar_figurinha);

        stickerPack = getIntent().getParcelableExtra(EXTRA_STICKER_PACK_DATA);

        etVideoUrl = findViewById(R.id.etVideoUrl);
        tvErro = findViewById(R.id.tvErro);
        btnBaixar = findViewById(R.id.btnBaixar);
        btnEnviarWhats = findViewById(R.id.btnEnviarWhats);
        expandedStickerPreview = findViewById(R.id.sticker_details_expanded_sticker);
        stickerLoader = findViewById(R.id.sticker_loader);
        vvOriginal = findViewById(R.id.vvOriginal);
        //playerView = findViewById(R.id.playerView);

        btnBaixar.setOnClickListener(view -> downloadVideo());
        btnEnviarWhats.setOnClickListener(view -> enviarWhatsapp());

        etVideoUrl.setText(url);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Pastas e Figurinhas");
        }
    }

    private void downloadVideo() {
        if (etVideoUrl.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Informe URL", Toast.LENGTH_SHORT).show();
            return;
        }

        stickerLoader.setVisibility(View.VISIBLE);
        expandedStickerPreview.setVisibility(View.INVISIBLE);

        String url = etVideoUrl.getText().toString().trim();

        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder().url(url).build();
                Response response = client.newCall(request).execute();
                ResponseBody body = response.body();

                if (body != null) {
                    String extensaoArquivo = body.contentType().subtype();

                    File rootDir = Environment.getExternalStorageDirectory();
                    File tempDir = new File(rootDir, "00-Figurinhas/temp");
                    if (!tempDir.exists())
                        tempDir.mkdirs();

                    videoOriginalFile = new File(tempDir, "video_original." + extensaoArquivo);

                    FileOutputStream fos = new FileOutputStream(videoOriginalFile);
                    fos.write(body.bytes());
                    fos.close();

                    // TODO: Colocar na pasta da figurinha só depois
                    File stickerPackDir = new File(rootDir, "00-Figurinhas/assets/" + stickerPack.identifier);
                    String prefix = getNextStickerPrefix(stickerPackDir);
                    outputFile = new File(tempDir, prefix + "_" + System.currentTimeMillis() + ".webp");
                    if (outputFile.exists()) {
                        outputFile.delete();
                        outputFile = new File(tempDir, prefix + "_" + System.currentTimeMillis() + ".webp");
                    }

                    String ffmpegCommand = "-i " + videoOriginalFile.getAbsolutePath() + " -vf scale=512:512 -loop 0 -preset default -an -vsync 0 " + outputFile.getAbsolutePath();

                    File finalOutputFile = outputFile;
                    FFmpegKit.executeAsync(ffmpegCommand, session -> {
                        ReturnCode returnCode = session.getReturnCode();
                        if (ReturnCode.isSuccess(returnCode)) {
                            runOnUiThread(() -> {

                                runOnUiThread(() -> {
                                    final Uri stickerAssetUri = Uri.fromFile(finalOutputFile);
                                    DraweeController controller = Fresco.newDraweeControllerBuilder()
                                            .setUri(stickerAssetUri)
                                            .setAutoPlayAnimations(true)
                                            .build();

                                    expandedStickerPreview.setImageResource(R.drawable.sticker_error);
                                    expandedStickerPreview.setController(controller);

                                    expandedStickerPreview.setVisibility(View.VISIBLE);
                                    stickerLoader.setVisibility(View.INVISIBLE);

                                    Toast.makeText(this, "Conversão concluído", Toast.LENGTH_SHORT).show();
                                });
                            });
                        } else {
                            runOnUiThread(() -> Toast.makeText(this, "Erro na conversão do vídeo.", Toast.LENGTH_LONG).show());
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> tvErro.setText("Falha no download: " + e.getMessage()));
            }
        }).start();
    }

    public static String getNextStickerPrefix(File stickerPackDir) {
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
                    // Ignora arquivos que não seguem o padrão esperado
                }
            }
        }

        int max = numbers.isEmpty() ? 0 : Collections.max(numbers);
        int next = max + 1;

        return String.format("%02d", next);
    }

    private void enviarWhatsapp() {
        if(videoOriginalFile == null || outputFile == null) {
            Toast.makeText(this, "Arquivo não existe", Toast.LENGTH_SHORT).show();
            return;
        }

        File rootDir = Environment.getExternalStorageDirectory();
        File outputFileInAssets = new File(rootDir, "00-Figurinhas/assets/" + stickerPack.identifier + "/" + outputFile.getName());

        try {
            FileInputStream fis = new FileInputStream(outputFile);
            FileOutputStream fos = new FileOutputStream(outputFileInAssets);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }

            fis.close();
            fos.close();

            ContentsJsonHelper.atualizaContentsJsonAndContentProvider(this);
            ContentsJsonHelper.stickerAlteradoTelaCriar = new Sticker(outputFile.getName(), Arrays.asList("😂", "🎉"), "");

            finish();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, e.getMessage() + "\n" + Objects.requireNonNull(e.getCause()).getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}