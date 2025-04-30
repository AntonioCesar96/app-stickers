package com.example.samplestickerapp;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
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

    public static void showAlertDialogOld(Context context, String titulo, String msg, String ok, String no,
                                       CallbackOkDialog callbackOkDialog, CallbackNoDialog callbackNoDialog) {

        LayoutInflater inflater = ((AppCompatActivity) context).getLayoutInflater();
        View customTitleView = inflater.inflate(R.layout.custom_dialog_title, null);
        ImageButton closeButton = customTitleView.findViewById(R.id.closeButton);
        TextView dialogTitle = customTitleView.findViewById(R.id.dialogTitle);

        if (titulo == null || titulo.isEmpty())
            dialogTitle.setTextSize(0);
        else
            dialogTitle.setText(titulo);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setCustomTitle(customTitleView)
                .setCancelable(false)
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

        closeButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.BLACK);
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.BLACK);
    }


    public static void showAlertDialog(Context context,
                                        String titulo,
                                        String msg,
                                        String okText,
                                        String noText,
                                        CallbackOkDialog callbackOkDialog,
                                        CallbackNoDialog callbackNoDialog) {

        // Infla apenas o body, sem título customizado
        LayoutInflater inflater = ((AppCompatActivity) context).getLayoutInflater();
        View bodyView = inflater.inflate(R.layout.dialog_body_with_buttons, null);

        // Ajusta título (se quiser mostrar título, pode usar setTitle)
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setCancelable(false)          // NÃO permite fechar tocando fora ou Back
                .setTitle(titulo)
                .setMessage(null)              // retira message nativo
                .setPositiveButton("Fechar", (dialog2, which) -> {
                    dialog2.dismiss();
                })
                .setView(bodyView);

        // Monta o diálogo
        AlertDialog dialog = builder.create();
        dialog.show();

        // Referências dos elementos
        TextView tvMsg = bodyView.findViewById(R.id.dialogMessage);
        Button btnProcessar = bodyView.findViewById(R.id.buttonProcessar);
        Button btnFechar = bodyView.findViewById(R.id.buttonFechar);

        // Aplica textos recebidos
        tvMsg.setText(msg);
        btnProcessar.setText(okText);
        btnFechar.setText(noText);

        btnProcessar.setOnClickListener(v -> {
            callbackOkDialog.callback();
            dialog.dismiss();
        });

        btnFechar.setOnClickListener(v -> {
            callbackNoDialog.callback();
            dialog.dismiss();
        });

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.BLACK);
    }


    public static void showAlertDialog3333333(Context context, String titulo, String msg, String ok, String no,
                                              CallbackOkDialog callbackOkDialog, CallbackNoDialog callbackNoDialog) {

        LayoutInflater inflater = ((AppCompatActivity) context).getLayoutInflater();
        View customTitleView = inflater.inflate(R.layout.custom_dialog_title, null);
        ImageButton closeButton = customTitleView.findViewById(R.id.closeButton);
        TextView dialogTitle = customTitleView.findViewById(R.id.dialogTitle);

        if (titulo == null || titulo.isEmpty())
            dialogTitle.setTextSize(0);
        else
            dialogTitle.setText(titulo);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setCustomTitle(customTitleView)
                .setCancelable(false)
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

        closeButton.setOnClickListener(v -> dialog.dismiss());

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
