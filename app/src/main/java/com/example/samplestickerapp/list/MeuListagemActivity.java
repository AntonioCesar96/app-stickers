/*
 * Copyright (c) WhatsApp Inc. and its affiliates.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.example.samplestickerapp.list;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.samplestickerapp.BaseActivity;
import com.example.samplestickerapp.R;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MeuListagemActivity extends BaseActivity {
    public static final String EXTRA_STICKER_PACK_LIST_DATA = "sticker_pack_listagem";
    private static final int STICKER_PREVIEW_DISPLAY_LIMIT = 6;
    private LinearLayoutManager packLayoutManager;
    private RecyclerView packRecyclerView;
    private MeuStickerPackListAdapter allStickerPacksListAdapter;
    private MeuListagemActivity.WhiteListCheckAsyncTask whiteListCheckAsyncTask;
    private ArrayList<MeuStickerPackModel> stickerPackList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.meu_activity_sticker_pack_list);
        packRecyclerView = findViewById(R.id.sticker_pack_listagem_meu);

        stickerPackList = MeuContentsJsonHelper.getStickerPacks();
        showStickerPackList(stickerPackList);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Pastas e Figurinhas");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        whiteListCheckAsyncTask = new MeuListagemActivity.WhiteListCheckAsyncTask(this);
        whiteListCheckAsyncTask.execute(stickerPackList.toArray(new MeuStickerPackModel[0]));
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (whiteListCheckAsyncTask != null && !whiteListCheckAsyncTask.isCancelled()) {
            whiteListCheckAsyncTask.cancel(true);
        }
    }

    private void showStickerPackList(List<MeuStickerPackModel> stickerPackList) {
        allStickerPacksListAdapter = new MeuStickerPackListAdapter(stickerPackList);
        packRecyclerView.setAdapter(allStickerPacksListAdapter);
        packLayoutManager = new LinearLayoutManager(this);
        packLayoutManager.setOrientation(RecyclerView.VERTICAL);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(
                packRecyclerView.getContext(),
                packLayoutManager.getOrientation()
        );
        packRecyclerView.addItemDecoration(dividerItemDecoration);
        packRecyclerView.setLayoutManager(packLayoutManager);
        packRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(this::recalculateColumnCount);
    }

    private void recalculateColumnCount() {
        final int previewSize = getResources().getDimensionPixelSize(R.dimen.sticker_pack_list_item_preview_image_size);
        int firstVisibleItemPosition = packLayoutManager.findFirstVisibleItemPosition();
        MeuStickerPackListItemViewHolder viewHolder = (MeuStickerPackListItemViewHolder) packRecyclerView.findViewHolderForAdapterPosition(firstVisibleItemPosition);
        if (viewHolder != null) {
            final int widthOfImageRow = viewHolder.imageRowView.getMeasuredWidth();
            final int max = Math.max(widthOfImageRow / previewSize, 1);
            int maxNumberOfImagesInARow = Math.min(STICKER_PREVIEW_DISPLAY_LIMIT, max);
            int minMarginBetweenImages = (widthOfImageRow - maxNumberOfImagesInARow * previewSize) / (maxNumberOfImagesInARow - 1);
            allStickerPacksListAdapter.setImageRowSpec(maxNumberOfImagesInARow, minMarginBetweenImages);
        }
    }


    static class WhiteListCheckAsyncTask extends AsyncTask<MeuStickerPackModel, Void, List<MeuStickerPackModel>> {
        private final WeakReference<MeuListagemActivity> stickerPackListActivityWeakReference;

        WhiteListCheckAsyncTask(MeuListagemActivity stickerPackListActivity) {
            this.stickerPackListActivityWeakReference = new WeakReference<>(stickerPackListActivity);
        }

        @Override
        protected final List<MeuStickerPackModel> doInBackground(MeuStickerPackModel... stickerPackArray) {
            final MeuListagemActivity stickerPackListActivity = stickerPackListActivityWeakReference.get();
            if (stickerPackListActivity == null) {
                return Arrays.asList(stickerPackArray);
            }
            return Arrays.asList(stickerPackArray);
        }

        @Override
        protected void onPostExecute(List<MeuStickerPackModel> stickerPackList) {
            final MeuListagemActivity stickerPackListActivity = stickerPackListActivityWeakReference.get();
            if (stickerPackListActivity != null) {
                stickerPackListActivity.allStickerPacksListAdapter.setStickerPackList(stickerPackList);
                stickerPackListActivity.allStickerPacksListAdapter.notifyDataSetChanged();
            }
        }
    }
}