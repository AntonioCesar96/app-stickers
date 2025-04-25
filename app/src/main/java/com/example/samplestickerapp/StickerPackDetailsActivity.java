/*
 * Copyright (c) WhatsApp Inc. and its affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.example.samplestickerapp;

import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.drawee.view.SimpleDraweeView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Locale;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class StickerPackDetailsActivity extends AddStickerPackActivity {

    /**
     * Do not change below values of below 3 lines as this is also used by WhatsApp
     */
    public static final String EXTRA_STICKER_PACK_ID = "sticker_pack_id";
    public static final String EXTRA_STICKER_PACK_AUTHORITY = "sticker_pack_authority";
    public static final String EXTRA_STICKER_PACK_NAME = "sticker_pack_name";

    public static final String EXTRA_STICKER_PACK_WEBSITE = "sticker_pack_website";
    public static final String EXTRA_STICKER_PACK_EMAIL = "sticker_pack_email";
    public static final String EXTRA_STICKER_PACK_PRIVACY_POLICY = "sticker_pack_privacy_policy";
    public static final String EXTRA_STICKER_PACK_LICENSE_AGREEMENT = "sticker_pack_license_agreement";
    public static final String EXTRA_STICKER_PACK_TRAY_ICON = "sticker_pack_tray_icon";
    public static final String EXTRA_SHOW_UP_BUTTON = "show_up_button";


    private RecyclerView recyclerView;
    private GridLayoutManager layoutManager;
    private StickerPreviewAdapter adapter;
    private int numColumns;
    private View addButton;
    private View alreadyAddedText;
    private StickerPack stickerPack;
    private View divider;
    private TextView packSizeTextView;
    private SimpleDraweeView expandedStickerView;
    private AlertDialog dialog;
    private MenuItem deleteMenuItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sticker_pack_details);
        boolean showUpButton = getIntent().getBooleanExtra(EXTRA_SHOW_UP_BUTTON, false);
        stickerPack = getIntent().getParcelableExtra("sticker_pack");
        TextView packNameTextView = findViewById(R.id.pack_name);
        TextView packPublisherTextView = findViewById(R.id.author);
        ImageView packTrayIcon = findViewById(R.id.tray_image);
        packSizeTextView = findViewById(R.id.pack_size);
        expandedStickerView = findViewById(R.id.sticker_details_expanded_sticker);

        addButton = findViewById(R.id.add_to_whatsapp_button);
        alreadyAddedText = findViewById(R.id.already_added_text);
        layoutManager = new GridLayoutManager(this, 1);
        recyclerView = findViewById(R.id.sticker_list);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.getViewTreeObserver().addOnGlobalLayoutListener(pageLayoutListener);
        recyclerView.addOnScrollListener(dividerScrollListener);
        divider = findViewById(R.id.divider);
        if (adapter == null) {
            adapter = new StickerPreviewAdapter(getLayoutInflater(), R.drawable.sticker_error, getResources().getDimensionPixelSize(R.dimen.sticker_pack_details_image_size), getResources().getDimensionPixelSize(R.dimen.sticker_pack_details_image_padding),
                    stickerPack, expandedStickerView, getOnUpdateSizeListener(), getOnSelectionListener());
            recyclerView.setAdapter(adapter);
        }
        packNameTextView.setText(stickerPack.name);
        packPublisherTextView.setText(stickerPack.getStickers().size() + " figurinhas");
        packTrayIcon.setImageURI(StickerPackLoader.getStickerAssetUri(stickerPack.identifier, stickerPack.trayImageFile));
        packSizeTextView.setText(String.format(new Locale("pt", "BR"), "%.2f", (double) stickerPack.getTotalSize() / 1024.0) + " KB");

        addButton.setOnClickListener(v -> addStickerPackToWhatsApp(stickerPack.identifier, stickerPack.name));

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(showUpButton);
            getSupportActionBar().setTitle("Detalhes");
        }
        findViewById(R.id.sticker_pack_animation_indicator).setVisibility(stickerPack.animatedStickerPack ? View.VISIBLE : View.GONE);

        limparTemp();
    }

    private StickerPreviewAdapter.OnUpdateSizeListener getOnUpdateSizeListener() {
        return stickerPack1 -> packSizeTextView.setText(String.format(new Locale("pt", "BR"), "%.2f", (double) stickerPack.getTotalSize() / 1024.0) + " KB");
    }

    private StickerPreviewAdapter.OnSelectionListener getOnSelectionListener() {
        return new StickerPreviewAdapter.OnSelectionListener() {
            @Override
            public void onSelectionModeChanged(boolean active) {
                if (deleteMenuItem != null) {
                    deleteMenuItem.setVisible(active);
                }
            }

            @Override
            public void onSelectionCountChanged(int count) {
                if (deleteMenuItem != null) {
                    if (count == 0) {
                        getSupportActionBar().setTitle("Detalhes");
                    } else {
                        getSupportActionBar().setTitle(String.format("Excluir (%d)", count));
                        deleteMenuItem.setTitle(String.format("Excluir (%d)", count));
                    }
                }
            }
        };
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

        if (!ContentsJsonHelper.stickersAlterados.isEmpty()) {
            if (dialog != null && dialog.isShowing())
                dialog.dismiss();

            for (int i = 0; i < ContentsJsonHelper.stickersAlterados.size(); i++) {
                adapter.stickerPack.getStickers().add(0, ContentsJsonHelper.stickersAlterados.get(i));
                adapter.notifyItemInserted(0);
                adapter.notifyItemRangeChanged(0, adapter.stickerPack.getStickers().size());
            }

            ContentsJsonHelper.stickerPackAlterado = adapter.stickerPack;
            ContentsJsonHelper.stickersAlterados = new ArrayList<>();
        }
    }

    private void launchInfoActivity(String publisherWebsite, String publisherEmail, String privacyPolicyWebsite, String licenseAgreementWebsite, String trayIconUriString) {
        Intent intent = new Intent(StickerPackDetailsActivity.this, StickerPackInfoActivity.class);
        intent.putExtra(StickerPackDetailsActivity.EXTRA_STICKER_PACK_ID, stickerPack.identifier);
        intent.putExtra(StickerPackDetailsActivity.EXTRA_STICKER_PACK_WEBSITE, publisherWebsite);
        intent.putExtra(StickerPackDetailsActivity.EXTRA_STICKER_PACK_EMAIL, publisherEmail);
        intent.putExtra(StickerPackDetailsActivity.EXTRA_STICKER_PACK_PRIVACY_POLICY, privacyPolicyWebsite);
        intent.putExtra(StickerPackDetailsActivity.EXTRA_STICKER_PACK_LICENSE_AGREEMENT, licenseAgreementWebsite);
        intent.putExtra(StickerPackDetailsActivity.EXTRA_STICKER_PACK_TRAY_ICON, trayIconUriString);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar, menu);
        // inicialmente oculto
        deleteMenuItem = menu.findItem(R.id.action_delete);
        deleteMenuItem.setVisible(false);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.action_delete && stickerPack != null) {
            adapter.handleDelete(this);
            return true;
        }
        if (item.getItemId() == R.id.action_adicionar_novo && stickerPack != null) {
            if ((stickerPack.getStickers().size() + 1) > 30) {
                Toast.makeText(StickerPackDetailsActivity.this,
                        "Não é possível adicionar mais 1 figurinha nesse pacote, pois em um pacote é permitido " +
                                "30 figurinhas, se gerarmos mais 1 nesse pacote ele ficará com "
                                + (stickerPack.getStickers().size() + 1) + " figurinhas", Toast.LENGTH_LONG).show();
                return true;
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Digite algo");

            View dialogView = getLayoutInflater().inflate(R.layout.dialog_input, null);
            final EditText input = dialogView.findViewById(R.id.input_url);
            input.setText("https://media.tenor.com/QA_IqSKoWTcAAAPo/the-rock.mp4");

            builder.setView(dialogView);

            builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });

            dialog = builder.create();
            dialog.show();

            Button negativeButton = dialog.getButton(DialogInterface.BUTTON_NEGATIVE);
            negativeButton.setTextColor(Color.RED);

            ImageButton btnLimpar = dialogView.findViewById(R.id.btn_limpar);
            ImageButton btnColar = dialogView.findViewById(R.id.btn_colar);
            ImageButton btnBaixar = dialogView.findViewById(R.id.btn_baixar);

            btnLimpar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    input.setText("");  // Limpa o conteúdo do EditText
                }
            });

            btnColar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = clipboard.getPrimaryClip();
                    if (clip != null && clip.getItemCount() > 0) {
                        CharSequence text = clip.getItemAt(0).getText();
                        input.setText(text);
                    } else {
                        Toast.makeText(StickerPackDetailsActivity.this, "Nada copiado para colar", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            btnBaixar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String url = input.getText().toString();

                    if (url.isEmpty() || !Patterns.WEB_URL.matcher(url).matches()) {
                        Toast.makeText(StickerPackDetailsActivity.this, "URL inválida", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    startDownload(url);
                }
            });

            return true;
        }

        if (item.getItemId() == R.id.action_listar_diretorio && stickerPack != null) {
            if ((stickerPack.getStickers().size() + 1) > 30) {
                Toast.makeText(StickerPackDetailsActivity.this,
                        "Não é possível adicionar mais 1 figurinha nesse pacote, pois em um pacote é permitido " +
                                "30 figurinhas, se gerarmos mais 1 nesse pacote ele ficará com "
                                + (stickerPack.getStickers().size() + 1) + " figurinhas", Toast.LENGTH_LONG).show();
                return true;
            }

            Intent intent = new Intent(this, FileExplorerActivity.class);
            intent.putExtra("sticker_pack", stickerPack);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void startDownload(String url) {
        // Criar e configurar o ProgressDialog
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Download em progresso");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setMax(100);
        progressDialog.setCancelable(false);
        progressDialog.show();

        new Thread(() -> {
            try {
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder().url(url).build();
                Response response = client.newCall(request).execute();
                ResponseBody body = response.body();

                if (body != null) {
                    long contentLength = body.contentLength();
                    String extensaoArquivo = body.contentType().subtype();

                    if (extensaoArquivo.equals("plain")) {
                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            Toast.makeText(StickerPackDetailsActivity.this,
                                    "Há algo errado com a URL\nFormato baixado: " + extensaoArquivo,
                                    Toast.LENGTH_LONG).show();
                        });
                        return;
                    }

                    // Tenta extrair o nome do arquivo do header Content-Disposition
                    String fileName = null;
                    String disposition = response.header("Content-Disposition");
                    if (disposition != null && disposition.contains("filename=")) {
                        fileName = disposition.split("filename=")[1].replace("\"", "").trim();
                    } else {
                        // fallback para URL path
                        Uri uri = Uri.parse(url);
                        fileName = uri.getLastPathSegment();
                    }

                    // fallback final se tudo falhar
                    if (fileName == null || fileName.isEmpty()) {
                        fileName = "f_" + System.currentTimeMillis() + "." + extensaoArquivo;
                    }

                    File downloadDir = new File(Environment.getExternalStorageDirectory(), "Download");
                    File videoOriginalFile = new File(downloadDir, fileName);

                    InputStream is = body.byteStream();
                    FileOutputStream fos = new FileOutputStream(videoOriginalFile);
                    byte[] buffer = new byte[8192];
                    long totalRead = 0;
                    int read;
                    while ((read = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, read);
                        totalRead += read;
                        int progress = (int) ((totalRead * 100) / contentLength);
                        runOnUiThread(() -> progressDialog.setProgress(progress));
                    }

                    fos.flush();
                    fos.close();
                    is.close();

                    runOnUiThread(() -> {
                        progressDialog.dismiss();

                        Intent intent = new Intent(StickerPackDetailsActivity.this, FileExplorerActivity.class);
                        intent.putExtra("sticker_pack", stickerPack);
                        intent.putExtra("abrir_download", true);
                        startActivity(intent);
                    });
                } else {
                    progressDialog.dismiss();
                    Toast.makeText(StickerPackDetailsActivity.this,
                            "Aconteceu algum problema",
                            Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(StickerPackDetailsActivity.this,
                            "Erro: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }


    private final ViewTreeObserver.OnGlobalLayoutListener pageLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            setNumColumns(recyclerView.getWidth() / recyclerView.getContext().getResources().getDimensionPixelSize(R.dimen.sticker_pack_details_image_size));
        }
    };

    private void setNumColumns(int numColumns) {
        if (this.numColumns != numColumns) {
            layoutManager.setSpanCount(numColumns);
            this.numColumns = numColumns;
            if (adapter != null) {
                adapter.notifyDataSetChanged();
            }
        }
    }

    private final RecyclerView.OnScrollListener dividerScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(@NonNull final RecyclerView recyclerView, final int newState) {
            super.onScrollStateChanged(recyclerView, newState);
            updateDivider(recyclerView);
        }

        @Override
        public void onScrolled(@NonNull final RecyclerView recyclerView, final int dx, final int dy) {
            super.onScrolled(recyclerView, dx, dy);
            updateDivider(recyclerView);
        }

        private void updateDivider(RecyclerView recyclerView) {
            boolean showDivider = recyclerView.computeVerticalScrollOffset() > 0;
            if (divider != null) {
                divider.setVisibility(showDivider ? View.VISIBLE : View.INVISIBLE);
            }
        }
    };
}
