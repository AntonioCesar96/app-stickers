package com.example.samplestickerapp;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import static org.junit.Assert.*;

public class SelectedStickersTest {
    @Rule public TemporaryFolder temp = new TemporaryFolder();

    private File source(File assets, String id, boolean animated) throws IOException {
        File pack = new File(assets, id); assertTrue(pack.mkdir());
        Files.write(new File(pack, "icone.png").toPath(), new byte[]{7, 8});
        if (animated) assertTrue(new File(pack, "animado.txt").createNewFile());
        return pack;
    }

    private void sticker(File source, String name, int value) throws IOException {
        Files.write(new File(source, name).toPath(), new byte[]{(byte) value, 2, 3});
    }

    @Test public void copiesOriginalBytesPreservesSourceAndBuildsDraftThenReadyPack() throws Exception {
        File assets = temp.newFolder("assets"), source = source(assets, "source", false);
        SelectedStickers.Result result = null;
        for (int i = 1; i <= 3; i++) {
            sticker(source, i + "_a.webp", i);
            result = add(assets, "source", i + "_a.webp", false);
            assertEquals(i, result.selectedCount); assertEquals(3, result.count); assertEquals(3 - i, result.fillerCount); assertFalse(result.duplicate);
            assertTrue(new File(source, i + "_a.webp").isFile());
        }
        File target = new File(assets, result.identifier);
        assertTrue(SelectedStickers.isCollection(target));
        assertTrue(new File(target, "v_3.txt").isFile());
        assertFalse(new File(target, "v_1.txt").exists());
        assertArrayEquals(new byte[]{7, 8}, Files.readAllBytes(new File(target, "icone.png").toPath()));
        assertEquals(3, target.listFiles((d, n) -> n.endsWith(".webp")).length);
        assertArrayEquals(new byte[]{1, 2, 3}, Files.readAllBytes(target.listFiles((d, n) -> n.startsWith("01_"))[0].toPath()));
    }

    @Test public void deduplicatesIdenticalBytesAcrossSourcesAndNames() throws Exception {
        File assets = temp.newFolder("assets"), a = source(assets, "a", false), b = source(assets, "b", false);
        sticker(a, "1_a.webp", 9); sticker(b, "2_b.webp", 9);
        SelectedStickers.Result first = add(assets, "a", "1_a.webp", false);
        SelectedStickers.Result again = add(assets, "b", "2_b.webp", false);
        assertTrue(again.duplicate); assertEquals(first.identifier, again.identifier); assertEquals(1, again.selectedCount); assertEquals(3, again.count);
    }

    @Test public void separatesTypesAndNeverReusesUnmanagedNameCollision() throws Exception {
        File assets = temp.newFolder("assets"), a = source(assets, "a", false), b = source(assets, "b", true);
        assertTrue(new File(assets, "Selecionadas - estaticas").mkdir());
        sticker(a, "1_a.webp", 1); sticker(b, "1_b.webp", 2);
        SelectedStickers.Result plain = add(assets, "a", "1_a.webp", false);
        SelectedStickers.Result animated = add(assets, "b", "1_b.webp", true);
        assertEquals("Selecionadas - estaticas 2", plain.identifier);
        assertNotEquals(plain.identifier, animated.identifier);
        assertTrue(new File(new File(assets, animated.identifier), "animado.txt").exists());
    }

    @Test public void startsNextPackAtThirtyAndFindsDuplicatesInFullPack() throws Exception {
        File assets = temp.newFolder("assets"), source = source(assets, "source", false);
        String first = null;
        for (int i = 1; i <= 31; i++) {
            sticker(source, i + "_a.webp", i);
            SelectedStickers.Result result = add(assets, "source", i + "_a.webp", false);
            if (i == 1) { first = result.identifier; assertEquals("Individuais - estaticas", result.name); }
            if (i == 30) assertEquals(30, result.count);
            if (i == 31) { assertEquals(1, result.selectedCount); assertEquals(3, result.count); assertNotEquals(first, result.identifier);
                assertEquals("Individuais - estaticas +1", result.name); }
        }
        SelectedStickers.Result duplicate = add(assets, "source", "1_a.webp", false);
        assertTrue(duplicate.duplicate); assertEquals(first, duplicate.identifier); assertEquals(30, duplicate.count);
    }

    @Test public void rejectsMissingOrEscapingSourceAndLeavesNoPartialPack() throws Exception {
        File assets = temp.newFolder("assets"), source = source(assets, "source", false);
        sticker(source, "1_a.webp", 1);
        expectFailure(() -> add(assets, "source", "../source/icone.png", false));
        expectFailure(() -> add(assets, "../assets/source", "1_a.webp", true));
        expectFailure(() -> add(assets, "source", "missing.webp", false));
        assertTrue(new File(source, "icone.png").delete());
        expectFailure(() -> add(assets, "source", "1_a.webp", false));
        assertEquals(1, assets.list().length);
        assertEquals(1, temp.getRoot().list().length);
    }

