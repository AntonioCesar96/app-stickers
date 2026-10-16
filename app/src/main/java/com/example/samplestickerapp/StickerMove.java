package com.example.samplestickerapp;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Moves original WebP bytes on the same filesystem; rolls back a failed batch. */
final class StickerMove {
    static boolean compatible(String sourceId, boolean animated, String targetId, boolean targetAnimated,
                              int targetCount, int selectedCount) {
        return !sourceId.equals(targetId) && animated == targetAnimated && selectedCount > 0
                && targetCount + selectedCount <= 30;
    }

    static synchronized void move(File assets, String sourceId, String targetId, List<String> names,
                                  boolean animated, boolean targetAnimated) throws IOException {
        File source = packDirectory(assets, sourceId), target = packDirectory(assets, targetId);
        if (names.isEmpty() || new HashSet<>(names).size() != names.size())
            throw new IOException("Selecione uma ou mais figurinhas sem repetições.");
        if (!compatible(sourceId, animated, targetId, targetAnimated, webps(target).length, names.size()))
            throw new IOException("O pacote de destino não é compatível ou não tem espaço para a seleção.");
        if (webps(source).length - names.size() < 3)
            throw new IOException("O pacote de origem precisa manter pelo menos 3 figurinhas.");
        List<File> originals = new ArrayList<>(), destinations = new ArrayList<>();
        long next = 1;
        for (File file : webps(target)) {
            try { next = Math.max(next, Long.parseLong(file.getName().split("_")[0]) + 1); }
            catch (NumberFormatException ignored) { }
        }
        for (String name : names) {
            File file = new File(source, name).getCanonicalFile();
            if (!source.equals(file.getParentFile()) || !file.isFile() || !name.toLowerCase(Locale.ROOT).endsWith(".webp"))
                throw new IOException("Uma figurinha selecionada não está mais disponível. Atualize o pacote.");
            File destination = new File(target, String.format(Locale.US, "%02d_%s.webp", next++, UUID.randomUUID()));
            if (destination.exists()) throw new IOException("Já existe um arquivo com o mesmo nome no destino.");
            originals.add(file); destinations.add(destination);
        }
        int moved = 0;
        try {
            for (int i = 0; i < originals.size(); i++) {
                if (!originals.get(i).renameTo(destinations.get(i))) throw new IOException("Não foi possível mover a seleção.");
                moved++;
            }
            bumpVersion(source);
            bumpVersion(target);
        } catch (IOException error) {
            boolean restored = true;
            for (int i = moved - 1; i >= 0; i--) restored &= destinations.get(i).renameTo(originals.get(i));
            if (!restored) throw new IOException("Parte da seleção já está no destino. Os arquivos foram preservados; atualize os pacotes.", error);
            throw error;
        }
    }

    private static File packDirectory(File assets, String id) throws IOException {
        File root = assets.getCanonicalFile(), directory = new File(root, id).getCanonicalFile();
        if (!root.equals(directory.getParentFile()) || !directory.isDirectory())
            throw new IOException("O pacote não está mais disponível.");
        return directory;
    }

    private static File[] webps(File directory) throws IOException {
        File[] files = directory.listFiles(f -> f.isFile() && f.getName().toLowerCase(Locale.ROOT).endsWith(".webp"));
        if (files == null) throw new IOException("Não foi possível ler o pacote.");
        return files;
    }

    private static void bumpVersion(File directory) throws IOException {
        File[] versions = directory.listFiles((dir, name) -> name.matches("v_[0-9]+\\.txt"));
        if (versions == null) throw new IOException("Não foi possível atualizar a versão do pacote.");
        long version = 0;
        try {
            for (File file : versions) version = Math.max(version, Long.parseLong(file.getName().substring(2).replace(".txt", "")));
        } catch (NumberFormatException e) { throw new IOException("Versão do pacote inválida.", e); }
        if (version >= Integer.MAX_VALUE) throw new IOException("Versão do pacote fora do limite permitido.");
        File marker = new File(directory, "v_" + (version + 1) + ".txt");
        if (!marker.createNewFile()) throw new IOException("Não foi possível atualizar a versão do pacote.");
        for (File old : versions) if (!old.delete()) throw new IOException("Não foi possível substituir a versão do pacote.");
    }
}
