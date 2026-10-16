package com.example.samplestickerapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Entry point for one image/video stream shared by another Android app. */
public class ShareMediaActivity extends AppCompatActivity {
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private SharedMediaImport media;
    private boolean loadingPacks;
    private boolean editorOpened;
    private String selectedPack;
    private Spinner packPicker;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_share_media);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Mídia compartilhada");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        packPicker = findViewById(R.id.shared_pack);
        findViewById(R.id.shared_trim).setOnClickListener(v -> openEditor(true));
        findViewById(R.id.shared_crop).setOnClickListener(v -> openEditor(false));
        findViewById(R.id.shared_manage_packs).setOnClickListener(v -> {
            rememberPack();
            if (!PermissionHelper.hasManageExternalStoragePermission(this))
                PermissionHelper.requestManageExternalStoragePermission(this);
            else startActivity(new Intent(this, SplashActivity.class));
        });
        setActions(false);
        if (state != null) {
            selectedPack = state.getString("pack");
            String path = state.getString("importedPath");
            if (path != null && new File(path).isFile()) {
                media = new SharedMediaImport(new File(path), state.getString("kind"), state.getString("name"));
                showMedia(); return;
            }
        }
        Intent incoming = new Intent(getIntent());
        worker.execute(() -> {
            try {
                SharedMediaImport imported = SharedMediaImport.receive(getApplicationContext(), incoming);
                runOnUiThread(() -> {
                    if (isDestroyed() || isFinishing()) { imported.file.delete(); return; }
                    media = imported; showMedia();
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    if (isDestroyed() || isFinishing()) return;
                    findViewById(R.id.shared_loading).setVisibility(View.GONE);
                    ((TextView) findViewById(R.id.shared_status)).setText("Não foi possível receber o arquivo. Compartilhe a foto ou o vídeo novamente pelo aplicativo de origem.");
                });
            }
        });
    }

    private void setActions(boolean enabled) {
        findViewById(R.id.shared_trim).setEnabled(enabled);
        findViewById(R.id.shared_crop).setEnabled(enabled);
        packPicker.setEnabled(enabled);
    }

    private void showMedia() {
        ((TextView) findViewById(R.id.shared_file_name)).setText(media.name);
        boolean image = "image".equals(media.kind);
        findViewById(R.id.shared_trim).setVisibility(image ? View.GONE : View.VISIBLE);
        ((android.widget.Button) findViewById(R.id.shared_crop)).setText(image ? "Cortar foto" : "Cortar enquadramento");
        refreshPacks();
    }

    private void refreshPacks() {
        if (media == null || loadingPacks) return;
        rememberPack(); setActions(false);
        if (!PermissionHelper.hasManageExternalStoragePermission(this)) {
            findViewById(R.id.shared_loading).setVisibility(View.GONE);
            ((TextView) findViewById(R.id.shared_status)).setText("Permita o acesso aos pacotes para escolher onde salvar sua figurinha.");
            ((android.widget.Button) findViewById(R.id.shared_manage_packs)).setText("Permitir acesso aos pacotes");
            return;
        }
        loadingPacks = true;
        findViewById(R.id.shared_loading).setVisibility(View.VISIBLE);
        worker.execute(() -> {
            ArrayList<StickerPack> compatible = new ArrayList<>();
            try {
                for (StickerPack pack : StickerPackLoader.fetchStickerPacks(this))
                    if (pack.animatedStickerPack == !"image".equals(media.kind) && pack.getStickers().size() < 30) compatible.add(pack);
            } catch (Exception ignored) { /* First use may require initializing the app's catalog. */ }
            runOnUiThread(() -> {
                if (isDestroyed() || isFinishing()) return;
                loadingPacks = false;
                findViewById(R.id.shared_loading).setVisibility(View.GONE);
                packPicker.setAdapter(new ContatoSpinnerAdapter(this, compatible));
                for (int i = 0; i < compatible.size(); i++) if (compatible.get(i).identifier.equals(selectedPack)) packPicker.setSelection(i);
                setActions(!compatible.isEmpty());
                ((TextView) findViewById(R.id.shared_status)).setText(compatible.isEmpty()
                        ? "Crie um pacote " + ("image".equals(media.kind) ? "estático" : "animado") + " com espaço disponível. Depois volte a esta tela."
                        : "image".equals(media.kind) ? "Escolha o pacote e ajuste o corte da foto."
                        : "Escolha o pacote. Aparar seleciona a duração; cortar ajusta a área da figurinha.");
                ((android.widget.Button) findViewById(R.id.shared_manage_packs)).setText("Criar ou gerenciar pacotes");
            });
        });
    }

    private void openEditor(boolean trim) {
        StickerPack pack = (StickerPack) packPicker.getSelectedItem();
        if (media == null || pack == null || !media.file.isFile()) return;
        selectedPack = pack.identifier;
        editorOpened = true;
        if ("image".equals(media.kind)) {
            startActivity(new Intent(this, CropImageActivity.class).putExtra("file_path", media.file.getAbsolutePath()).putExtra("sticker_pack", pack));
        } else {
            new FileExplorerHelper(this, pack).extracted(media.file, trim ? CustomVideoRangeActivity.class : CropVideoActivity.class);
        }
    }

    private void rememberPack() {
        Object selected = packPicker.getSelectedItem();
        if (selected instanceof StickerPack) selectedPack = ((StickerPack) selected).identifier;
    }

    @Override protected void onResume() { super.onResume(); editorOpened = false; if (media != null) refreshPacks(); }
    @Override protected void onSaveInstanceState(Bundle state) {
        rememberPack(); state.putString("pack", selectedPack);
        if (media != null) {
            state.putString("importedPath", media.file.getAbsolutePath()); state.putString("kind", media.kind); state.putString("name", media.name);
        }
        super.onSaveInstanceState(state);
    }
    @Override public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }
    @Override protected void onDestroy() {
        worker.shutdownNow();
        if (isFinishing() && !editorOpened && media != null) {
            CompressionJob pending = CompressionJob.pending(this);
            boolean sourceNeeded = pending != null && (media.file.getAbsolutePath().equals(pending.data.optString("preview"))
                    || media.file.getAbsolutePath().equals(pending.data.optString("source")));
            if (!sourceNeeded) media.file.delete();
        }
        super.onDestroy();
    }
}
