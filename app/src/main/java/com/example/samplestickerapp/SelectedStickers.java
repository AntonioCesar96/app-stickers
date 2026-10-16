package com.example.samplestickerapp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.UUID;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Copies original bytes to managed collections, keeping source packs intact. */
final class SelectedStickers {
    private static final String MARKER = ".selected-stickers";
    private static final String FILLER_PREFIX = "00_selected-filler_";

    interface Templates {
        String[] names(boolean animated) throws IOException;
        InputStream open(boolean animated, String name) throws IOException;
    }

    static final class Result {
        final String identifier;
        final int count;
        final int selectedCount, fillerCount;
        final boolean duplicate;
        Result(File directory, boolean duplicate) throws IOException {
            identifier = directory.getName(); count = webps(directory).length; this.duplicate = duplicate;
            int fillers = 0;
            for (File file : webps(directory)) if (isFiller(file)) fillers++;
            fillerCount = fillers; selectedCount = count - fillers;
        }
    }

    static boolean isCollection(File directory) { return new File(directory, MARKER).isFile(); }

    static synchronized Result add(File assets, String sourceId, String name, boolean animated, Templates templates) throws IOException {
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
            for (File sticker : existing) if (!isFiller(sticker) && digest.equals(digest(sticker))) {
                reconcile(directory, null, animated, templates);
                return new Result(directory, true);
            }
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
                prepareFillers(stage, Arrays.asList(webps(stage)), animated, templates);
                createMarker(new File(stage, MARKER));
                createMarker(new File(stage, "v_1.txt"));
                if (animated) createMarker(new File(stage, "animado.txt"));
                if (!stage.renameTo(target)) throw new IOException("Não foi possível criar o pacote de selecionadas.");
            } finally { cleanup(stage); }
        } else {
            reconcile(target, input, animated, templates);
        }
        return new Result(target, false);
    }

    /** Reserved names plus byte verification distinguish temporary copies from explicit choices. */
    static boolean isFiller(File file) throws IOException {
        return file.getName().matches(FILLER_PREFIX + "[a-f0-9]{64}\\.webp")
                && file.getName().equals(FILLER_PREFIX + digest(file) + ".webp");
    }

    private static Set<String> prepareFillers(File stage, List<File> selected, boolean animated, Templates templates) throws IOException {
        int needed = Math.max(0, 3 - selected.size());
        Set<String> desired = new HashSet<>(), hashes = new HashSet<>();
        if (needed == 0) return desired;
        for (File file : selected) hashes.add(digest(file));
        String[] names = templates.names(animated);
        Arrays.sort(names);
        for (String name : names) {
            File candidate = new File(stage, "template.tmp");
            try (InputStream input = templates.open(animated, name)) { copy(input, candidate); }
            String hash = digest(candidate);
            if (hashes.add(hash)) {
                String fillerName = FILLER_PREFIX + hash + ".webp";
                if (!candidate.renameTo(new File(stage, fillerName))) throw new IOException("Não foi possível preparar a figurinha inicial.");
                desired.add(fillerName);
                if (desired.size() == needed) return desired;
            } else candidate.delete();
        }
        throw new IOException("Não há figurinhas iniciais distintas suficientes para completar o mínimo de 3.");
    }

    private static void reconcile(File target, File input, boolean animated, Templates templates) throws IOException {
        List<File> selected = new ArrayList<>(), fillers = new ArrayList<>();
        for (File file : webps(target)) { if (isFiller(file)) fillers.add(file); else selected.add(file); }
        File stage = new File(target.getParentFile().getParentFile(), ".selected-" + UUID.randomUUID());
        if (!stage.mkdir()) throw new IOException("Não foi possível preparar a seleção.");
        List<File> published = new ArrayList<>(), removed = new ArrayList<>();
        boolean committed = false;
        try {
            if (input != null) {
                File addition = new File(stage, String.format(Locale.US, "%02d_%s.webp", nextIndex(target), digest(input)));
                copy(input, addition); selected.add(addition);
            }
            Set<String> desired = prepareFillers(stage, selected, animated, templates);
            for (File prepared : webps(stage)) {
                File destination = new File(target, prepared.getName());
                if (destination.exists()) {
                    if (!desired.contains(prepared.getName()) || !isFiller(destination))
                        throw new IOException("Já existe um arquivo com o nome reservado no pacote.");
                    continue;
                }
                if (!prepared.renameTo(destination)) throw new IOException("Não foi possível publicar a figurinha.");
                published.add(destination);
            }
            // Publish replacements first, so removing filler never leaves fewer than three on success.
            for (File filler : fillers) if (!desired.contains(filler.getName())) {
                if (!filler.renameTo(new File(stage, filler.getName()))) throw new IOException("Não foi possível substituir a figurinha temporária.");
                removed.add(filler);
            }
            if (!published.isEmpty() || !removed.isEmpty()) bumpVersion(target);
            committed = true;
        } finally {
            if (!committed) {
                boolean restored = true;
                for (File original : removed) restored &= new File(stage, original.getName()).renameTo(original);
                for (File file : published) restored &= file.delete();
                if (!restored) throw new IOException("Não foi possível concluir a seleção. Os arquivos temporários foram preservados em " + stage.getAbsolutePath());
            }
            cleanup(stage);
        }
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
        try (InputStream input = new FileInputStream(source)) { copy(input, destination); }
    }

    private static void copy(InputStream input, File destination) throws IOException {
        try (FileOutputStream output = new FileOutputStream(destination)) {
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
