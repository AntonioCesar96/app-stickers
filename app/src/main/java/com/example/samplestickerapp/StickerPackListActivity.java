/*
 * Copyright (c) WhatsApp Inc. and its affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.example.samplestickerapp;

import static com.example.samplestickerapp.PickMediaHelper.REQUEST_PICK_MEDIA;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.util.Log;
import android.util.Pair;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;


public class StickerPackListActivity extends AddStickerPackActivity {
    private static final int MENU_TRIM = 1;
    private static final int MENU_CROP = 2;
    public static final String EXTRA_STICKER_PACK_LIST_DATA = "sticker_pack_list";
    private static final int STICKER_PREVIEW_DISPLAY_LIMIT = 5;
    private LinearLayoutManager packLayoutManager;
    private RecyclerView packRecyclerView;
    private StickerPackListAdapter allStickerPacksListAdapter;
    private ArrayList<StickerPack> stickerPackList;
    private View contextMenuAnchor;
    private File file;
    private StickerPack stickerPackSelecionado;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sticker_pack_list);
        packRecyclerView = findViewById(R.id.sticker_pack_list);
        stickerPackList = getIntent().getParcelableArrayListExtra(EXTRA_STICKER_PACK_LIST_DATA);
        showStickerPackList(stickerPackList);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Pacotes");
        }

        // 1) Cria âncora invisível e centraliza:
        FrameLayout root = findViewById(android.R.id.content);
        contextMenuAnchor = new View(this);
        contextMenuAnchor.setId(View.generateViewId());
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                0, 0, Gravity.CENTER
        );
        root.addView(contextMenuAnchor, lp);
        registerForContextMenu(contextMenuAnchor);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (ContentsJsonHelper.stickerPackAlterado != null) {
//            ContentsJsonHelper.atualizaContentsJsonAndContentProvider(this);
//            recarregarListagem();
//            ContentsJsonHelper.stickerPackAlterado = null;

            for (int i = 0; i < stickerPackList.size(); i++) {
                if (ContentsJsonHelper.stickerPackAlterado.identifier.equals(stickerPackList.get(i).identifier)) {

//                    if (!ContentsJsonHelper.stickersAlterados.isEmpty()) {
//                        for (int x = 0; x < ContentsJsonHelper.stickersAlterados.size(); x++) {
//                            ContentsJsonHelper.stickerPackAlterado.getStickers().add(0, ContentsJsonHelper.stickersAlterados.get(x));
//                        }
//
//                        ContentsJsonHelper.stickersAlterados = new ArrayList<>();
//                    }

                    stickerPackList.set(i, ContentsJsonHelper.stickerPackAlterado);
                    allStickerPacksListAdapter.notifyItemChanged(stickerPackList.indexOf(ContentsJsonHelper.stickerPackAlterado));
                    Toast.makeText(this, "Pacote " + ContentsJsonHelper.stickerPackAlterado.identifier + " atualizado", Toast.LENGTH_SHORT).show();
                    ContentsJsonHelper.stickerPackAlterado = null;
                    ContentsJsonHelper.stickersAlterados = new ArrayList<>();
                    break;
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu_listagem_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_meu_update) {

            ContentsJsonHelper.atualizaContentsJsonAndContentProvider(this);
            recarregarListagem();

            return true;
        }

        if (id == R.id.action_meu_listagem) {
            showCreateStickerPackDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void showCreateStickerPackDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Novo StickerPack");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 40, 50, 10);

        final EditText input = new EditText(this);
        input.setHint("Digite o nome");
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        layout.addView(input);

        final CheckBox checkBox = new CheckBox(this);
        checkBox.setText("É animado?");
        layout.addView(checkBox);

        builder.setView(layout);

        builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.setPositiveButton("Criar", null);

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(dlg -> {
            Button botaoCriar = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            botaoCriar.setTextColor(Color.RED);

            Button botaoCancelar = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            botaoCancelar.setTextColor(Color.GRAY);

            botaoCriar.setOnClickListener(v -> {
                String nome = input.getText().toString().trim();
                boolean animatedStickerPack = checkBox.isChecked();

                if (nome.isEmpty()) {
                    Toast.makeText(getApplicationContext(), "Nome não pode ser vazio", Toast.LENGTH_SHORT).show();
                    return;
                }

                //final String folderName = nome.replace(" ", "_").toLowerCase();
                boolean jaExiste = false;
                for (int i = 0; i < stickerPackList.size(); i++) {
                    if (nome.equals(stickerPackList.get(i).identifier)) {
                        jaExiste = true;
                        break;
                    }
                }

                if (jaExiste) {
                    Toast.makeText(getApplicationContext(), "Esse nome já foi usado", Toast.LENGTH_SHORT).show();
                    return;
                }

                try {
                    String pastaDefault = animatedStickerPack ? "Pacote 1" : "Pacote 2";

                    File stickerDir = new File(FilesHelper.getAssetsDir(), nome);
                    stickerDir.mkdirs();

                    String[] pacoteArquivos = getAssets().list(pastaDefault);

                    for (String asset : pacoteArquivos) {
                        String assetPath = pastaDefault + "/" + asset;
                        File outputPath = new File(stickerDir, asset);

                        try (InputStream in = getAssets().open(assetPath);
                             OutputStream out = new FileOutputStream(outputPath)) {
                            byte[] buffer = new byte[1024];
                            int read;
                            while ((read = in.read(buffer)) != -1) {
                                out.write(buffer, 0, read);
                            }
                            out.flush();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                ContentsJsonHelper.atualizaContentsJsonAndContentProvider(StickerPackListActivity.this);
                recarregarListagem();

                Toast.makeText(getApplicationContext(), "Criado: " + nome + " | Animado: " + animatedStickerPack, Toast.LENGTH_SHORT).show();
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    public void recarregarListagem() {
        new Thread(() -> {
            Pair<String, ArrayList<StickerPack>> result;

            try {
                ArrayList<StickerPack> stickerPackList;
                final Context context = StickerPackListActivity.this;

                if (context != null) {
                    stickerPackList = StickerPackLoader.fetchStickerPacks(context);
                    if (stickerPackList.size() == 0) {
                        result = new Pair<>("could not find any packs", null);
                    } else {
                        for (StickerPack stickerPack : stickerPackList) {
                            StickerPackValidator.verifyStickerPackValidity(context, stickerPack);
                        }
                        result = new Pair<>(null, stickerPackList);
                    }
                } else {
                    result = new Pair<>("could not fetch sticker packs", null);
                }

            } catch (Exception e) {
                Log.e("EntryActivity", "error fetching sticker packs", e);
                result = new Pair<>(e.getMessage() + "\n" + Objects.requireNonNull(e.getCause()).getMessage(), null);
            }

            Pair<String, ArrayList<StickerPack>> finalResult = result;
            runOnUiThread(() -> {
                if (finalResult.first != null) {
                    Toast.makeText(this, finalResult.first, Toast.LENGTH_LONG).show();
                } else {
                    boolean itemAdicionado = false;
                    for (StickerPack pack2 : finalResult.second) {
                        boolean exists = false;
                        for (StickerPack pack1 : stickerPackList) {
                            if (pack1.identifier.equals(pack2.identifier)) {
                                exists = true;
                                break;
                            }
                        }

                        if (!exists) {
                            stickerPackList.add(0, pack2);
                            itemAdicionado = true;
                        }
                    }

                    if (itemAdicionado) {
                        allStickerPacksListAdapter.setStickerPackList(finalResult.second);
                        allStickerPacksListAdapter.notifyItemInserted(0);
                        allStickerPacksListAdapter.notifyItemRangeChanged(0, stickerPackList.size());
                    } else {
                        stickerPackList = finalResult.second;
                        showStickerPackList(stickerPackList);
                    }
                }
            });

        }).start();
    }

    private void showStickerPackList(List<StickerPack> stickerPackList) {
        allStickerPacksListAdapter = new StickerPackListAdapter(stickerPackList, onAddButtonClickedListener);
        packRecyclerView.setAdapter(allStickerPacksListAdapter);
        packLayoutManager = new LinearLayoutManager(this);
        packLayoutManager.setOrientation(RecyclerView.VERTICAL);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(
                packRecyclerView.getContext(),
                packLayoutManager.getOrientation()
        );
        packRecyclerView.addItemDecoration(dividerItemDecoration);
        packRecyclerView.setLayoutManager(packLayoutManager);
        packRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(this::recalculateColumnCount);
    }

    private final StickerPackListAdapter.OnAddButtonClickedListener onAddButtonClickedListener = pack -> {
        if ((pack.getStickers().size() + 1) > 30) {
            Toast.makeText(StickerPackListActivity.this,
                    "Não é possível adicionar mais 1 figurinha nesse pacote, pois em um pacote é permitido " +
                            "30 figurinhas, se gerarmos mais 1 nesse pacote ele ficará com "
                            + (pack.getStickers().size() + 1) + " figurinhas", Toast.LENGTH_LONG).show();
            return;
        }

        this.stickerPackSelecionado = pack;
        PickMediaHelper.open(StickerPackListActivity.this);
    };

    private void recalculateColumnCount() {
        final int previewSize = getResources().getDimensionPixelSize(R.dimen.sticker_pack_list_item_preview_image_size);
        int firstVisibleItemPosition = packLayoutManager.findFirstVisibleItemPosition();
        StickerPackListItemViewHolder viewHolder = (StickerPackListItemViewHolder) packRecyclerView.findViewHolderForAdapterPosition(firstVisibleItemPosition);
        if (viewHolder != null) {
            final int widthOfImageRow = viewHolder.imageRowView.getMeasuredWidth();
            final int max = Math.max(widthOfImageRow / previewSize, 1);
            int maxNumberOfImagesInARow = Math.min(STICKER_PREVIEW_DISPLAY_LIMIT, max);
            int minMarginBetweenImages = (widthOfImageRow - maxNumberOfImagesInARow * previewSize) / (maxNumberOfImagesInARow - 1);
            allStickerPacksListAdapter.setImageRowSpec(maxNumberOfImagesInARow, minMarginBetweenImages);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        File file = PickMediaHelper.onActivityResult(this, requestCode, resultCode, data);
        if (file != null) {
            onItemClick(file);
        }
    }

    private void onItemClick(File file) {
        String name = file.getName().toLowerCase();

        // TODO: quando .webp avaliar se é estatico ou nao para saber qual tela abrir
        List<String> extensaoVideos = Arrays.asList(
                ".mp4", ".webm", ".mkv", ".avi", ".mov", ".flv", ".wmv", ".3gp", ".ts", ".gif", ".webp");
        if (extensaoVideos.stream().anyMatch(name::endsWith)) {
            this.file = file;
            openContextMenu(contextMenuAnchor);
            return;
        }

        List<String> extensaoImagens = Arrays.asList(
                ".jpg", ".jpeg", ".png", ".bmp", ".svg");
        if (extensaoImagens.stream().anyMatch(name::endsWith)) {
            this.file = file;
            Intent intent = new Intent(this, CropImageActivity.class);
            intent.putExtra("sticker_pack", stickerPackSelecionado);
            intent.putExtra("file_path", file.getAbsolutePath());
            startActivity(intent);
            return;
        }

        Toast.makeText(this, "Não é possível criar uma figurinha a partir desse arquivo", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.setHeaderTitle("Opções");
        menu.add(0, MENU_TRIM, 0, "Aparar(Trim)");
        menu.add(0, MENU_CROP, 1, "Cortar(Crop)");
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        FileExplorerHelper fileExplorerHelper = new FileExplorerHelper(this, stickerPackSelecionado);
        switch (item.getItemId()) {
            case MENU_TRIM:
                fileExplorerHelper.extracted(file, CustomVideoRangeActivity.class);
                return true;
            case MENU_CROP:
                fileExplorerHelper.extracted(file, CropVideoActivity.class);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }
}
