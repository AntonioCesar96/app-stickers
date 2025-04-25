package com.example.samplestickerapp;

import android.content.ContentProvider;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Environment;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class ContentsJsonHelper {

    public static StickerPack stickerPackAlterado;
    public static ArrayList<Sticker> stickersAlterados = new ArrayList<>();

    public static ArrayList<StickerPackModel> getStickerPacks() {
        File assetsDir = getAssetsDir();

        List<Pasta> pastas = new ArrayList<>();
        File[] diretorios = assetsDir.listFiles(File::isDirectory);

        if (diretorios == null)
            return new ArrayList<>();

        Arrays.sort(diretorios, (f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified()));

        for (File dir : diretorios) {
            String nomePasta = dir.getName();
            File[] arquivosAnimadosList = dir.listFiles((d, name) -> name.equalsIgnoreCase("animado.txt"));
            File[] versaoList = dir.listFiles((d, name) -> name.startsWith("v_") && name.endsWith(".txt"));

            boolean ehAnimado = arquivosAnimadosList != null && arquivosAnimadosList.length > 0;
            int versao = 1;
            if (versaoList != null) {
                String versaoTxt = versaoList[0].getName().replace(".txt", "").replace("v_", "");
                versao = Integer.parseInt(versaoTxt);
            }

            List<String> arquivos = new ArrayList<>();

            File[] arquivosNaPasta = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".webp"));
            if (arquivosNaPasta != null) {
                Arrays.sort(arquivosNaPasta, Comparator.comparingLong(f -> Long.parseLong(f.getName().split("_")[0])));
                for (File arquivo : arquivosNaPasta) {
                    arquivos.add(arquivo.getName());
                }
            }

            pastas.add(new Pasta(nomePasta, ehAnimado, arquivos, versao));
        }

        ArrayList<StickerPackModel> stickerPacks = new ArrayList<>();
        for (Pasta pasta : pastas) {

            final String identifier = pasta.nome;
            final String name = pasta.nome;
            final String publisher = "Antonio";
            final String trayImage = "icone.png";
            final String publisherEmail = "antoniocss19@gmail.com";
            final String publisherWebsite = "";
            final String privacyPolicyWebsite = "";
            final String licenseAgreementWebsite = "";
            final String imageDataVersion = "" + pasta.versao;
            final boolean avoidCache = false;
            final boolean animatedStickerPack = pasta.ehAnimado;
            final StickerPackModel stickerPack = new StickerPackModel(identifier, name, publisher, trayImage, publisherEmail, publisherWebsite, privacyPolicyWebsite, licenseAgreementWebsite, imageDataVersion, avoidCache, animatedStickerPack);

            List<StickerModel> stickers = new ArrayList<>();
            int count = 0;
            for (String arquivo : pasta.arquivos) {
                count += 1;
                if (count > 30) {
                    break;
                }

                StickerModel sticker = new StickerModel(arquivo, Arrays.asList("😂", "🎉"), "");
                stickers.add(sticker);
            }

            stickerPack.setStickers(stickers);
            stickerPacks.add(stickerPack);
        }

        return stickerPacks;
    }

    public static void atualizaContentsJson() {
        File assetsDir = getAssetsDir();
        ArrayList<StickerPackModel> stickerPacks = getStickerPacks();

        atualizaContentsJson(stickerPacks, assetsDir);
    }

    private static void atualizaContentsJson(ArrayList<StickerPackModel> stickerPacks, File assetsDir) {
        ContentsModel contents = new ContentsModel();
        contents.androidPlayStoreLink = "";
        contents.iosAppStoreLink = "";
        contents.stickerPacks = stickerPacks;

        Gson gson = new Gson();
        String json = gson.toJson(contents);

        File contentsJson = new File(assetsDir, "contents.json");
        try (FileWriter writer = new FileWriter(contentsJson)) {
            writer.write(json);
        } catch (Exception e) {
            e.printStackTrace();
        }
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


    public static void updatePack(String identifier) {
        File assetsDir = getAssetsDir();
        File identifierDir = new File(assetsDir, identifier);

        File[] versaoList = identifierDir.listFiles((d, name) -> name.startsWith("v_") && name.endsWith(".txt"));
        if (versaoList != null) {
            String versaoTxt = versaoList[0].getName().replace(".txt", "").replace("v_", "");
            int versao = Integer.parseInt(versaoTxt) + 1;

            versaoList[0].delete();

            File versaoFile = new File(identifierDir, "v_" + versao + ".txt");
            try (FileWriter writer = new FileWriter(versaoFile)) {
                writer.write(versao);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        ArrayList<StickerPackModel> stickerPacks = getStickerPacks();

        atualizaContentsJson(stickerPacks, assetsDir);
    }

    public static void atualizaContentProvider(Context context) {

        ContentResolver resolver = context.getContentResolver();
        ContentProviderClient client = resolver.acquireContentProviderClient(BuildConfig.CONTENT_PROVIDER_AUTHORITY);
        if (client != null) {
            ContentProvider provider = client.getLocalContentProvider();
            if (provider instanceof StickerContentProvider) {
                ((StickerContentProvider) provider).clearCache();
            }
            client.release();
        }
    }

    public static void atualizaContentsJsonAndContentProvider(Context context) {
        try {
            ContentsJsonHelper.atualizaContentsJson();
            ContentsJsonHelper.atualizaContentProvider(context);
        } catch (Exception e) {
            Toast.makeText(context, e.getMessage() + "\n" + Objects.requireNonNull(e.getCause()).getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    public static void updatePackAndContentProvider(String identifier, Context context) {
        try {
            ContentsJsonHelper.updatePack(identifier);
            ContentsJsonHelper.atualizaContentProvider(context);
        } catch (Exception e) {
            Toast.makeText(context, e.getMessage() + "\n" + Objects.requireNonNull(e.getCause()).getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    public static void removerFigurinha(String identifier, String imageFileName, Context context) {
        try {
            File sticker = new File(new File(getAssetsDir(), identifier), imageFileName);
            sticker.delete();

            ContentsJsonHelper.updatePack(identifier);
            ContentsJsonHelper.atualizaContentProvider(context);
        } catch (Exception e) {
            Toast.makeText(context, e.getMessage() + "\n" + Objects.requireNonNull(e.getCause()).getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    public static void removerFigurinhas(String identifier, ArrayList<String> imageFileNames, Context context) {
        try {
            for (String imageFileName : imageFileNames) {
                File sticker = new File(new File(getAssetsDir(), identifier), imageFileName);
                sticker.delete();
            }

            ContentsJsonHelper.updatePack(identifier);
            ContentsJsonHelper.atualizaContentProvider(context);
        } catch (Exception e) {
            Toast.makeText(context, e.getMessage() + "\n" + Objects.requireNonNull(e.getCause()).getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    public static void removerPacote(String identifier, Context context) {
        try {
            deleteRecursive(new File(getAssetsDir(), identifier));

            atualizaContentsJsonAndContentProvider(context);
        } catch (Exception e) {
            Toast.makeText(context, e.getMessage() + "\n" + Objects.requireNonNull(e.getCause()).getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    public static void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            File[] children = fileOrDirectory.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        fileOrDirectory.delete();
    }
}