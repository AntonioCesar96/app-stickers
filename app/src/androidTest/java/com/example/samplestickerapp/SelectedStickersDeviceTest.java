package com.example.samplestickerapp;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.view.View;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import java.io.File;
import java.io.FileOutputStream;
import java.util.UUID;
import static org.junit.Assert.*;

public class SelectedStickersDeviceTest {
    @Test public void previewShowsSelectionButtonBelowStickerAndImageDismissesIt() throws Exception {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Context context = instrumentation.getTargetContext();
        StickerPack pack = StickerPackLoader.fetchStickerPacks(context).get(0);
        StickerPackDetailsActivity screen = (StickerPackDetailsActivity) instrumentation.startActivitySync(
                new Intent(context, StickerPackDetailsActivity.class).putExtra("sticker_pack", pack)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        try {
            instrumentation.waitForIdleSync();
            Thread.sleep(500);
            instrumentation.runOnMainSync(() -> {
                RecyclerView list = screen.findViewById(R.id.sticker_list);
                RecyclerView.ViewHolder first = list.findViewHolderForAdapterPosition(0);
                assertNotNull(first);
                ((StickerPreviewViewHolder) first).stickerPreviewView.performClick();
                View panel = screen.findViewById(R.id.selected_sticker_preview);
                View sticker = screen.findViewById(R.id.sticker_details_expanded_sticker);
                View button = screen.findViewById(R.id.add_selected_sticker);
                assertEquals(View.VISIBLE, panel.getVisibility());
                assertTrue(button.isShown());
                assertTrue(button.getTop() >= sticker.getBottom());
                assertTrue(panel.getX() >= 0 && panel.getY() >= 0);
                assertTrue(panel.getX() + panel.getWidth() <= list.getWidth() + 1);
                assertTrue(panel.getY() + panel.getHeight() <= list.getHeight() + 1);
            });
            Thread.sleep(1200); // Fresco loads the expanded animation asynchronously.
            Bitmap screenshot = instrumentation.getUiAutomation().takeScreenshot();
            try (FileOutputStream out = new FileOutputStream(new File(context.getCacheDir(), "selected-sticker-preview.png"))) {
                assertTrue(screenshot.compress(Bitmap.CompressFormat.PNG, 100, out));
            } finally { screenshot.recycle(); }
            instrumentation.runOnMainSync(() -> {
                screen.findViewById(R.id.sticker_details_expanded_sticker).performClick();
                assertEquals(View.INVISIBLE, screen.findViewById(R.id.selected_sticker_preview).getVisibility());
            });
        } finally { instrumentation.runOnMainSync(screen::finish); }
    }

    @Test public void collectionCopiesRealWebpIntoIsolatedStorageWithoutChangingSource() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        File workspace = new File(context.getCacheDir(), "selected-test-" + UUID.randomUUID());
        File assets = new File(workspace, "assets"), source = new File(assets, "source");
        assertTrue(source.mkdirs());
        Bitmap image = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888);
        image.eraseColor(android.graphics.Color.GREEN);
        try {
            try (FileOutputStream out = new FileOutputStream(new File(source, "1_test.webp"))) {
                assertTrue(image.compress(Bitmap.CompressFormat.WEBP, 80, out));
            }
            try (FileOutputStream out = new FileOutputStream(new File(source, "icone.png"))) {
                Bitmap icon = Bitmap.createScaledBitmap(image, 96, 96, true);
                try { assertTrue(icon.compress(Bitmap.CompressFormat.PNG, 100, out)); } finally { icon.recycle(); }
            }
            SelectedStickers.Result first = SelectedStickers.add(assets, "source", "1_test.webp", false);
            SelectedStickers.Result duplicate = SelectedStickers.add(assets, "source", "1_test.webp", false);
            assertEquals(1, first.count); assertTrue(duplicate.duplicate);
            assertTrue(new File(source, "1_test.webp").isFile());
            File collection = new File(assets, first.identifier);
            assertTrue(SelectedStickers.isCollection(collection));
            File copied = collection.listFiles((d, n) -> n.endsWith(".webp"))[0];
            assertEquals(512, android.graphics.BitmapFactory.decodeFile(copied.getAbsolutePath()).getWidth());
        } finally { image.recycle(); ContentsJsonHelper.deleteRecursive(workspace); }
    }
}
