package com.example.samplestickerapp;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;

public class StickerMoveTest {
    @Rule public TemporaryFolder temp = new TemporaryFolder();

    private File pack(String name, int count) throws IOException {
        File directory = temp.newFolder(name);
        for (int i = 1; i <= count; i++) Files.write(new File(directory, i + "_same.webp").toPath(), new byte[]{(byte) i});
        return directory;
    }

    @Test public void movesMultipleOriginalsWithoutOverwritingExistingNames() throws Exception {
        File source = pack("source", 5), target = pack("target", 3);
        StickerMove.move(temp.getRoot(), "source", "target", Arrays.asList("1_same.webp", "2_same.webp"), false, false);
        assertFalse(new File(source, "1_same.webp").exists());
        assertFalse(new File(source, "2_same.webp").exists());
        assertArrayEquals(new byte[]{1}, Files.readAllBytes(new File(target, "1_same.webp").toPath()));
        File[] appended = target.listFiles((dir, name) -> name.startsWith("04_") || name.startsWith("05_"));
        assertEquals(2, appended.length);
        Arrays.sort(appended);
        assertArrayEquals(new byte[]{1}, Files.readAllBytes(appended[0].toPath()));
        assertArrayEquals(new byte[]{2}, Files.readAllBytes(appended[1].toPath()));
        assertTrue(new File(source, "v_1.txt").exists());
        assertTrue(new File(target, "v_1.txt").exists());
    }

    @Test public void compatibilityRequiresAnotherPackSameTypeAndSpaceForEntireSelection() {
        assertTrue(StickerMove.compatible("a", true, "b", true, 28, 2));
        assertFalse(StickerMove.compatible("a", true, "a", true, 3, 1));
        assertFalse(StickerMove.compatible("a", true, "b", false, 3, 1));
        assertFalse(StickerMove.compatible("a", true, "b", true, 29, 2));
        assertFalse(StickerMove.compatible("a", true, "b", true, 3, 0));
    }

    @Test public void minimumAndCapacityAreRecheckedBeforeTouchingFiles() throws Exception {
        File source = pack("source", 4), target = pack("target", 30);
        expectFailure(() -> StickerMove.move(temp.getRoot(), "source", "target", Collections.singletonList("1_same.webp"), false, false));
        assertTrue(new File(source, "1_same.webp").exists());
        File other = pack("other", 3);
        expectFailure(() -> StickerMove.move(temp.getRoot(), "source", "other", Arrays.asList("1_same.webp", "2_same.webp"), false, false));
        assertTrue(new File(source, "1_same.webp").exists());
        assertEquals(3, other.list().length);
        assertEquals(30, target.list().length);
    }

    @Test public void missingInputAndPathsOutsidePackCannotPartiallyMoveSelection() throws Exception {
        File source = pack("source", 5), target = pack("target", 3);
        expectFailure(() -> StickerMove.move(temp.getRoot(), "source", "target", Arrays.asList("1_same.webp", "missing.webp"), false, false));
        expectFailure(() -> StickerMove.move(temp.getRoot(), "source", "target", Collections.singletonList("../target/1_same.webp"), false, false));
        assertTrue(new File(source, "1_same.webp").exists());
        assertEquals(3, target.list().length);
    }

    @Test public void rollsBackAllFilesIfUpdatingTargetVersionFails() throws Exception {
        File source = pack("source", 5), target = pack("target", 3);
        assertTrue(new File(target, "v_2147483647.txt").createNewFile());
        expectFailure(() -> StickerMove.move(temp.getRoot(), "source", "target", Arrays.asList("1_same.webp", "2_same.webp"), false, false));
        assertArrayEquals(new byte[]{1}, Files.readAllBytes(new File(source, "1_same.webp").toPath()));
        assertArrayEquals(new byte[]{2}, Files.readAllBytes(new File(source, "2_same.webp").toPath()));
        assertEquals(4, target.list().length);
    }

    private interface Operation { void run() throws IOException; }
    private void expectFailure(Operation operation) throws Exception {
        try { operation.run(); fail("Expected move rejection"); }
        catch (IOException expected) { assertNotNull(expected.getMessage()); }
    }
}
