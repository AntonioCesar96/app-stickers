/*
 * Copyright (c) WhatsApp Inc. and its affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.example.samplestickerapp;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

public class SplashActivity extends BaseActivity {

    private Button btnPermissao;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        FilesHelper.setContext(getBaseContext());

        btnPermissao = findViewById(R.id.btnPermissao);

        btnPermissao.setOnClickListener(view -> {
            PermissionHelper.requestManageExternalStoragePermission(this);
        });

        if (!PermissionHelper.hasManageExternalStoragePermission(this)) {
            PermissionHelper.requestManageExternalStoragePermission(this);
        } else {
            criarPastasIniciais();
            ContentsJsonHelper.atualizaContentsJsonAndContentProvider(this);

            // Pode acessar os arquivos com segurança
            final Intent intent = new Intent(this, EntryActivity.class);
            startActivity(intent);
            finish();
            overridePendingTransition(0, 0);
        }
    }

    private void criarPastasIniciais() {
        try {
            ContentsJsonHelper.deleteRecursive(FilesHelper.getTempDir());

            File figurinhasDir = FilesHelper.getFigurinhaDir();
            if (!figurinhasDir.exists())
                figurinhasDir.mkdirs();

            File assetsDir = new File(figurinhasDir, "assets");
            if (!assetsDir.exists()) {
                assetsDir.mkdirs();
                copyAssetsRecursively("Pacote 2", getAssets());
                copyAssetsRecursively("Pacote 1", getAssets());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (PermissionHelper.hasManageExternalStoragePermission(this)) {
            criarPastasIniciais();
            ContentsJsonHelper.atualizaContentsJsonAndContentProvider(this);

            // Pode acessar os arquivos com segurança
            final Intent intent = new Intent(this, EntryActivity.class);
            startActivity(intent);
            finish();
            overridePendingTransition(0, 0);
        }
    }

    private void copyAssetsRecursively(String path, AssetManager assetManager) throws IOException {
        File assetsDir = FilesHelper.getAssetsDir();
        String[] pacoteArquivos = assetManager.list(path);
        File pacoteDir = new File(assetsDir, path);
        pacoteDir.mkdirs();

        for (String asset : pacoteArquivos) {
            copyAssetFile(assetManager, path + "/" + asset, new File(pacoteDir, asset));
        }
    }

    private void copyAssetFile(AssetManager assetManager, String assetPath, File outputPath) throws IOException {
        try (InputStream in = assetManager.open(assetPath);
             OutputStream out = new FileOutputStream(outputPath)) {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            out.flush();
        }
    }
}
