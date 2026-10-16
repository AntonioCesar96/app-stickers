package com.example.samplestickerapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.drawee.view.SimpleDraweeView;

final class MoveStickerPacksAdapter extends RecyclerView.Adapter<MoveStickerPacksAdapter.Holder> {
    private final MoveStickersViewModel model;
    MoveStickerPacksAdapter(MoveStickersViewModel model) { this.model = model; }

    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_move_sticker_pack, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull Holder holder, int position) {
        StickerPack pack = model.destinations.get(position);
        boolean selected = model.selected != null && model.selected.identifier.equals(pack.identifier);
        holder.name.setText(pack.name);
        holder.count.setText(pack.getStickers().size() + "/30 figurinhas • "
                + (pack.animatedStickerPack ? "Animadas" : "Estáticas"));
        holder.checked.setChecked(selected);
        holder.itemView.setSelected(selected);
        holder.itemView.setEnabled(!model.moving);
        holder.itemView.setContentDescription(pack.name + ", " + holder.count.getText()
                + (selected ? ", selecionado" : ""));
        holder.itemView.setOnClickListener(v -> model.select(pack));
        holder.previews.removeAllViews();
        for (int i = 0; i < Math.min(4, pack.getStickers().size()); i++) {
            SimpleDraweeView image = (SimpleDraweeView) LayoutInflater.from(holder.itemView.getContext())
                    .inflate(R.layout.sticker_packs_list_image_item, holder.previews, false);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0,
                    Math.round(56 * holder.itemView.getResources().getDisplayMetrics().density), 1);
            image.setLayoutParams(params);
            image.getHierarchy().setActualImageScaleType(com.facebook.drawee.drawable.ScalingUtils.ScaleType.FIT_CENTER);
            image.setImageURI(StickerPackLoader.getStickerAssetUri(pack.identifier, pack.getStickers().get(i).imageFileName));
            image.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            holder.previews.addView(image);
        }
    }

    @Override public int getItemCount() { return model.destinations.size(); }

    static final class Holder extends RecyclerView.ViewHolder {
        final TextView name, count;
        final RadioButton checked;
        final LinearLayout previews;
        Holder(View view) {
            super(view);
            name = view.findViewById(R.id.move_pack_name);
            count = view.findViewById(R.id.move_pack_count);
            checked = view.findViewById(R.id.move_pack_checked);
            previews = view.findViewById(R.id.move_pack_previews);
        }
    }
}