    @Test public void failedVersionUpdateRollsBackOnlyNewCopy() throws Exception {
        File assets = temp.newFolder("assets"), source = source(assets, "source", false);
        sticker(source, "1_a.webp", 1); sticker(source, "2_a.webp", 2);
        SelectedStickers.Result first = add(assets, "source", "1_a.webp", false);
        File target = new File(assets, first.identifier);
        assertTrue(new File(target, "v_2147483647.txt").createNewFile());
        expectFailure(() -> add(assets, "source", "2_a.webp", false));
        assertEquals(3, target.listFiles((d, n) -> n.endsWith(".webp")).length);
        assertEquals(0, target.listFiles((d, n) -> n.endsWith(".tmp")).length);
        assertTrue(new File(source, "2_a.webp").isFile());
    }

    private final SelectedStickers.Templates templates = new SelectedStickers.Templates() {
        public String[] names(boolean animated) { return new String[]{"0.webp", "1.webp", "2.webp"}; }
        public java.io.InputStream open(boolean animated, String name) {
            return new java.io.ByteArrayInputStream(new byte[]{(byte) (201 + Integer.parseInt(name.substring(0, 1))), 2, 3});
        }
    };

    @Test public void whatsappNamesPreserveInternalIdentifiersAndSeparateTypes() {
        assertEquals("Individuais - animadas", SelectedStickers.displayName("Selecionadas - animadas", true));
        assertEquals("Individuais - animadas +1", SelectedStickers.displayName("Selecionadas - animadas 2", true));
        assertEquals("Individuais - estaticas +2", SelectedStickers.displayName("Selecionadas - estaticas 3", false));
        assertEquals("Individuais - estaticas", SelectedStickers.displayName("custom", false));
    }

    @Test public void duplicateAlsoRepairsOldIncompleteCollection() throws Exception {
        File assets = temp.newFolder("assets"), source = source(assets, "source", false);
        sticker(source, "1_a.webp", 1);
        SelectedStickers.Result first = add(assets, "source", "1_a.webp", false);
        File target = new File(assets, first.identifier);
        for (File file : target.listFiles()) if (SelectedStickers.isFiller(file)) assertTrue(file.delete());
        SelectedStickers.Result repaired = add(assets, "source", "1_a.webp", false);
        assertTrue(repaired.duplicate); assertEquals(3, repaired.count); assertEquals(1, repaired.selectedCount);
        assertEquals(2, repaired.fillerCount);
    }

    @Test public void explicitlyChoosingAnInitialPromotesItAndDoesNotDeduplicateAgainstFiller() throws Exception {
        File assets = temp.newFolder("assets"), source = source(assets, "source", false);
        sticker(source, "1_a.webp", 201);
        SelectedStickers.Result first = add(assets, "source", "1_a.webp", false);
        File target = new File(assets, first.identifier);
        File filler = target.listFiles((d, n) -> n.startsWith("00_selected-filler_"))[0];
        SelectedStickers.Result second = add(assets, first.identifier, filler.getName(), false);
        assertFalse(second.duplicate); assertEquals(2, second.selectedCount); assertEquals(1, second.fillerCount);
        File remaining = target.listFiles((d, n) -> n.startsWith("00_selected-filler_"))[0];
        SelectedStickers.Result third = add(assets, first.identifier, remaining.getName(), false);
        assertEquals(3, third.selectedCount); assertEquals(0, third.fillerCount); assertEquals(3, third.count);
    }

    @Test public void templateFailureRollsBackAdditionAndKeepsPreviousFillers() throws Exception {
        File assets = temp.newFolder("assets"), source = source(assets, "source", false);
        sticker(source, "1_a.webp", 1); sticker(source, "2_a.webp", 2);
        SelectedStickers.Result first = add(assets, "source", "1_a.webp", false);
        SelectedStickers.Templates missing = new SelectedStickers.Templates() {
            public String[] names(boolean animated) { return new String[0]; }
            public java.io.InputStream open(boolean animated, String name) { return null; }
        };
        expectFailure(() -> SelectedStickers.add(assets, "source", "2_a.webp", false, missing));
        File target = new File(assets, first.identifier);
        assertEquals(3, target.listFiles((d, n) -> n.endsWith(".webp")).length);
        assertTrue(new File(target, "v_1.txt").isFile());
        assertEquals(2, assets.list().length);
    }

    @Test public void modifiedFillerIsPreservedAsARealChoice() throws Exception {
        File assets = temp.newFolder("assets"), source = source(assets, "source", false);
        sticker(source, "1_a.webp", 1); sticker(source, "2_a.webp", 2);
        SelectedStickers.Result first = add(assets, "source", "1_a.webp", false);
        File target = new File(assets, first.identifier);
        File changed = target.listFiles((d, n) -> n.startsWith("00_selected-filler_"))[0];
        Files.write(changed.toPath(), new byte[]{99});
        SelectedStickers.Result second = add(assets, "source", "2_a.webp", false);
        assertEquals(3, second.selectedCount); assertEquals(0, second.fillerCount);
        assertArrayEquals(new byte[]{99}, Files.readAllBytes(changed.toPath()));
    }

    private SelectedStickers.Result add(File assets, String source, String name, boolean animated) throws IOException {
        return SelectedStickers.add(assets, source, name, animated, templates);
    }

    private interface Operation { void run() throws IOException; }
    private void expectFailure(Operation action) throws Exception {
        try { action.run(); fail("Expected rejection"); } catch (IOException expected) { assertNotNull(expected.getMessage()); }
    }
}
