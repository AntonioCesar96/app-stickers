/*
 * Copyright (c) WhatsApp Inc. and its affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.example.samplestickerapp.list;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.format.Formatter;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.samplestickerapp.R;
import com.facebook.drawee.view.SimpleDraweeView;

import java.util.List;

public class MeuStickerPackListAdapter extends RecyclerView.Adapter<MeuStickerPackListItemViewHolder> {
    @NonNull
    private List<MeuStickerPackModel> stickerPacks;
    private int maxNumberOfStickersInARow;
    private int minMarginBetweenImages;

    MeuStickerPackListAdapter(@NonNull List<MeuStickerPackModel> stickerPacks) {
        this.stickerPacks = stickerPacks;
    }

    @NonNull
    @Override
    public MeuStickerPackListItemViewHolder onCreateViewHolder(@NonNull final ViewGroup viewGroup, final int i) {
        final Context context = viewGroup.getContext();
        final LayoutInflater layoutInflater = LayoutInflater.from(context);
        final View stickerPackRow = layoutInflater.inflate(R.layout.meu_sticker_packs_list_item, viewGroup, false);
        return new MeuStickerPackListItemViewHolder(stickerPackRow);
    }

    @Override
    public void onBindViewHolder(@NonNull final MeuStickerPackListItemViewHolder viewHolder, final int index) {
        MeuStickerPackModel pack = stickerPacks.get(index);
        final Context context = viewHolder.publisherView.getContext();
        viewHolder.publisherView.setText(pack.publisher);
        viewHolder.filesizeView.setText(Formatter.formatShortFileSize(context, pack.totalSize));

        viewHolder.titleView.setText(pack.name);
        viewHolder.container.setOnClickListener(view -> {
            Intent intent = new Intent(view.getContext(), MeuStickerPackDetailsActivity.class);
            intent.putExtra(MeuStickerPackDetailsActivity.EXTRA_SHOW_UP_BUTTON, true);
            intent.putExtra(MeuStickerPackDetailsActivity.EXTRA_STICKER_PACK_DATA, pack);
            view.getContext().startActivity(intent);
        });
        viewHolder.imageRowView.removeAllViews();
        //if this sticker pack contains less stickers than the max, then take the smaller size.
        int actualNumberOfStickersToShow = Math.min(maxNumberOfStickersInARow, pack.stickers.size());
        for (int i = 0; i < actualNumberOfStickersToShow; i++) {
            final SimpleDraweeView rowImage = (SimpleDraweeView) LayoutInflater.from(context).inflate(R.layout.meu_sticker_packs_list_image_item, viewHolder.imageRowView, false);
            rowImage.setImageURI(MeuContentsJsonHelper.getStickerAssetUri(pack.identifier, pack.stickers.get(i).imageFileName));
            final LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) rowImage.getLayoutParams();
            final int marginBetweenImages = minMarginBetweenImages - lp.leftMargin - lp.rightMargin;
            if (i != actualNumberOfStickersToShow - 1 && marginBetweenImages > 0) { //do not set the margin for the last image
                lp.setMargins(lp.leftMargin, lp.topMargin, lp.rightMargin + marginBetweenImages, lp.bottomMargin);
                rowImage.setLayoutParams(lp);
            }
            viewHolder.imageRowView.addView(rowImage);
        }
        setAddButtonAppearance(viewHolder.addButton, pack);
        viewHolder.animatedStickerPackIndicator.setVisibility(pack.animatedStickerPack ? View.VISIBLE : View.GONE);
    }

    private void setAddButtonAppearance(ImageView addButton, MeuStickerPackModel pack) {
//        if (pack.getIsWhitelisted()) {
            addButton.setImageResource(R.drawable.sticker_3rdparty_added);
            addButton.setClickable(false);
            addButton.setOnClickListener(null);
            setBackground(addButton, null);
//        } else {
//            addButton.setImageResource(R.drawable.sticker_3rdparty_add);
//            addButton.setOnClickListener(v -> onAddButtonClickedListener.onAddButtonClicked(pack));
//            TypedValue outValue = new TypedValue();
//            addButton.getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
//            addButton.setBackgroundResource(outValue.resourceId);
//        }
    }

    private void setBackground(View view, Drawable background) {
        if (Build.VERSION.SDK_INT >= 16) {
            view.setBackground(background);
        } else {
            view.setBackgroundDrawable(background);
        }
    }

    @Override
    public int getItemCount() {
        return stickerPacks.size();
    }

    void setImageRowSpec(int maxNumberOfStickersInARow, int minMarginBetweenImages) {
        this.minMarginBetweenImages = minMarginBetweenImages;
        if (this.maxNumberOfStickersInARow != maxNumberOfStickersInARow) {
            this.maxNumberOfStickersInARow = maxNumberOfStickersInARow;
            notifyDataSetChanged();
        }
    }

    void setStickerPackList(List<MeuStickerPackModel> stickerPackList) {
        this.stickerPacks = stickerPackList;
    }
}
