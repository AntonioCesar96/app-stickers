package com.example.samplestickerapp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.UUID;

/** Copies original bytes to managed collections, keeping source packs intact. */
final class SelectedStickers {
    private static final String MARKER = ".selected-stickers";

    static final class Result {
        final String identifier;
        final int count;
        final boolean duplicate;
        Result(File directory, boolean duplicate) throws IOException {
            identifier = directory.getName(); count = webps(directory).length; this.duplicate = duplicate;
        }
    }

    static boolean isCollection(File directory) { return new File(directory, MARKER).isFile(); }

    static synchronized Result add(File assets, String sourceId, String name, boolean animated) throws IOException {
        File root = assets.getCanonicalFile(), source = new File(root, sourceId).getCanonicalFile();
        File input = new File(source, name).getCanonicalFile();
        if (!root.equals(source.getParentFile()) || !source.equals(input.getParentFile())
                || !input.isFile() || input.length() == 0 || !name.toLowerCase(Locale.ROOT).endsWith(".webp"))
            throw new IOException("A figurinha não está mais disponível. Atualize o pacote.");
        if (new File(source, "animado.txt").isFile() != animated)
            throw new IOException("O tipo do pacote foi alterado. Abra-o novamente.");
        String digest = digest(input);
        File target = null;
        File[] directories = root.listFiles(File::isDirectory);
        if (directories == null) throw new IOException("Não foi possível ler os pacotes.");
        Arrays.sort(directories, Comparator.comparing(File::getName));
        for (File directory : directories) {
            if (!isCollection(directory) || new File(directory, "animado.txt").isFile() != animated) continue;
            File[] existing = webps(directory);
            for (File sticker : existing) if (digest.equals(digest(sticker))) return new Result(directory, true);
            if (target == null && existing.length < 30) target = directory;
        }
        if (target == null) {
            String base = animated ? "Selecionadas - animadas" : "Selecionadas - estaticas";
            target = new File(root, base);
            for (int i = 2; target.exists(); i++) target = new File(root, base + " " + i);
            // Prepare outside assets: directory scanning never sees a half-created collection.
            File stage = new File(root.getParentFile(), ".selected-" + UUID.randomUUID());
            if (!stage.mkdir()) throw new IOException("Não foi possível preparar o pacote de selecionadas.");
            try {
                copy(input, new File(stage, "01_" + digest + ".webp"));
                copy(new File(source, "icone.png"), new File(stage, "icone.png"));
                createMarker(new File(stage, MARKER));
                createMarker(new File(stage, "v_1.txt"));
                if (animated) createMarker(new File(stage, "animado.txt"));
                if (!stage.renameTo(target)) throw new IOException("Não foi possível criar o pacote de selecionadas.");
            } finally { cleanup(stage); }
        } else {
            File destination = new File(target, String.format(Locale.US, "%02d_%s.webp", nextIndex(target), digest));
            File staged = File.createTempFile("selected-", ".tmp", target);
            boolean committed = false;
            try {
                copy(input, staged);
                if (!staged.renameTo(destination)) throw new IOException("Não foi possível copiar a figurinha.");
                bumpVersion(target);
                committed = true;
            } finally {
                staged.delete();
                if (!committed) destination.delete();
            }
        }
        return new Result(target, false);
    }

    private static File[] webps(File directory) throws IOException {
        File[] files = directory.listFiles(f -> f.isFile() && f.getName().toLowerCase(Locale.ROOT).endsWith(".webp"));
        if (files == null) throw new IOException("Não foi possível ler o pacote.");
        return files;
    }

    private static long nextIndex(File directory) throws IOException {
        long next = 1;
        for (File file : webps(directory)) {
            try { next = Math.max(next, Long.parseLong(file.getName().split("_")[0]) + 1); }
            catch (NumberFormatException ignored) { }
        }
        return next;
    }

    private static String digest(File file) throws IOException {
        try {
            MessageDigest hash = MessageDigest.getInstance("SHA-256");
            try (FileInputStream input = new FileInputStream(file)) {
                byte[] buffer = new byte[16384]; int count;
                while ((count = input.read(buffer)) != -1) hash.update(buffer, 0, count);
            }
            StringBuilder hex = new StringBuilder();
            for (byte value : hash.digest()) hex.append(String.format(Locale.US, "%02x", value & 255));
            return hex.toString();
        } catch (NoSuchAlgorithmException e) { throw new IOException("SHA-256 indisponível.", e); }
    }

    private static void copy(File source, File destination) throws IOException {
        try (FileInputStream input = new FileInputStream(source); FileOutputStream output = new FileOutputStream(destination)) {
            byte[] buffer = new byte[16384]; int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            output.getFD().sync();
        }
    }

    private static void createMarker(File file) throws IOException {
        if (!file.createNewFile()) throw new IOException("Não foi possível preparar o pacote.");
    }

    private static void bumpVersion(File directory) throws IOException {
        File[] versions = directory.listFiles((dir, name) -> name.matches("v_[0-9]+\\.txt"));
        if (versions == null) throw new IOException("Não foi possível ler a versão do pacote.");
        long version = 0;
        try { for (File file : versions) version = Math.max(version, Long.parseLong(file.getName().substring(2).replace(".txt", ""))); }
        catch (NumberFormatException e) { throw new IOException("Versão inválida.", e); }
        if (version >= Integer.MAX_VALUE) throw new IOException("Versão do pacote fora do limite.");
        File marker = new File(directory, "v_" + (version + 1) + ".txt");
        createMarker(marker);
        for (File old : versions) if (!old.delete()) {
            marker.delete();
            throw new IOException("Não foi possível substituir a versão do pacote.");
        }
    }

    private static void cleanup(File directory) {
        File[] children = directory.listFiles();
        if (children != null) for (File file : children) file.delete();
        directory.delete();
    }
}
