package com.example.samplestickerapp;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;

public class MoveStickersSheet extends BottomSheetDialogFragment {
    static final String TAG = "move_stickers_sheet";
    static final String RESULT = "stickers_moved";
    private MoveStickersViewModel model;

    static MoveStickersSheet create(StickerPack source, ArrayList<String> files) {
        MoveStickersSheet sheet = new MoveStickersSheet();
        Bundle args = new Bundle();
        args.putParcelable("source", source);
        args.putStringArrayList("files", files);
        sheet.setArguments(args);
        return sheet;
    }

    @Override public void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        model = new ViewModelProvider(this).get(MoveStickersViewModel.class);
        model.initialize(requireArguments().getParcelable("source"), requireArguments().getStringArrayList("files"));
    }

    @NonNull @Override public Dialog onCreateDialog(@Nullable Bundle state) {
        BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(state);
        dialog.getBehavior().setMaxWidth(getResources().getDisplayMetrics().widthPixels);
        dialog.getBehavior().setSkipCollapsed(true);
        return dialog;
    }

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                                                @Nullable Bundle state) {
        return inflater.inflate(R.layout.sheet_move_stickers, container, false);
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        ((TextView) view.findViewById(R.id.move_selection_count)).setText(getString(R.string.move_selected_count, model.files.size()));
        RecyclerView list = view.findViewById(R.id.move_pack_list);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        MoveStickerPacksAdapter adapter = new MoveStickerPacksAdapter(model);
        list.setAdapter(adapter);
        Button confirm = view.findViewById(R.id.move_confirm);
        confirm.setText(getString(R.string.move_confirm, model.files.size()));
        confirm.setOnClickListener(v -> model.move());
        model.changes.observe(getViewLifecycleOwner(), ignored -> {
            setCancelable(!model.moving);
            ((BottomSheetDialog) requireDialog()).getBehavior().setDraggable(!model.moving);
            confirm.setEnabled(!model.loading && !model.moving && !model.completed && model.selected != null);
            view.findViewById(R.id.move_loading).setVisibility(model.loading || model.moving ? View.VISIBLE : View.GONE);
            TextView message = view.findViewById(R.id.move_message);
            String text = model.error != null ? model.error : model.moving ? getString(R.string.move_running)
                    : model.loading ? getString(R.string.move_loading)
                    : model.destinations.isEmpty() ? getString(R.string.move_empty) : "";
            message.setText(text);
            message.setVisibility(text.isEmpty() ? View.GONE : View.VISIBLE);
            adapter.notifyDataSetChanged();
            if (model.completed) {
                Bundle result = new Bundle();
                result.putParcelable("source", model.updatedSource);
                result.putString("destination", model.selected.name);
                result.putString("error", model.error);
                getParentFragmentManager().setFragmentResult(RESULT, result);
                dismiss();
            }
        });
    }

    @Override public void onStart() {
        super.onStart();
        BottomSheetDialog dialog = (BottomSheetDialog) requireDialog();
        View sheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (sheet != null) {
            ViewGroup.LayoutParams params = sheet.getLayoutParams();
            params.height = Math.round(getResources().getDisplayMetrics().heightPixels * .75f);
            sheet.setLayoutParams(params);
        }
        dialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
    }
}
