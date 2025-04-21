/*
 * Copyright (c) WhatsApp Inc. and its affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.example.samplestickerapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.ReturnCode;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Objects;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;


public class CriarFigurinhaActivity extends BaseActivity {
    public static final String EXTRA_STICKER_PACK_DATA = "sticker_pack_01";

    private StickerPack stickerPack;
    private EditText etVideoUrl;
    private Button btnBaixar, btnLimpar, btnColar, btnTrim, btnCrop;
    private ProgressBar stickerLoader;
    private File videoOriginalFile;
    private File tempMp4File;

    private String urlInicial = "https://external.fcgr3-1.fna.fbcdn.net/emg1/v/t13/4952283205153878508?stp=dst-src&url=https%3A%2F%2Fmedia0.giphy.com%2Fmedia%2Fv1.Y2lkPWFlZWNjYzExeGE1dnYwaTNvYjV2NHg4dXd3ajA1eWRrbGlzY3M0dWRnejA2MXVuaCZlcD12MV9naWZzX2dpZklkJmN0PWc%2FlprIQG8Pl3T4gktKOZ%2F200.gif&utld=giphy.com&_nc_gid=48UGsFLBtQEczFYnwVTP4w&_nc_oc=AdkWc9aawUrTHSvPDbA8xgSWozbEGFY8D9kHuW6HjwtDZ59yQGcK4XZ7InaO_OlD8ys1Djr0TaW_fUYplV53D0kn&ccb=13-1&oh=06_Q3-yAYO4FKtG0fmDil1iurMOw5CtGTXidHeamSTglDoeEOV9&oe=6805F9C8&_nc_sid=1d65fc";
    //private String urlInicial = "https://media.tenor.com/QA_IqSKoWTcAAAPs/the-rock.webm";
    //private String urlInicial = "https://media.tenor.com/QA_IqSKoWTcAAAPo/the-rock.mp4";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_criar_figurinha);

        stickerPack = getIntent().getParcelableExtra(EXTRA_STICKER_PACK_DATA);

        etVideoUrl = findViewById(R.id.etVideoUrl);
        btnBaixar = findViewById(R.id.btnBaixar);
        btnLimpar = findViewById(R.id.btnLimpar);
        btnTrim = findViewById(R.id.btnTrim);
        btnColar = findViewById(R.id.btnColar);
        btnCrop = findViewById(R.id.btnCrop);
        stickerLoader = findViewById(R.id.sticker_loader);

        btnBaixar.setOnClickListener(view -> downloadVideo());
        btnLimpar.setOnClickListener(view -> limpar());
        btnColar.setOnClickListener(view -> colar());
        btnTrim.setOnClickListener(view -> trimVideo());
        btnCrop.setOnClickListener(view -> cropVideo());

        btnTrim.setVisibility(View.GONE);
        btnCrop.setVisibility(View.GONE);

        etVideoUrl.setText(urlInicial);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Pastas e Figurinhas");
        }

        File tempDir = new File(Environment.getExternalStorageDirectory(), "00-Figurinhas/temp/video_original.mp4");
        if(tempDir.exists()) {
            btnTrim.setVisibility(View.VISIBLE);
            btnCrop.setVisibility(View.VISIBLE);
        }

        //limparTemp();
    }

    private void trimVideo() {
        File tempDir = new File(Environment.getExternalStorageDirectory(), "00-Figurinhas/temp/video_original.mp4");
        if(!tempDir.exists()) {
            Toast.makeText(this, "Video da URL não foi baixado", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, CustomVideoRangeActivity.class);
        intent.putExtra("sticker_pack", stickerPack);
        startActivity(intent);
    }

    private void cropVideo() {
        File tempDir = new File(Environment.getExternalStorageDirectory(), "00-Figurinhas/temp/video_original.mp4");
        if(!tempDir.exists()) {
            Toast.makeText(this, "Video da URL não foi baixado", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, CropVideoActivity.class);
        intent.putExtra("sticker_pack", stickerPack);
        startActivity(intent);
    }

    private void downloadVideo() {
        if (etVideoUrl.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Informe URL", Toast.LENGTH_SHORT).show();
            return;
        }

        stickerLoader.setVisibility(View.VISIBLE);

        String url = etVideoUrl.getText().toString().trim();

        // TODO: baixar o video e converter pra MP4 se for outro formato

        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder().url(url).build();
                Response response = client.newCall(request).execute();
                ResponseBody body = response.body();

                if (body != null) {
                    String extensaoArquivo = body.contentType().subtype();

                    if(extensaoArquivo.equals("plain")) {
                        runOnUiThread(() -> Toast.makeText(this, "Há algo errado com a URL\nFormato baixado: " + extensaoArquivo, Toast.LENGTH_LONG).show());
                        return;
                    }

                    File tempDir = new File(Environment.getExternalStorageDirectory(), "00-Figurinhas/temp");
                    if (!tempDir.exists())
                        tempDir.mkdirs();

                    videoOriginalFile = new File(tempDir, "video_original." + extensaoArquivo);

                    FileOutputStream fos = new FileOutputStream(videoOriginalFile);
                    fos.write(body.bytes());
                    fos.close();

                    if (extensaoArquivo.toLowerCase().equals("mp4")) {
                        runOnUiThread(() -> {
                            stickerLoader.setVisibility(View.INVISIBLE);

                            btnTrim.setVisibility(View.VISIBLE);
                            btnCrop.setVisibility(View.VISIBLE);
                        });
                        return;
                    }

                    tempMp4File = new File(tempDir, "video_original.mp4");
                    if (tempMp4File.exists()) {
                        tempMp4File.delete();
                        tempMp4File = new File(tempDir, "video_original.mp4");
                    }

                    String ffmpegCommand = "-i " + videoOriginalFile.getAbsolutePath()
                            + " -vf fps=24 -r 24 -c:v libx264 -crf 23 -preset fast -pix_fmt yuv420p -an "
                            + tempMp4File.getAbsolutePath();

                    FFmpegKit.executeAsync(ffmpegCommand, session -> {
                        ReturnCode returnCode = session.getReturnCode();
                        if (ReturnCode.isSuccess(returnCode)) {
                            runOnUiThread(() -> {
                                stickerLoader.setVisibility(View.INVISIBLE);

                                btnTrim.setVisibility(View.VISIBLE);
                                btnCrop.setVisibility(View.VISIBLE);

                                Intent intent = new Intent(this, CropVideoActivity.class);
                                intent.putExtra("sticker_pack", stickerPack);
                                intent.putExtra("extensao_arquivo", extensaoArquivo);
                                //startActivity(intent);
                            });
                        } else {
                            runOnUiThread(() -> Toast.makeText(this, "Erro na conversão do vídeo.", Toast.LENGTH_LONG).show());
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, e.getMessage() + "\n" + Objects.requireNonNull(e.getCause()).getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void colar() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

        if (clipboard != null && clipboard.hasPrimaryClip()) {
            ClipData clipData = clipboard.getPrimaryClip();

            if (clipData != null && clipData.getItemCount() > 0) {
                CharSequence clipText = clipData.getItemAt(0).getText();

                if (clipText != null) {
                    etVideoUrl.setText(clipText.toString());
                } else {
                    Toast.makeText(this, "Nada de texto copiado!", Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            Toast.makeText(this, "Área de transferência vazia.", Toast.LENGTH_SHORT).show();
        }
    }

    private void limpar() {
        etVideoUrl.setText("");
    }

    private void limparTemp() {
        File tempDir = new File(Environment.getExternalStorageDirectory(), "00-Figurinhas/temp");
        ContentsJsonHelper.deleteRecursive(tempDir);
        if (!tempDir.exists())
            tempDir.mkdirs();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (ContentsJsonHelper.stickerAlteradoTelaCriar != null) {
            finish();
        }
    }
}