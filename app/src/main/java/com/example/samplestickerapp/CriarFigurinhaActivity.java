/*
 * Copyright (c) WhatsApp Inc. and its affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.example.samplestickerapp;

import static com.example.samplestickerapp.StickerPackValidator.ANIMATED_STICKER_FILE_LIMIT_KB;
import static com.example.samplestickerapp.StickerPackValidator.ANIMATED_STICKER_FRAME_DURATION_MIN;
import static com.example.samplestickerapp.StickerPackValidator.ANIMATED_STICKER_TOTAL_DURATION_MAX;
import static com.example.samplestickerapp.StickerPackValidator.IMAGE_HEIGHT;
import static com.example.samplestickerapp.StickerPackValidator.IMAGE_WIDTH;
import static com.example.samplestickerapp.StickerPackValidator.KB_IN_BYTES;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Movie;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.ReturnCode;
import com.arthenica.ffmpegkit.SessionState;
import com.facebook.animated.webp.WebPImage;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.common.ImageDecodeOptions;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
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
    private TextView tvInfo;
    private Button btnBaixar, btnComandos;
    private Button btnEnviarWhats;
    private SimpleDraweeView expandedStickerPreview;
    private ProgressBar stickerLoader;
    private VideoView vvOriginal;
    //private PlayerView playerView;
    private File videoOriginalFile;
    private File tempWebpFile;

    //private String urlInicial = "https://external.fcgr3-1.fna.fbcdn.net/emg1/v/t13/8567978231481450529?stp=dst-src&url=https%3A%2F%2Fmedia2.giphy.com%2Fmedia%2Fv1.Y2lkPTA1NzQyMTNjNGE0YWdibDJtbW0xd2tob3NybzdpZHlpczU2a3NjZjFmZXV6bGpuNyZlcD12MV9naWZzJmN0PWc%2F3xz2BLBOt13X9AgjEA%2F200.gif&utld=giphy.com&_nc_gid=EuCz5oeQvE21QdSKISiwCA&_nc_oc=AdkjMkY0P_2Wj889eNd92izUKXiO9aoEOHGSUCIQc2JA-qyfS-DOd0QuUYuntygS9ShJKOt5sWEFBP6eDgCP55Ge&ccb=13-1&oh=06_Q39-yEcZCMRP-60jvOheo1UVIGiPo8vjIDvCuBCk6Wn4_L4&oe=67FD9B00&_nc_sid=1d65fc"
    //private String urlInicial = "https://media.tenor.com/QA_IqSKoWTcAAAPs/the-rock.webm"
    private String urlInicial = "https://media.tenor.com/QA_IqSKoWTcAAAPo/the-rock.mp4";
    private String ffmpegCommandGerado = " -vf \"setpts=1.0*PTS,fps=24,scale=512:-1\" -ss 00:00:00 -loop 0 -crf 4 -preset default -pix_fmt yuva420p -an ";

    //private ExoPlayer exoPlayer;
    private long playbackPosition = 0L;
    private boolean playWhenReady = true;

    CheckBox checkboxEstatico;
    EditText inputVelocidade, inputVelocidadeFps, inputScale, inputStartTime, inputDuration, inputQuality;
    String velocidade = "1", velocidadeFps = "24";
    String scale = "512:-1", startTime = "00:00:00", duration = "0", quality = "4";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_criar_figurinha);

        stickerPack = getIntent().getParcelableExtra(EXTRA_STICKER_PACK_DATA);

        etVideoUrl = findViewById(R.id.etVideoUrl);
        tvErro = findViewById(R.id.tvErro);
        tvInfo = findViewById(R.id.tvInfo);
        btnBaixar = findViewById(R.id.btnBaixar);
        btnComandos = findViewById(R.id.btnComandos);
        btnEnviarWhats = findViewById(R.id.btnEnviarWhats);
        expandedStickerPreview = findViewById(R.id.sticker_details_expanded_sticker);
        stickerLoader = findViewById(R.id.sticker_loader);
        vvOriginal = findViewById(R.id.vvOriginal);

        btnBaixar.setOnClickListener(view -> downloadVideo());
        btnEnviarWhats.setOnClickListener(view -> enviarWhatsapp());
        btnComandos.setOnClickListener(view -> openDialogOpcoesVideo());

        etVideoUrl.setText(urlInicial);

        tempWebpFile = new File(Environment.getExternalStorageDirectory(), "00-Figurinhas/temp/cropped_output.webp");
        if(!tempWebpFile.exists()) {
            tempWebpFile = new File(Environment.getExternalStorageDirectory(),"00-Figurinhas/temp/video_original.webp");
        }

        if(tempWebpFile.exists()) {
            try {
                byte[] bytes2 = getBytes(tempWebpFile);
                WebPImage webPImage2 = WebPImage.createFromByteArray(bytes2, ImageDecodeOptions.defaults());
                extracted(webPImage2);
            }catch (Exception e) {
                if(tempWebpFile.exists()) {
                    tempWebpFile.delete();
                }
                e.printStackTrace();
                Toast.makeText(this, e.getMessage() + "\n" + Objects.requireNonNull(e.getCause()).getMessage(), Toast.LENGTH_SHORT).show();
            }
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Pastas e Figurinhas");
        }
    }

    private void openDialogOpcoesVideo() {
        LayoutInflater inflater = LayoutInflater.from(this); // ou getLayoutInflater() se estiver em uma Activity
        View dialogView = inflater.inflate(R.layout.dialog_custom, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Configurações de Exportação");
        builder.setView(dialogView);

        checkboxEstatico = dialogView.findViewById(R.id.checkbox_estatico);
        inputVelocidade = dialogView.findViewById(R.id.input_velocidade);
        inputVelocidadeFps = dialogView.findViewById(R.id.input_velocidade_fps);
        inputScale = dialogView.findViewById(R.id.input_scale);
        inputStartTime = dialogView.findViewById(R.id.input_start_time);
        inputDuration = dialogView.findViewById(R.id.input_duration);
        inputQuality = dialogView.findViewById(R.id.input_quality);

        resetInputs();

        checkboxEstatico.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    velocidade = "1";
                    velocidadeFps = "1";
                    scale = "512:-1";
                    startTime = "00:00:00";
                    duration = "0";
                    quality = "4";
                } else {
                    velocidade = "1";
                    velocidadeFps = "24";
                    scale = "512:-1";
                    startTime = "00:00:00";
                    duration = "0";
                    quality = "4";
                }

                resetInputs();
            }
        });

        builder.setPositiveButton("OK", (dialog, which) -> {
            generateFFmpegCommand();
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
    }

    public void generateFFmpegCommand() {
        velocidade = inputVelocidade.getText().toString().trim();
        velocidadeFps = inputVelocidadeFps.getText().toString().trim();
        scale = inputScale.getText().toString().trim();
        startTime = inputStartTime.getText().toString().trim();
        duration = inputDuration.getText().toString().trim();
        quality = inputQuality.getText().toString().trim();
        //fps = inputFps.getText().toString().trim();

        if (velocidade.isEmpty() || velocidadeFps.isEmpty() || scale.isEmpty()
                || startTime.isEmpty() || duration.isEmpty() || quality.isEmpty()) {

            Toast.makeText(this, "Preencha todos os campos antes de gerar o comando.", Toast.LENGTH_SHORT).show();
            return;
        }

        int velocidadeFpsValue = velocidadeFps.equals("0") ? 24 : (Math.min(Integer.parseInt(velocidadeFps), 45));
        double adjustedValue = velocidade.equals("0") ? 1 : 1 / Double.parseDouble(velocidade);

        ffmpegCommandGerado = "-vf \"setpts=" + adjustedValue + "*PTS,fps=" + velocidadeFpsValue + ",scale=" + scale + "\"";

        ffmpegCommandGerado += " -ss " + startTime;
        if (!duration.equals("0"))
            ffmpegCommandGerado += " -t " + duration;
        ffmpegCommandGerado += " -loop 0";
        ffmpegCommandGerado += " -crf " + quality;
        //ffmpegCommand += " -r " + fps;

        ffmpegCommandGerado += " -preset default";
        ffmpegCommandGerado += " -pix_fmt yuva420p";

        ffmpegCommandGerado += " -an";
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

                    File tempDir = new File(Environment.getExternalStorageDirectory(), "00-Figurinhas/temp");
                    if (!tempDir.exists())
                        tempDir.mkdirs();

                    videoOriginalFile = new File(tempDir, "video_original." + extensaoArquivo);

                    FileOutputStream fos = new FileOutputStream(videoOriginalFile);
                    fos.write(body.bytes());
                    fos.close();

                    tempWebpFile = new File(tempDir, "video_original.webp");
                    if (tempWebpFile.exists()) {
                        tempWebpFile.delete();
                        tempWebpFile = new File(tempDir, "video_original.webp");
                    }
                    // cria partir de um sequência de imagens: ffmpeg -framerate 10 -i frame_%03d.png -loop 0 output.webp
                    String ffmpegCommand = "-i " + videoOriginalFile.getAbsolutePath() + " " + ffmpegCommandGerado + " " + tempWebpFile.getAbsolutePath();

                    FFmpegKit.executeAsync(ffmpegCommand, session -> {
                        ReturnCode returnCode = session.getReturnCode();
                        if (ReturnCode.isSuccess(returnCode)) {
                            runOnUiThread(() -> {
                                byte[] bytes = getBytes(tempWebpFile);
                                final WebPImage webPImage = WebPImage.createFromByteArray(bytes, ImageDecodeOptions.defaults());

                                if (webPImage.getFrameCount() == 1) {
                                    try {
                                        File outputFrame1 = new File(Environment.getExternalStorageDirectory(), "00-Figurinhas/temp/frame1.webp");
                                        File outputFrame2 = new File(Environment.getExternalStorageDirectory(), "00-Figurinhas/temp/frame2.webp");

                                        FileOutputStream fos1 = new FileOutputStream(outputFrame1);
                                        fos1.write(bytes);
                                        fos1.close();

                                        byte[] frame2Bytes = bytes.clone();
                                        // altera um byte do conteúdo (ex: último byte XOR) isso pro ffmeg entender como um frame diferente, checksum muda
                                        frame2Bytes[frame2Bytes.length - 10] ^= 0x01;

                                        FileOutputStream fos2 = new FileOutputStream(outputFrame2);
                                        fos2.write(frame2Bytes);
                                        fos2.close();

                                        if (tempWebpFile.exists()) {
                                            tempWebpFile.delete();
                                            tempWebpFile = new File(tempDir, "video_original.webp");
                                        }

                                        File inputTemp = new File(Environment.getExternalStorageDirectory(), "00-Figurinhas/temp");
                                        String ffmpegCommand2 = "-framerate 5 -i " + inputTemp.getAbsolutePath() + "/frame%d.webp -loop 0 -c:v libwebp_anim " + tempWebpFile.getAbsolutePath();

                                        FFmpegKit.executeAsync(ffmpegCommand2, session2 -> {
                                            ReturnCode returnCode2 = session2.getReturnCode();
                                            if (ReturnCode.isSuccess(returnCode2)) {
                                                runOnUiThread(() -> {
                                                    byte[] bytes2 = getBytes(tempWebpFile);
                                                    WebPImage webPImage2 = WebPImage.createFromByteArray(bytes2, ImageDecodeOptions.defaults());
                                                    extracted(webPImage2);

                                                });
                                            } else {
                                                runOnUiThread(() -> Toast.makeText(this, "Erro na conversão do vídeo.", Toast.LENGTH_LONG).show());
                                            }
                                        });

                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        Toast.makeText(this, e.getMessage() + "\n" + Objects.requireNonNull(e.getCause()).getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                    return;
                                }

                                extracted(webPImage);
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

    private void extracted(WebPImage webPImage) {
        String infos = "Altura: " + webPImage.getHeight();
        infos += "\nLargura: " + webPImage.getWidth();
        infos += "\nQtd Frames: " + webPImage.getFrameCount();
        infos += "\nDuração: " + webPImage.getDuration() / 1000 + " segundos";
        infos += "\nTamanho: " + String.format(new Locale("pt", "BR"), "%.2f", webPImage.getSizeInBytes() / 1024.0) + " KB";

        tvInfo.setText(infos);

        final Uri stickerAssetUri = Uri.fromFile(tempWebpFile)
                .buildUpon()
                .appendQueryParameter("t", String.valueOf(System.currentTimeMillis()))
                .build();
        DraweeController controller = Fresco.newDraweeControllerBuilder()
                .setUri(stickerAssetUri)
                .setAutoPlayAnimations(true)
                .build();

        expandedStickerPreview.setImageResource(R.drawable.sticker_error);
        expandedStickerPreview.setController(controller);

        expandedStickerPreview.setVisibility(View.VISIBLE);
        stickerLoader.setVisibility(View.INVISIBLE);

        expandedStickerPreview.setOnClickListener(view -> {

            Intent intent = new Intent(view.getContext(), CropVideoActivity.class);
            intent.putExtra("sticker_pack", stickerPack);
            startActivity(intent);

//            stickerLoader.setVisibility(View.VISIBLE);
//
//            File outputDir = new File(Environment.getExternalStorageDirectory(), "00-Figurinhas/temp/webp_frames");
//            if (!outputDir.exists()) outputDir.mkdirs();
//
//            String outputPattern = new File(outputDir, "frame_%03d.png").getAbsolutePath();
//
//            String extractCmd = "-i \"" + videoOriginalFile.getAbsolutePath() + "\" \"" + outputPattern + "\"";
//
//            FFmpegKit.executeAsync(extractCmd, session1 -> {
//                ReturnCode returnCode1 = session1.getReturnCode();
//
//                if (ReturnCode.isSuccess(returnCode1)) {
//                    File[] frameFiles = outputDir.listFiles((dir, name) -> name.endsWith(".png"));
//                    if (frameFiles != null && frameFiles.length > 0) {
//                        List<Bitmap> bitmaps = new ArrayList<>();
//                        for (File frameFile : frameFiles) {
//                            Bitmap bmp = BitmapFactory.decodeFile(frameFile.getAbsolutePath());
//                            bitmaps.add(bmp);
//                        }
//
//                        runOnUiThread(() -> {
//                            mostrarDialogComFrames(this, bitmaps, frameFiles);
//                        });
//                    }
//                } else {
//                    Log.e("FFmpegKit", "Erro ao extrair frames: " + session1.getFailStackTrace());
//                    runOnUiThread(() -> {
//                        Toast.makeText(this, "Erro ao extrair frames", Toast.LENGTH_SHORT).show();
//                    });
//                }
//            });
        });
    }

    public void mostrarDialogComFrames(Context context, List<Bitmap> frames, File[] frameFiles) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_frames, null);
        RecyclerView recyclerView = dialogView.findViewById(R.id.framesRecyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(context, 1));
        recyclerView.setAdapter(new FrameAdapter(context, frames, frameFiles));
        builder.setView(dialogView);
        builder.setTitle("Frames extraídos");
        builder.setPositiveButton("Fechar", null);

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.RED);

        stickerLoader.setVisibility(View.INVISIBLE);
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
        if (tempWebpFile == null) {
            Toast.makeText(this, "Arquivo não existe", Toast.LENGTH_SHORT).show();
            return;
        }

        byte[] bytes = getBytes(tempWebpFile);
        final WebPImage webPImage = WebPImage.createFromByteArray(bytes, ImageDecodeOptions.defaults());
        if (bytes.length > ANIMATED_STICKER_FILE_LIMIT_KB * KB_IN_BYTES) {
            Toast.makeText(this, "figurinhas animadas tem que ter tamanho menor q " + ANIMATED_STICKER_FILE_LIMIT_KB + "KB, tamanho atual " + String.format(new Locale("pt", "BR"), "%.2f", bytes.length / KB_IN_BYTES) + " KB", Toast.LENGTH_SHORT).show();
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
            FileInputStream fis = new FileInputStream(tempWebpFile);
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