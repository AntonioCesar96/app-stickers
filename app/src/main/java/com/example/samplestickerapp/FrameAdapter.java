package com.example.samplestickerapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class FrameAdapter extends RecyclerView.Adapter<FrameAdapter.FrameViewHolder> {
    private final List<Bitmap> frames;
    private final File[] frameFiles;
    private final Context context;

    public FrameAdapter(Context context, List<Bitmap> frames, File[] frameFiles) {
        this.context = context;
        this.frames = frames;
        this.frameFiles = frameFiles;
    }

    @NonNull
    @Override
    public FrameViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ImageView imageView = new ImageView(parent.getContext());

        LinearLayout container = new LinearLayout(parent.getContext());
        container.setOrientation(LinearLayout.VERTICAL);
        container.setGravity(Gravity.CENTER_HORIZONTAL);

        int width = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                160,
                context.getResources().getDisplayMetrics()
        );

        int height = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                160,
                context.getResources().getDisplayMetrics()
        );

        int marginBottom = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                5,
                context.getResources().getDisplayMetrics()
        );

        LinearLayout.LayoutParams imageParams = new LinearLayout.LayoutParams(
                width, //ViewGroup.LayoutParams.WRAP_CONTENT,
                height
        );
        imageParams.setMargins(0, 0, 0, marginBottom);
        imageView.setLayoutParams(imageParams);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

        container.addView(imageView);

        return new FrameViewHolder(container, imageView);
    }

    @Override
    public void onBindViewHolder(@NonNull FrameViewHolder holder, int position) {
        Bitmap frame = frames.get(position);
        holder.imageView.setImageBitmap(frame);
//        holder.imageView.setOnClickListener(v -> {
//            try {
//                File src = frameFiles[position];
//                File dest = new File(Environment.getExternalStorageDirectory(), "00-Figurinhas/temp/selected_frame_" + position + ".png");
//                try (InputStream in = new FileInputStream(src);
//                     OutputStream out = new FileOutputStream(dest)) {
//                    byte[] buffer = new byte[4096];
//                    int length;
//                    while ((length = in.read(buffer)) > 0) {
//                        out.write(buffer, 0, length);
//                    }
//                }
//                Toast.makeText(context, "Frame salvo: " + dest.getAbsolutePath(), Toast.LENGTH_SHORT).show();
//            } catch (IOException e) {
//                e.printStackTrace();
//                Toast.makeText(context, "Erro ao salvar frame", Toast.LENGTH_SHORT).show();
//            }
//        });
    }

    @Override
    public int getItemCount() {
        return frames.size();
    }

    static class FrameViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public FrameViewHolder(@NonNull View itemView, ImageView imageView) {
            super(itemView);
            this.imageView = imageView;
        }
    }
}
