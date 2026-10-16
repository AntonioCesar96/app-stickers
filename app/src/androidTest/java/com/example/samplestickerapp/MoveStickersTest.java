package com.example.samplestickerapp;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class MoveStickersTest {
    private final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
    private final Context context = instrumentation.getTargetContext();
    private final ArrayList<File> fixtures = new ArrayList<>();

    private File pack(String name, int count, boolean animated) throws Exception {
        File directory = new File(FilesHelper.getAssetsDir(), name);
        assertFalse(directory.exists());
        assertTrue(directory.mkdir());
        fixtures.add(directory);
        copyAsset("Pacote 2/icone.png", new File(directory, "icone.png"));
        assertTrue(new File(directory, "v_1.txt").createNewFile());
        if (animated) assertTrue(new File(directory, "animado.txt").createNewFile());
        for (int i = 1; i <= count; i++) copyAsset(animated ? "Pacote 1/03_Heart.webp"
                : "Pacote 2/01_Cuppy_smile.webp", new File(directory, String.format(java.util.Locale.US, "%02d_test.webp", i)));
        return directory;
    }

    private void copyAsset(String path, File target) throws Exception {
        try (InputStream input = context.getAssets().open(path); FileOutputStream output = new FileOutputStream(target)) {
            byte[] buffer = new byte[8192]; int n;
            while ((n = input.read(buffer)) != -1) output.write(buffer, 0, n);
        }
    }

    private StickerPack loaded(File directory) {
        for (StickerPack pack : StickerPackLoader.fetchStickerPacks(context)) {
            if (pack.identifier.equals(directory.getName())) return pack;
        }
        throw new AssertionError("Pack not loaded");
    }

    private void waitFor(Check check) throws Exception {
        long deadline = System.currentTimeMillis() + 15000;
        AtomicBoolean ready = new AtomicBoolean();
        while (!ready.get() && System.currentTimeMillis() < deadline) {
            instrumentation.runOnMainSync(() -> ready.set(check.ready()));
            Thread.sleep(50);
        }
        assertTrue("Timed out waiting for move UI", ready.get());
    }
    private interface Check { boolean ready(); }

    private MoveStickersSheet open(StickerPackDetailsActivity screen, int number) throws Exception {
        waitFor(() -> {
            RecyclerView list = screen.findViewById(R.id.sticker_list);
            return list.findViewHolderForAdapterPosition(number - 1) != null;
        });
        instrumentation.runOnMainSync(() -> {
            RecyclerView list = screen.findViewById(R.id.sticker_list);
            list.findViewHolderForAdapterPosition(0).itemView.findViewById(R.id.sticker_preview).performLongClick();
            if (number > 1) list.findViewHolderForAdapterPosition(1).itemView.findViewById(R.id.sticker_preview).performClick();
        });
        waitFor(() -> screen.findViewById(R.id.action_move) != null && screen.findViewById(R.id.action_move).isShown());
        instrumentation.runOnMainSync(() -> {
            screen.findViewById(R.id.action_move).performClick();
            screen.getSupportFragmentManager().executePendingTransactions();
        });
        AtomicReference<MoveStickersSheet> sheet = new AtomicReference<>();
        waitFor(() -> {
            MoveStickersSheet current = (MoveStickersSheet) screen.getSupportFragmentManager().findFragmentByTag(MoveStickersSheet.TAG);
            if (current == null || current.getView() == null) return false;
            MoveStickersViewModel model = new ViewModelProvider(current).get(MoveStickersViewModel.class);
            sheet.set(current);
            return !model.loading;
        });
        return sheet.get();
    }

    private void choose(MoveStickersSheet sheet, File destination) throws Exception {
        AtomicReference<Integer> position = new AtomicReference<>();
        instrumentation.runOnMainSync(() -> {
            MoveStickersViewModel model = new ViewModelProvider(sheet).get(MoveStickersViewModel.class);
            for (int i = 0; i < model.destinations.size(); i++) {
                if (model.destinations.get(i).identifier.equals(destination.getName())) position.set(i);
            }
            assertNotNull(position.get());
            ((RecyclerView) sheet.requireView().findViewById(R.id.move_pack_list)).scrollToPosition(position.get());
        });
        waitFor(() -> ((RecyclerView) sheet.requireView().findViewById(R.id.move_pack_list))
                .findViewHolderForAdapterPosition(position.get()) != null);
        instrumentation.runOnMainSync(() -> {
            RecyclerView list = sheet.requireView().findViewById(R.id.move_pack_list);
            View row = list.findViewHolderForAdapterPosition(position.get()).itemView;
            assertEquals(destination.getName(), ((TextView) row.findViewById(R.id.move_pack_name)).getText().toString());
            assertTrue(((LinearLayout) row.findViewById(R.id.move_pack_previews)).getChildCount() > 0);
            row.performClick();
            assertTrue(sheet.requireView().findViewById(R.id.move_confirm).isEnabled());
        });
    }

    @Test public void selectionSheetFiltersRotatesAndMovesMultipleThenOneWithoutChangingBytes() throws Exception {
        FilesHelper.setContext(context);
        assertNull("Do not disturb an unfinished user compression", CompressionJob.pending(context));
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        File source = pack("TesteMoverOrigem" + suffix, 6, false);
        File destination = pack("TesteMoverDestino" + suffix, 3, false);
        File almostFull = pack("TesteMoverCheio" + suffix, 29, false);
        File animated = pack("TesteMoverAnimado" + suffix, 3, true);
        ContentsJsonHelper.atualizaContentsJsonAndContentProvider(context);
        byte[] original = read(new File(source, "01_test.webp"));
        Intent input = new Intent(context, StickerPackDetailsActivity.class).putExtra("sticker_pack", loaded(source));
        try (ActivityScenario<StickerPackDetailsActivity> scenario = ActivityScenario.launch(input)) {
            AtomicReference<StickerPackDetailsActivity> activity = new AtomicReference<>();
            scenario.onActivity(activity::set);
            MoveStickersSheet sheet = open(activity.get(), 2);
            MoveStickersSheet initial = sheet;
            instrumentation.runOnMainSync(() -> {
                MoveStickersViewModel model = new ViewModelProvider(initial).get(MoveStickersViewModel.class);
                for (StickerPack pack : model.destinations) {
                    assertNotEquals(source.getName(), pack.identifier);
                    assertNotEquals(almostFull.getName(), pack.identifier);
                    assertNotEquals(animated.getName(), pack.identifier);
                }
                View bottom = initial.requireDialog().findViewById(com.google.android.material.R.id.design_bottom_sheet);
                assertEquals(1.0, bottom.getWidth() / (double) activity.get().getResources().getDisplayMetrics().widthPixels, .01);
                initial.dismiss();
            });
            assertEquals(6, loaded(source).getStickers().size());
            assertEquals(3, loaded(destination).getStickers().size());
            // Closing the sheet must keep the existing selection. Reopen without reselecting.
            instrumentation.runOnMainSync(() -> {
                StickerPackDetailsActivity screen = activity.get();
                RecyclerView grid = screen.findViewById(R.id.sticker_list);
                assertEquals(2, ((StickerPreviewAdapter) grid.getAdapter()).selectedFiles().size());
                screen.findViewById(R.id.action_move).performClick();
                screen.getSupportFragmentManager().executePendingTransactions();
            });
            AtomicReference<MoveStickersSheet> current = new AtomicReference<>();
            waitFor(() -> {
                MoveStickersSheet s = (MoveStickersSheet) activity.get().getSupportFragmentManager().findFragmentByTag(MoveStickersSheet.TAG);
                current.set(s);
                return s != null && s.getView() != null && !new ViewModelProvider(s).get(MoveStickersViewModel.class).loading;
            });
            choose(current.get(), destination);
            scenario.recreate();
            scenario.onActivity(activity::set);
            waitFor(() -> {
                MoveStickersSheet s = (MoveStickersSheet) activity.get().getSupportFragmentManager().findFragmentByTag(MoveStickersSheet.TAG);
                current.set(s);
                if (s == null || s.getView() == null) return false;
                View button = s.requireView().findViewById(R.id.move_confirm);
                android.graphics.Rect visible = new android.graphics.Rect();
                return button.isEnabled() && button.getGlobalVisibleRect(visible) && visible.height() >= button.getHeight();
            });
            instrumentation.runOnMainSync(() -> {
                MoveStickersViewModel model = new ViewModelProvider(current.get()).get(MoveStickersViewModel.class);
                assertEquals(destination.getName(), model.selected.identifier);
            });
            screenshot(current.get());
            instrumentation.runOnMainSync(() -> current.get().requireView().findViewById(R.id.move_confirm).performClick());
            waitFor(() -> activity.get().getSupportFragmentManager().findFragmentByTag(MoveStickersSheet.TAG) == null);
            assertEquals(4, loaded(source).getStickers().size());
            assertEquals(5, loaded(destination).getStickers().size());
            for (File file : destination.listFiles((dir, name) -> name.endsWith(".webp"))) assertArrayEquals(original, read(file));
            MoveStickersSheet single = open(activity.get(), 1);
            choose(single, almostFull);
            instrumentation.runOnMainSync(() -> single.requireView().findViewById(R.id.move_confirm).performClick());
            waitFor(() -> activity.get().getSupportFragmentManager().findFragmentByTag(MoveStickersSheet.TAG) == null);
            assertEquals(3, loaded(source).getStickers().size());
            assertEquals(30, loaded(almostFull).getStickers().size());
            assertEquals(5, loaded(destination).getStickers().size());
            instrumentation.runOnMainSync(() -> {
                RecyclerView grid = activity.get().findViewById(R.id.sticker_list);
                assertTrue(((StickerPreviewAdapter) grid.getAdapter()).selectedFiles().isEmpty());
                assertEquals("3 figurinhas", ((TextView) activity.get().findViewById(R.id.author)).getText().toString());
            });
        } finally {
            for (File directory : fixtures) {
                assertEquals(FilesHelper.getAssetsDir().getCanonicalFile(), directory.getCanonicalFile().getParentFile());
                for (File file : directory.listFiles()) assertTrue(file.delete());
                assertTrue(directory.delete());
            }
            ContentsJsonHelper.atualizaContentsJsonAndContentProvider(context);
            ContentsJsonHelper.packsChanged = false;
        }
    }

    private byte[] read(File file) throws Exception {
        try (InputStream input = new FileInputStream(file); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192]; int n;
            while ((n = input.read(buffer)) != -1) output.write(buffer, 0, n);
            return output.toByteArray();
        }
    }

    private void screenshot(MoveStickersSheet sheet) {
        instrumentation.waitForIdleSync();
        // Wait for the dialog's enter animation, which continues on the render thread after idle.
        try { Thread.sleep(700); } catch (InterruptedException e) { throw new AssertionError(e); }
        Bitmap bitmap = instrumentation.getUiAutomation().takeScreenshot();
        assertNotNull(bitmap);
        instrumentation.runOnMainSync(() -> {
            View button = sheet.requireView().findViewById(R.id.move_confirm);
            android.graphics.Rect rect = new android.graphics.Rect();
            assertTrue(button.getGlobalVisibleRect(rect));
            com.google.android.material.button.MaterialButton confirm = (com.google.android.material.button.MaterialButton) button;
            assertNotEquals("Confirm text must contrast with its background", confirm.getCurrentTextColor(),
                    confirm.getBackgroundTintList().getColorForState(confirm.getDrawableState(), 0));
            android.util.Log.i("MoveSheetTest", "Confirm button " + rect + "; screen height=" + bitmap.getHeight());
            android.util.Log.i("MoveSheetTest", "Confirm text=" + ((TextView) button).getText()
                    + "; color=" + Integer.toHexString(((TextView) button).getCurrentTextColor())
                    + "; alpha=" + button.getAlpha() + "; z=" + button.getZ()
                    + "; root height=" + sheet.requireView().getHeight()
                    + "; button y=" + button.getY());
            assertTrue("Confirm must be inside the physical screen", rect.bottom <= bitmap.getHeight());
        });
        try (FileOutputStream output = new FileOutputStream(new File(context.getExternalFilesDir(null), "move-sheet.png"))) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output);
        } catch (Exception e) { throw new AssertionError(e); }
        finally { bitmap.recycle(); }
    }
}
