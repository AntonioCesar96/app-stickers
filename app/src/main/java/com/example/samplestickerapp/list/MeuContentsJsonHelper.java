package com.example.samplestickerapp.list;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.Environment;

import androidx.annotation.NonNull;

import com.example.samplestickerapp.BuildConfig;
import com.example.samplestickerapp.Pasta;
import com.example.samplestickerapp.StickerContentProvider;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MeuContentsJsonHelper {

    public static ArrayList<MeuStickerPackModel> getStickerPacks() {
        File assetsDir = getAssetsDir();

        List<Pasta> pastas = new ArrayList<>();
        File[] diretorios = assetsDir.listFiles(File::isDirectory);

        if (diretorios == null)
            return new ArrayList<>();

        Arrays.sort(diretorios, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
        ArrayList<MeuStickerPackModel> stickerPacks = new ArrayList<>();

        for (File dir : diretorios) {
            String nomePasta = dir.getName();
            File[] arquivosAnimadosTxt = dir.listFiles((d, name) -> name.equalsIgnoreCase("animado.txt"));
            boolean ehAnimado = arquivosAnimadosTxt != null && arquivosAnimadosTxt.length > 0;

            final String identifier = nomePasta;
            final String name = nomePasta;
            final String publisher = "Antonio";
            final String trayImage = "icone.png";
            final String publisherEmail = "antoniocss19@Gmail.com";
            final String publisherWebsite = "";
            final String privacyPolicyWebsite = "";
            final String licenseAgreementWebsite = "";
            final String imageDataVersion = "1";
            final boolean avoidCache = false;
            final boolean animatedStickerPack = ehAnimado;
            final MeuStickerPackModel stickerPack = new MeuStickerPackModel(identifier, name, publisher, trayImage, publisherEmail, publisherWebsite, privacyPolicyWebsite, licenseAgreementWebsite, imageDataVersion, avoidCache, animatedStickerPack);

            List<MeuStickerModel> stickers = new ArrayList<>();
            File[] arquivosNaPasta = dir.listFiles((d, name1) -> name1.toLowerCase().endsWith(".webp"));
            if (arquivosNaPasta != null) {
                for (File arquivo : arquivosNaPasta) {
                    MeuStickerModel sticker = new MeuStickerModel(arquivo.getName(), Arrays.asList("😂", "🎉"), "", arquivo.length());
                    stickers.add(sticker);
                }
            }

            stickerPack.setStickers(stickers);
            stickerPacks.add(stickerPack);
        }

        return stickerPacks;
    }

    @NonNull
    private static File getAssetsDir() {
        File rootDir = Environment.getExternalStorageDirectory();
        File figurinhasDir = new File(rootDir, "00-Figurinhas");
        if (!figurinhasDir.exists()) {
            figurinhasDir.mkdirs();
        }
        File assetsDir = new File(figurinhasDir, "assets");
        if (!assetsDir.exists()) {
            assetsDir.mkdirs();
        }

        return assetsDir;
    }

    public static Uri getStickerAssetUri(String identifier, String stickerName) {
        File assetsDir = getAssetsDir();
        File file = new File(assetsDir, identifier + "/" + stickerName);
        return Uri.fromFile(file);
    }
}