/*
 * Copyright (c) WhatsApp Inc. and its affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.example.samplestickerapp.list;

import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import com.example.samplestickerapp.R;
import com.facebook.drawee.view.SimpleDraweeView;

class MeuStickerPreviewViewHolder extends RecyclerView.ViewHolder {

    final SimpleDraweeView stickerPreviewView;

    MeuStickerPreviewViewHolder(final View itemView) {
        super(itemView);
        stickerPreviewView = itemView.findViewById(R.id.sticker_preview_meu);
    }
}