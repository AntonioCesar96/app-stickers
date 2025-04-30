package com.example.samplestickerapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

public class RecentFileDao {
    private final RecentFileDbHelper dbHelper;

    public RecentFileDao(Context context) {
        dbHelper = new RecentFileDbHelper(context);
    }

    /**
     * Insere um novo caminho de arquivo e retorna o ID gerado.
     */
    public long inserir(String caminho) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.query(
                RecentFileDbHelper.TABLE_NAME,
                new String[]{RecentFileDbHelper.COLUMN_ID},
                RecentFileDbHelper.COLUMN_PATH + " = ?",
                new String[]{caminho},
                null, null, null
        );
        boolean exists = c.moveToFirst();
        c.close();
        if (exists) {
            db.close();
            return -1;  // já existe
        }
        ContentValues cv = new ContentValues();
        cv.put(RecentFileDbHelper.COLUMN_PATH, caminho);
        long id = dbHelper.getWritableDatabase()
                .insert(RecentFileDbHelper.TABLE_NAME, null, cv);
        db.close();
        return id;
    }

    /**
     * Busca todos os caminhos de arquivo salvos.
     */
    public List<String> buscarTodos() {
        List<String> lista = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String[] colunas = {RecentFileDbHelper.COLUMN_PATH};
        Cursor cursor = db.query(
                RecentFileDbHelper.TABLE_NAME,
                colunas,
                null, null, null, null,
                RecentFileDbHelper.COLUMN_ID + " DESC"   // ordem: mais recentes primeiro
        );

        if (cursor != null) {
            while (cursor.moveToNext()) {
                String caminho = cursor.getString(
                        cursor.getColumnIndexOrThrow(RecentFileDbHelper.COLUMN_PATH));
                lista.add(caminho);
            }
            cursor.close();
        }

        db.close();
        return lista;
    }

    /**
     * Exclui o registro com o caminho especificado.
     */
    public int excluir(String caminho) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String where = RecentFileDbHelper.COLUMN_PATH + " = ?";
        String[] args = {caminho};
        int deleted = db.delete(RecentFileDbHelper.TABLE_NAME, where, args);
        db.close();
        return deleted;
    }

    /**
     * Opcional: limpa todos os registros.
     */
    public int excluirTodos() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int deleted = db.delete(RecentFileDbHelper.TABLE_NAME, null, null);
        db.close();
        return deleted;
    }
}
