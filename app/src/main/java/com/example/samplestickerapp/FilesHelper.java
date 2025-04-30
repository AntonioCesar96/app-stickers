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

    public static File getMp4Dir() {
        File file = new File(context.getCacheDir(), "mp4");
        if (!file.exists()) file.mkdirs();

        return file;
    }
}
