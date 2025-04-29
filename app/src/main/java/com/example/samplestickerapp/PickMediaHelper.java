package com.example.samplestickerapp;

import static android.app.Activity.RESULT_OK;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;

public class PickMediaHelper {
    public static final int REQUEST_PICK_MEDIA = 1001;

    public static void open(Context context) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"image/*", "video/*"});
        ((AppCompatActivity) context).startActivityForResult(intent, REQUEST_PICK_MEDIA);
    }

    public static File onActivityResult(Context context, int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_PICK_MEDIA && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                String displayName = GetAbsolutePathFromUri.getPath(context, uri);
                return new File(displayName);
            }
        }

        return null;
    }
}
