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
import android.os.Bundle;
import android.widget.Button;

import androidx.annotation.Nullable;

public class SplashActivity extends BaseActivity {

    private Button btnPermissao;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        btnPermissao = findViewById(R.id.btnPermissao);

        btnPermissao.setOnClickListener(view -> {
            PermissionHelper.requestManageExternalStoragePermission(this);
        });

        if (!PermissionHelper.hasManageExternalStoragePermission(this)) {
            PermissionHelper.requestManageExternalStoragePermission(this);
        } else {
            ContentsJsonHelper.atualizaContentsJsonAndContentProvider(this);

            // Pode acessar os arquivos com segurança
            final Intent intent = new Intent(this, EntryActivity.class);
            startActivity(intent);
            finish();
            overridePendingTransition(0, 0);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (PermissionHelper.hasManageExternalStoragePermission(this)) {
            ContentsJsonHelper.atualizaContentsJsonAndContentProvider(this);

            // Pode acessar os arquivos com segurança
            final Intent intent = new Intent(this, EntryActivity.class);
            startActivity(intent);
            finish();
            overridePendingTransition(0, 0);
        }
    }
}
