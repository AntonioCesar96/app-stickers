package com.example.samplestickerapp;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Environment;

import androidx.appcompat.app.AlertDialog;

import java.io.File;

public class FilesHelper {

    private static Context context;

    public static void setContext(Context c) {
        context = c;
    }

    // TODO: Alterar para os frames e os webp temporarios serem criados na pasta privada, os oficiais na pasta no SD
    public static File getRootDir() {
        return Environment.getExternalStorageDirectory();
    }

//    public static File getRootDir() {
//        if(context == null)
//            return null;
//        return context.getExternalFilesDir(null);
//    }

    public static File getFigurinhaDir() {
        return new File(getRootDir(), "00-Figurinhas");
    }

    public static File getAssetsDir() {
        return new File(getFigurinhaDir(), "assets");
    }

    public static File getTempDir() {
        File file = new File(context.getCacheDir(), "temp");
        if (!file.exists()) file.mkdirs();

        return file;
    }
}
