package com.example.samplestickerapp;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class FileExplorerActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProgressBar progressBar;

    private StickerPack stickerPack;
    private File file;
    boolean videosRecentes;
    private MenuItem deleteMenuItem;
    private String tituloToolbar;
    private FileAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_explorer);

        tituloToolbar = getIntent().getStringExtra("titulo");
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(tituloToolbar);
        }

        stickerPack = getIntent().getParcelableExtra("sticker_pack");
        videosRecentes = getIntent().getBooleanExtra("videos_recentes", false);

        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FileAdapter(new ArrayList<>(), videosRecentes, this::onItemClick, getOnSelectionListener());
        recyclerView.setAdapter(adapter);

        loadDirectory();
    }

    private FileAdapter.OnSelectionListener getOnSelectionListener() {
        return new FileAdapter.OnSelectionListener() {
            @Override
            public void onSelectionModeChanged(boolean active) {
                if (deleteMenuItem != null) {
                    deleteMenuItem.setVisible(active);
                }
            }

            @Override
            public void onSelectionCountChanged(int count) {
                if (deleteMenuItem != null) {
                    if (count == 0) {
                        getSupportActionBar().setTitle(tituloToolbar);
                    } else {
                        getSupportActionBar().setTitle(String.format("Excluir (%d)", count));
                        deleteMenuItem.setTitle(String.format("Excluir (%d)", count));
                    }
                }
            }
        };
    }

    private void loadDirectory() {
        progressBar.setVisibility(View.VISIBLE);

        File[] children;
        if (videosRecentes) {
            RecentFileDao dao = new RecentFileDao(this);
            List<String> recents = dao.buscarTodos();

            children = recents.stream()
                    .map(File::new)
                    .toArray(File[]::new);
        } else {
            children = FilesHelper.getMp4Dir().listFiles();
            if (children == null) children = new File[0];
        }

        List<File> list = Arrays.stream(children)
                .sorted((a, b) -> {
                    if (a.isDirectory() && !b.isDirectory()) return -1;
                    if (!a.isDirectory() && b.isDirectory()) return 1;
                    return a.getName().compareToIgnoreCase(b.getName());
                })
                .collect(Collectors.toList());

        ((FileAdapter) recyclerView.getAdapter()).updateList(list);

        progressBar.setVisibility(View.GONE);
    }

    private void onItemClick(File file) {
        String name = file.getName().toLowerCase();

        // TODO: quando .webp avaliar se é estatico ou nao para saber qual tela abrir
        List<String> extensaoVideos = Arrays.asList(
                ".mp4", ".webm", ".mkv", ".avi", ".mov", ".flv", ".wmv", ".3gp", ".ts", ".gif", ".webp");
        if (extensaoVideos.stream().anyMatch(name::endsWith)) {
            this.file = file;
            FileExplorerHelper fileExplorerHelper = new FileExplorerHelper(this, stickerPack);
            AlertDialogHelper.showAlertDialog(this, "",
                    "O que deseja fazer?",
                    "Aparar(Trim)", "Cortar(Crop)",
                    () -> {
                        fileExplorerHelper.extracted(file, CustomVideoRangeActivity.class);
                    }, () -> {
                        fileExplorerHelper.extracted(file, CropVideoActivity.class);
                    });
            return;
        }

        List<String> extensaoImagens = Arrays.asList(
                ".jpg", ".jpeg", ".png", ".bmp", ".svg");
        if (extensaoImagens.stream().anyMatch(name::endsWith)) {
            this.file = file;
            Intent intent = new Intent(this, CropImageActivity.class);
            intent.putExtra("sticker_pack", stickerPack);
            intent.putExtra("file_path", file.getAbsolutePath());
            startActivity(intent);
            return;
        }

        Toast.makeText(this, "Não é possível criar uma figurinha a partir desse arquivo", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!ContentsJsonHelper.stickersAlterados.isEmpty()) {
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_files_activity, menu);
        // inicialmente oculto
        deleteMenuItem = menu.findItem(R.id.action_delete);
        deleteMenuItem.setVisible(false);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }

        if (item.getItemId() == R.id.action_delete && stickerPack != null) {
            adapter.handleDelete(this);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
