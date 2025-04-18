/*
 * Copyright (c) WhatsApp Inc. and its affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.example.samplestickerapp.list;

import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.samplestickerapp.R;

class MeuStickerPackListItemViewHolder extends RecyclerView.ViewHolder {

    final View container;
    final TextView titleView;
    final TextView publisherView;
    final TextView filesizeView;
    final ImageView addButton;
    final ImageView animatedStickerPackIndicator;
    final LinearLayout imageRowView;

    MeuStickerPackListItemViewHolder(final View itemView) {
        super(itemView);
        container = itemView;
        titleView = itemView.findViewById(R.id.sticker_pack_title_meu);
        publisherView = itemView.findViewById(R.id.sticker_pack_publisher_meu);
        filesizeView = itemView.findViewById(R.id.sticker_pack_filesize_meu);
        addButton = itemView.findViewById(R.id.add_button_on_list_meu);
        imageRowView = itemView.findViewById(R.id.sticker_packs_list_item_image_list_meu);
        animatedStickerPackIndicator = itemView.findViewById(R.id.sticker_pack_animation_indicator_meu);
    }
}