package com.example.samplestickerapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.io.File;
import java.util.List;

public class ContatoSpinnerAdapter extends ArrayAdapter<StickerPack> {

    private LayoutInflater inflater;

    public ContatoSpinnerAdapter(@NonNull Context context, @NonNull List<StickerPack> objects) {
        super(context, 0, objects);
        inflater = LayoutInflater.from(context);
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return createItemView(position, convertView, parent);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // This is the “closed” state view (showing selected item)
        return createItemView(position, convertView, parent);
    }

    private View createItemView(int position, View convertView, ViewGroup parent) {
        final View view = convertView != null
                ? convertView
                : inflater.inflate(R.layout.spinner_item, parent, false);

        TextView tvNome = view.findViewById(R.id.textNome);
        ImageView ivThumb = view.findViewById(R.id.imageThumb);

        StickerPack item = getItem(position);
        if (item != null) {
            tvNome.setText(item.name);

            String imagePath = new File(Environment.getExternalStorageDirectory(),
                    "00-Figurinhas/assets/" + item.identifier + "/icone.png").getAbsolutePath();
            Bitmap bmp = BitmapFactory.decodeFile(imagePath);
            ivThumb.setImageBitmap(bmp);
        }
        return view;
    }
}
