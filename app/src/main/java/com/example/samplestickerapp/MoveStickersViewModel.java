package com.example.samplestickerapp;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MoveStickersViewModel extends AndroidViewModel {
    final MutableLiveData<Integer> changes = new MutableLiveData<>(0);
    final ArrayList<StickerPack> destinations = new ArrayList<>();
    final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    StickerPack source, selected, updatedSource;
    ArrayList<String> files;
    boolean initialized, loading = true, moving, completed;
    String error;

    @androidx.annotation.Keep
    public MoveStickersViewModel(@NonNull Application application) { super(application); }

    void initialize(StickerPack source, ArrayList<String> files) {
        if (initialized) return;
        initialized = true;
        this.source = source;
        this.files = new ArrayList<>(files);
        worker.execute(() -> {
            ArrayList<StickerPack> compatible = new ArrayList<>();
            String problem = null;
            try {
                for (StickerPack pack : StickerPackLoader.fetchStickerPacks(getApplication())) {
                    if (StickerMove.compatible(source.identifier, source.animatedStickerPack, pack.identifier,
                            pack.animatedStickerPack, pack.getStickers().size(), files.size())) compatible.add(pack);
                }
            } catch (Exception e) { problem = "Não foi possível carregar os pacotes: " + e.getMessage(); }
            String message = problem;
            main.post(() -> {
                destinations.addAll(compatible);
                error = message;
                loading = false;
                changed();
            });
        });
    }

    void select(StickerPack pack) {
        if (moving) return;
        selected = pack;
        changed();
    }

    void move() {
        if (moving || selected == null || completed) return;
        moving = true;
        error = null;
        changed();
        StickerPack destination = selected;
        worker.execute(() -> {
            StickerPack updated = null;
            String problem = null;
            boolean moved = false;
            try {
                // Recheck disk counts and source files at execution time, not just when showing the sheet.
                StickerMove.move(FilesHelper.getAssetsDir(), source.identifier, destination.identifier, files,
                        source.animatedStickerPack, destination.animatedStickerPack);
                moved = true;
                ContentsJsonHelper.atualizaContentsJson();
                ContentsJsonHelper.atualizaContentProvider(getApplication());
                for (StickerPack pack : StickerPackLoader.fetchStickerPacks(getApplication())) {
                    if (pack.identifier.equals(source.identifier)) updated = pack;
                }
                if (updated == null) throw new IllegalStateException("O pacote de origem não foi encontrado.");
            } catch (Exception e) {
                android.util.Log.e("StickerMove", "Falha ao mover figurinhas", e);
                problem = moved ? "Os arquivos foram movidos, mas não foi possível atualizar a lista. Reabra o app."
                        : e.getMessage();
            }
            StickerPack result = updated;
            String message = problem;
            boolean filesMoved = moved;
            main.post(() -> {
                ContentsJsonHelper.packsChanged |= filesMoved;
                moving = false;
                error = message;
                updatedSource = result;
                completed = filesMoved; // Never repeat a batch whose files were already moved.
                changed();
            });
        });
    }

    private void changed() { changes.setValue(changes.getValue() + 1); }

    @Override protected void onCleared() {
        // Finish an already submitted short disk operation, even if the UI is closed.
        worker.shutdown();
        super.onCleared();
    }
}
