/*
 * Copyright (c) WhatsApp Inc. and its affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.example.samplestickerapp;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StickerPreviewAdapter extends RecyclerView.Adapter<StickerPreviewViewHolder> {

    private static final float COLLAPSED_STICKER_PREVIEW_BACKGROUND_ALPHA = 1f;
    private static final float EXPANDED_STICKER_PREVIEW_BACKGROUND_ALPHA = 0.2f;

    @NonNull
    public StickerPack stickerPack;

    private final int cellSize;
    private final int cellLimit;
    private final int cellPadding;
    private final int errorResource;
    private final SimpleDraweeView expandedStickerPreview;
    private final View expandedPreviewContainer;
    private java.util.function.Consumer<Sticker> onAddSelected;

    private final LayoutInflater layoutInflater;
    private RecyclerView recyclerView;
    private View clickedStickerPreview;
    float expandedViewLeftX;
    float expandedViewTopY;
    OnUpdateSizeListener onUpdateSizeListener;

    // Estado de seleção
    private OnSelectionListener onSelectionListener;
    private boolean selectionMode = false;
    private final Set<Integer> selectedPositions = new HashSet<>();

    public interface OnSelectionListener {
        void onSelectionModeChanged(boolean active);

        void onSelectionCountChanged(int count);
    }

    StickerPreviewAdapter(
            @NonNull final LayoutInflater layoutInflater,
            final int errorResource,
            final int cellSize,
            final int cellPadding,
            @NonNull final StickerPack stickerPack,
            final SimpleDraweeView expandedStickerView,
            OnUpdateSizeListener onUpdateSizeListener,
            OnSelectionListener onSelectionListener) {
        this.cellSize = cellSize;
        this.cellPadding = cellPadding;
        this.cellLimit = 0;
        this.layoutInflater = layoutInflater;
        this.errorResource = errorResource;
        this.stickerPack = stickerPack;
        this.expandedStickerPreview = expandedStickerView;
        this.expandedPreviewContainer = (View) expandedStickerView.getParent();
        this.onUpdateSizeListener = onUpdateSizeListener;
        this.onSelectionListener = onSelectionListener;
    }

    void setOnAddSelected(java.util.function.Consumer<Sticker> listener) { onAddSelected = listener; }

    @NonNull
    @Override
    public StickerPreviewViewHolder onCreateViewHolder(@NonNull final ViewGroup viewGroup, final int i) {
        View itemView = layoutInflater.inflate(R.layout.sticker_image_item, viewGroup, false);
        StickerPreviewViewHolder vh = new StickerPreviewViewHolder(itemView);

        ViewGroup.LayoutParams layoutParams = vh.stickerPreviewView.getLayoutParams();
        layoutParams.height = cellSize;
        layoutParams.width = cellSize;
        vh.stickerPreviewView.setLayoutParams(layoutParams);
        vh.stickerPreviewView.setPadding(cellPadding, cellPadding, cellPadding, cellPadding);

        return vh;
    }

    @Override
    public void onBindViewHolder(@NonNull final StickerPreviewViewHolder holder, final int position) {
        holder.stickerPreviewView.setImageResource(errorResource);
        holder.stickerPreviewView.setController(Fresco.newDraweeControllerBuilder()
                .setUri(StickerPackLoader.getStickerAssetUri(stickerPack.identifier, stickerPack.getStickers().get(position).imageFileName))
                .setOldController(holder.stickerPreviewView.getController())
                .setAutoPlayAnimations(true)
                .build());

        // Indicação visual de seleção
        holder.selectionOverlay.setVisibility(
                selectedPositions.contains(position) ? View.VISIBLE : View.GONE
        );
        holder.selectionCheck.setVisibility(
                selectedPositions.contains(position) ? View.VISIBLE : View.GONE
        );

        // Clique e long-click
        holder.stickerPreviewView.setOnLongClickListener(v -> {
            if (!selectionMode) {
                hideExpandedStickerPreview();
                // entrar em modo seleção
                selectionMode = true;
                selectedPositions.add(position);
                notifyItemChanged(position);
                onSelectionListener.onSelectionModeChanged(true);
                onSelectionListener.onSelectionCountChanged(selectedPositions.size());
            } else {
                toggleSelection(position);
            }
            return true;
        });

        holder.stickerPreviewView.setOnClickListener(v -> {
            if (selectionMode) {
                toggleSelection(position);
            } else {
                expandPreview(position, holder.stickerPreviewView);
            }
        });
    }

    private void toggleSelection(int position) {
        if (selectedPositions.contains(position)) {
            selectedPositions.remove(position);
        } else {
            selectedPositions.add(position);
        }
        notifyItemChanged(position);
        onSelectionListener.onSelectionCountChanged(selectedPositions.size());
        if (selectedPositions.isEmpty()) {
            // sair do modo seleção
            selectionMode = false;
            onSelectionListener.onSelectionModeChanged(false);
        }
    }

    @Override
    public void onViewRecycled(@NonNull StickerPreviewViewHolder holder) {
        holder.stickerPreviewView.setController(null);
        super.onViewRecycled(holder);
    }

    ArrayList<String> selectedFiles() {
        ArrayList<String> files = new ArrayList<>();
        for (int i = 0; i < stickerPack.getStickers().size(); i++) {
            if (selectedPositions.contains(i)) files.add(stickerPack.getStickers().get(i).imageFileName);
        }
        return files;
    }

    void restoreSelection(List<String> files) {
        selectedPositions.clear();
        for (int i = 0; i < stickerPack.getStickers().size(); i++) {
            if (files.contains(stickerPack.getStickers().get(i).imageFileName)) selectedPositions.add(i);
        }
        selectionMode = !selectedPositions.isEmpty();
        notifyDataSetChanged();
        onSelectionListener.onSelectionModeChanged(selectionMode);
        onSelectionListener.onSelectionCountChanged(selectedPositions.size());
    }

    void replacePack(StickerPack pack) {
        hideExpandedStickerPreview();
        stickerPack = pack;
        restoreSelection(Collections.emptyList());
    }

    public void handleDelete(Context context) {
        if ((stickerPack.getStickers().size() - selectedPositions.size()) < 3) {
            Toast.makeText(context, "Não foi possível excluir " + selectedPositions.size() + " figurinha(s)" +
                    "\nUm pacote deve ter no mínimo 3, excluindo " + selectedPositions.size() + " " +
                    "o pacote ficaria com " + (stickerPack.getStickers().size() - selectedPositions.size()), Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("Confirmar exclusão")
                .setMessage("Tem certeza que deseja excluir " + selectedPositions.size() + " figurinha(s)")
                .setPositiveButton("Sim", (d, which) -> {
                    deleteSelectedItems(context);
                })
                .setNegativeButton("Não", null)
                .show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.RED);
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.GRAY);
    }

    private void deleteSelectedItems(Context context) {
        if (selectedPositions.isEmpty()) return;
        List<Integer> toRemove = new ArrayList<>(selectedPositions);
        Collections.sort(toRemove, Collections.reverseOrder());

        ArrayList<String> imageFileNames = new ArrayList<>();
        for (int pos : toRemove) {
            imageFileNames.add(stickerPack.getStickers().get(pos).imageFileName);
        }

        ContentsJsonHelper.removerFigurinhas(stickerPack.identifier, imageFileNames, context);

        for (int pos : toRemove) {
            stickerPack.getStickers().remove(pos);
            notifyItemRemoved(pos);
            notifyItemRangeChanged(pos, stickerPack.getStickers().size() - pos);
        }

        ContentsJsonHelper.stickerPackAlterado = stickerPack;
        stickerPack.setStickers(stickerPack.getStickers());
        onUpdateSizeListener.onUpdateSizeListener(stickerPack);

        // reset selection
        selectedPositions.clear();
        selectionMode = false;
        onSelectionListener.onSelectionCountChanged(0);
        onSelectionListener.onSelectionModeChanged(false);
    }


    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.recyclerView = recyclerView;
        recyclerView.addOnScrollListener(hideExpandedViewScrollListener);
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        recyclerView.removeOnScrollListener(hideExpandedViewScrollListener);
        this.recyclerView = null;
    }

    private final RecyclerView.OnScrollListener hideExpandedViewScrollListener =
            new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    if (dx != 0 || dy != 0) {
                        hideExpandedStickerPreview();
                    }
                }
            };

    private void positionExpandedStickerPreview(int selectedPosition) {
        if (expandedStickerPreview != null) {
            // Calculate the view's center (x, y), then use expandedStickerPreview's height and
            // width to
            // figure out what where to position it.
            final ViewGroup.MarginLayoutParams recyclerViewLayoutParams =
                    ((ViewGroup.MarginLayoutParams) recyclerView.getLayoutParams());
            final int recyclerViewLeftMargin = recyclerViewLayoutParams.leftMargin;
            final int recyclerViewRightMargin = recyclerViewLayoutParams.rightMargin;
            final int recyclerViewWidth = recyclerView.getWidth();
            final int recyclerViewHeight = recyclerView.getHeight();

            final StickerPreviewViewHolder clickedViewHolder =
                    (StickerPreviewViewHolder)
                            recyclerView.findViewHolderForAdapterPosition(selectedPosition);
            if (clickedViewHolder == null) {
                hideExpandedStickerPreview();
                return;
            }
            clickedStickerPreview = clickedViewHolder.itemView;
            final float clickedViewCenterX =
                    clickedStickerPreview.getX()
                            + recyclerViewLeftMargin
                            + clickedStickerPreview.getWidth() / 2f;
            final float clickedViewCenterY =
                    clickedStickerPreview.getY() + clickedStickerPreview.getHeight() / 2f;

            expandedViewLeftX = clickedViewCenterX - expandedPreviewContainer.getWidth() / 2f;
            expandedViewTopY = clickedViewCenterY - expandedPreviewContainer.getHeight() / 2f;

            // If the new x or y positions are negative, anchor them to 0 to avoid clipping
            // the left side of the device and the top of the recycler view.
            expandedViewLeftX = Math.max(expandedViewLeftX, 0);
            expandedViewTopY = Math.max(expandedViewTopY, 0);

            // If the bottom or right sides are clipped, we need to move the top left positions
            // so that those sides are no longer clipped.
            final float adjustmentX =
                    Math.max(
                            expandedViewLeftX
                                    + expandedPreviewContainer.getWidth()
                                    - recyclerViewWidth
                                    - recyclerViewRightMargin,
                            0);
            final float adjustmentY =
                    Math.max(expandedViewTopY + expandedPreviewContainer.getHeight() - recyclerViewHeight, 0);

            expandedViewLeftX -= adjustmentX;
            expandedViewTopY -= adjustmentY;


            expandedPreviewContainer.setX(Math.max(0, expandedViewLeftX));
            expandedPreviewContainer.setY(Math.max(0, expandedViewTopY));
        }
    }

    private void expandPreview(int position, View clickedStickerPreview) {
        if (isStickerPreviewExpanded()) {
            hideExpandedStickerPreview();
            return;
        }

        this.clickedStickerPreview = clickedStickerPreview;
        if (expandedStickerPreview != null) {
            positionExpandedStickerPreview(position);

            final Uri stickerAssetUri = StickerPackLoader.getStickerAssetUri(stickerPack.identifier, stickerPack.getStickers().get(position).imageFileName);
            DraweeController controller = Fresco.newDraweeControllerBuilder()
                    .setUri(stickerAssetUri)
                    .setAutoPlayAnimations(true)
                    .build();
            expandedStickerPreview.setImageResource(errorResource);
            expandedStickerPreview.setController(controller);

            expandedPreviewContainer.setVisibility(View.VISIBLE);
            expandedPreviewContainer.findViewById(R.id.add_selected_sticker).setOnClickListener(v -> {
                Sticker selected = stickerPack.getStickers().get(position);
                hideExpandedStickerPreview();
                if (onAddSelected != null) onAddSelected.accept(selected);
            });
            recyclerView.setAlpha(EXPANDED_STICKER_PREVIEW_BACKGROUND_ALPHA);

            expandedStickerPreview.setOnClickListener(v -> hideExpandedStickerPreview());
        }
    }

    public void hideExpandedStickerPreview() {
        if (isStickerPreviewExpanded() && expandedStickerPreview != null) {
            clickedStickerPreview.setVisibility(View.VISIBLE);
            expandedPreviewContainer.setVisibility(View.INVISIBLE);
            recyclerView.setAlpha(COLLAPSED_STICKER_PREVIEW_BACKGROUND_ALPHA);
        }
    }

    private boolean isStickerPreviewExpanded() {
        return expandedStickerPreview != null && expandedPreviewContainer.getVisibility() == View.VISIBLE;
    }

    @Override
    public int getItemCount() {
        int numberOfPreviewImagesInPack;
        numberOfPreviewImagesInPack = stickerPack.getStickers().size();
        if (cellLimit > 0) {
            return Math.min(numberOfPreviewImagesInPack, cellLimit);
        }
        return numberOfPreviewImagesInPack;
    }

    public interface OnUpdateSizeListener {
        void onUpdateSizeListener(StickerPack stickerPack);
    }
}
