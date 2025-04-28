package com.example.samplestickerapp;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Environment;

import androidx.appcompat.app.AlertDialog;

import java.io.File;

public class FilesHelper {

    public static File getRootDir() {
        return Environment.getExternalStorageDirectory();
    }

    public static File getFigurinhaDir() {
        return new File(getRootDir(), "00-Figurinhas");
    }

    public static File getAssetsDir() {
        return new File(getFigurinhaDir(), "assets");
    }

    public static File getTempDir() {
        return new File(getFigurinhaDir(), "temp");
    }
}
