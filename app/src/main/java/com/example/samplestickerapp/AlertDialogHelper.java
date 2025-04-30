package com.example.samplestickerapp;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ContextThemeWrapper;

public class AlertDialogHelper {
    public static void showAlertDialog(String msg, Context context) {
        new AlertDialog.Builder(context)
                .setTitle("Informativo")
                .setMessage(msg)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setCancelable(false)
                .show();
    }

    public static void showAlertDialog(Context context, String titulo, String msg, String ok, String no,
                                       CallbackOkDialog callbackOkDialog, CallbackNoDialog callbackNoDialog) {
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(titulo)
                .setMessage(msg)
                .setPositiveButton(ok, (dialog2, which) -> {
                    callbackOkDialog.callback();
                    dialog2.dismiss();
                })
                .setNegativeButton(no, (dialog2, which) -> {
                    callbackNoDialog.callback();
                    dialog2.dismiss();
                })
                .setCancelable(false)
                .create();

        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.BLACK);
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.BLACK);
    }


    public interface CallbackOkDialog {
        void callback();
    }

    public interface CallbackNoDialog {
        void callback();
    }
}
