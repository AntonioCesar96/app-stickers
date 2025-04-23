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
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.text.format.Formatter;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;

import java.util.List;
import java.util.Locale;

public class StickerPackListAdapter extends RecyclerView.Adapter<StickerPackListItemViewHolder> {
    @NonNull
    private List<StickerPack> stickerPacks;
    @NonNull
    private final OnAddButtonClickedListener onAddButtonClickedListener;
    private int maxNumberOfStickersInARow;
    private int minMarginBetweenImages;

    StickerPackListAdapter(@NonNull List<StickerPack> stickerPacks, @NonNull OnAddButtonClickedListener onAddButtonClickedListener) {
        this.stickerPacks = stickerPacks;
        this.onAddButtonClickedListener = onAddButtonClickedListener;
    }

    @NonNull
    @Override
    public StickerPackListItemViewHolder onCreateViewHolder(@NonNull final ViewGroup viewGroup, final int i) {
        final Context context = viewGroup.getContext();
        final LayoutInflater layoutInflater = LayoutInflater.from(context);
        final View stickerPackRow = layoutInflater.inflate(R.layout.sticker_packs_list_item, viewGroup, false);
        return new StickerPackListItemViewHolder(stickerPackRow);
    }

    @Override
    public void onBindViewHolder(@NonNull final StickerPackListItemViewHolder viewHolder, final int index) {
        StickerPack pack = stickerPacks.get(index);
        final Context context = viewHolder.publisherView.getContext();

        viewHolder.container.setOnClickListener(view -> {
            Intent intent = new Intent(view.getContext(), StickerPackDetailsActivity.class);
            intent.putExtra(StickerPackDetailsActivity.EXTRA_SHOW_UP_BUTTON, true);
            intent.putExtra("sticker_pack", pack);
            view.getContext().startActivity(intent);
        });
        viewHolder.imageRowView.removeAllViews();
        //if this sticker pack contains less stickers than the max, then take the smaller size.
        int actualNumberOfStickersToShow = Math.min(maxNumberOfStickersInARow, pack.getStickers().size());
        for (int i = 0; i < actualNumberOfStickersToShow; i++) {

            final SimpleDraweeView rowImage = (SimpleDraweeView) LayoutInflater.from(context).inflate(R.layout.sticker_packs_list_image_item, viewHolder.imageRowView, false);
            //if (!pack.animatedStickerPack) {
                rowImage.setImageURI(StickerPackLoader.getStickerAssetUri(pack.identifier, pack.getStickers().get(i).imageFileName));
//            } else {
//                final Uri stickerAssetUri = StickerPackLoader.getStickerAssetUri(pack.identifier, pack.getStickers().get(i).imageFileName)
//                        .buildUpon()
//                        .appendQueryParameter("t", String.valueOf(System.currentTimeMillis()))
//                        .build();
//                DraweeController controller = Fresco.newDraweeControllerBuilder()
//                        .setUri(stickerAssetUri)
//                        .setAutoPlayAnimations(true)
//                        .build();
//
//                rowImage.setImageResource(R.drawable.sticker_error);
//                rowImage.setController(controller);
//            }

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

        viewHolder.publisherView.setText(pack.publisher);
        viewHolder.filesizeView.setText(String.format(new Locale("pt", "BR"), "%.2f", (double) pack.getTotalSize() / 1024.0) + " KB");
        viewHolder.titleView.setText(pack.name);

        viewHolder.container.setOnLongClickListener(v -> {
            handleDelete(v.getContext(), index);
            return true;
        });
    }

    private void handleDelete(Context context, int position) {
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("Confirmar exclusão")
                .setMessage("Tem certeza que deseja excluir esta figurinha?")
                .setPositiveButton("Excluir", (d, which) -> {

                    AlertDialog dialog2 = new AlertDialog.Builder(context)
                            .setTitle("Tem certeza mesmo?")
                            .setMessage("Tem realmente certeza que deseja excluir esta figurinha?")
                            .setPositiveButton("Excluir", (d2, which2) -> {
                                StickerPack removido = stickerPacks.remove(position);

                                ContentsJsonHelper.removerPacote(removido.identifier, context);

                                notifyItemRemoved(position);
                                notifyItemRangeChanged(position, getItemCount());
                            })
                            .setNegativeButton("Cancelar", null)
                            .show();

                    dialog2.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.RED);
                    dialog2.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.GRAY);

                })
                .setNegativeButton("Cancelar", null)
                .show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.RED);
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.GRAY);
    }

    private void setAddButtonAppearance(ImageView addButton, StickerPack pack) {
        addButton.setImageResource(R.drawable.sticker_3rdparty_add);
        addButton.setOnClickListener(v -> onAddButtonClickedListener.onAddButtonClicked(pack));
        TypedValue outValue = new TypedValue();
        addButton.getContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
        addButton.setBackgroundResource(outValue.resourceId);
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

    void setStickerPackList(List<StickerPack> stickerPackList) {
        this.stickerPacks = stickerPackList;
    }

    public interface OnAddButtonClickedListener {
        void onAddButtonClicked(StickerPack stickerPack);
    }
}
