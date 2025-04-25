/*
 * Copyright (c) WhatsApp Inc. and its affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.example.samplestickerapp;

import android.view.View;
import android.widget.ImageView;

import androidx.recyclerview.widget.RecyclerView;

import com.facebook.drawee.view.SimpleDraweeView;

class StickerPreviewViewHolder extends RecyclerView.ViewHolder {

    final SimpleDraweeView stickerPreviewView;
    final View selectionOverlay;
    final ImageView selectionCheck;

    StickerPreviewViewHolder(final View itemView) {
        super(itemView);
        stickerPreviewView = itemView.findViewById(R.id.sticker_preview);
        selectionOverlay = itemView.findViewById(R.id.selectionOverlay);
        selectionCheck = itemView.findViewById(R.id.selectionCheck);
    }
}