package com.example.samplestickerapp;

import android.content.res.AssetManager;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Reads the immutable samples used by SplashActivity.criarPastasIniciais(). */
final class InitialStickerTemplates implements SelectedStickers.Templates {
    private final AssetManager assets;
    InitialStickerTemplates(AssetManager assets) { this.assets = assets; }
    private String directory(boolean animated) { return animated ? "Pacote 1" : "Pacote 2"; }

    @Override public String[] names(boolean animated) throws IOException {
        String[] files = assets.list(directory(animated));
        List<String> names = new ArrayList<>();
        if (files != null) for (String file : files)
            if (file.toLowerCase(Locale.ROOT).endsWith(".webp")) names.add(file);
        return names.toArray(new String[0]);
    }

    @Override public InputStream open(boolean animated, String name) throws IOException {
        if (name.contains("/") || name.contains("\\")) throw new IOException("Nome de figurinha inicial inválido.");
        return assets.open(directory(animated) + "/" + name);
    }
}
