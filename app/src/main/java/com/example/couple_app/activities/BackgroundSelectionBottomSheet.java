package com.example.couple_app.activities; // Sửa lại package cho đúng với project của bạn

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.couple_app.R; // Sửa lại R cho đúng
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;

public class BackgroundSelectionBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_DEFAULT_IDS = "default_ids";
    private RecyclerView rvBackgrounds;
    private BackgroundAdapter adapter;
    private BackgroundSelectionListener listener;

    // 1. Tạo hàm newInstance để nhận mảng ID
    public static BackgroundSelectionBottomSheet newInstance(int[] defaultIds) {
        BackgroundSelectionBottomSheet fragment = new BackgroundSelectionBottomSheet();
        Bundle args = new Bundle();
        args.putIntArray(ARG_DEFAULT_IDS, defaultIds);
        fragment.setArguments(args);
        return fragment;
    }

    // 2. Lấy listener (chính là HomeMain1Fragment)
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            // Lấy listener từ target fragment đã set trong HomeMain1Fragment
            listener = (BackgroundSelectionListener) getTargetFragment();
        } catch (ClassCastException e) {
            throw new ClassCastException("Calling fragment must implement BackgroundSelectionListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // 3. Inflate layout
        View view = inflater.inflate(R.layout.bottom_sheet_backgrounds, container, false);
        rvBackgrounds = view.findViewById(R.id.rvBackgrounds);

        // 4. Lấy mảng ID từ arguments
        List<Integer> backgroundList = new ArrayList<>();
        // Thêm một item "null" vào đầu danh sách để đại diện cho nút "Chọn từ thư viện"
        backgroundList.add(null);

        if (getArguments() != null) {
            int[] ids = getArguments().getIntArray(ARG_DEFAULT_IDS);
            if (ids != null) {
                for (int id : ids) {
                    backgroundList.add(id);
                }
            }
        }

        // 5. Cài đặt RecyclerView
        adapter = new BackgroundAdapter(getContext(), backgroundList, listener);
        rvBackgrounds.setLayoutManager(new GridLayoutManager(getContext(), 3));
        rvBackgrounds.setAdapter(adapter);

        return view;
    }

    // --- Bắt đầu Adapter nội bộ ---
    private static class BackgroundAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private static final int VIEW_TYPE_ADD = 0;
        private static final int VIEW_TYPE_ITEM = 1;

        private final Context context;
        private final List<Integer> backgroundIds;
        private final BackgroundSelectionListener adapterListener;

        public BackgroundAdapter(Context context, List<Integer> backgroundIds, BackgroundSelectionListener listener) {
            this.context = context;
            this.backgroundIds = backgroundIds;
            this.adapterListener = listener;
        }

        @Override
        public int getItemViewType(int position) {
            // Vị trí 0 là nút "Add", các vị trí khác là "Item"
            return (position == 0) ? VIEW_TYPE_ADD : VIEW_TYPE_ITEM;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(context);
            if (viewType == VIEW_TYPE_ADD) {
                View view = inflater.inflate(R.layout.item_background_add, parent, false);
                return new AddViewHolder(view);
            } else {
                View view = inflater.inflate(R.layout.item_background, parent, false);
                return new ItemViewHolder(view);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            if (holder.getItemViewType() == VIEW_TYPE_ADD) {
                // Đây là ViewHolder của nút "Add"
                AddViewHolder addHolder = (AddViewHolder) holder;
                addHolder.itemView.setOnClickListener(v -> {
                    if (adapterListener != null) {
                        adapterListener.onCustomSelectionRequested();
                    }
                });
            } else {
                // Đây là ViewHolder của ảnh mặc định
                ItemViewHolder itemHolder = (ItemViewHolder) holder;
                Integer resourceId = backgroundIds.get(position); // Lấy ID ảnh

                // Dùng Glide để tải ảnh (vì project của bạn đã có Glide)
                Glide.with(context)
                        .load(resourceId)
                        .centerCrop()
                        .into(itemHolder.imgBackground);

                itemHolder.itemView.setOnClickListener(v -> {
                    if (adapterListener != null) {
                        adapterListener.onDefaultBackgroundSelected(resourceId);
                    }
                });
            }
        }

        @Override
        public int getItemCount() {
            return backgroundIds.size();
        }

        // ViewHolder cho nút "Add"
        static class AddViewHolder extends RecyclerView.ViewHolder {
            // (Không cần view cụ thể bên trong vì ta chỉ cần setOnClickListener cho itemView)
            public AddViewHolder(@NonNull View itemView) {
                super(itemView);
            }
        }

        // ViewHolder cho ảnh mặc định
        static class ItemViewHolder extends RecyclerView.ViewHolder {
            ImageView imgBackground;
            public ItemViewHolder(@NonNull View itemView) {
                super(itemView);
                imgBackground = itemView.findViewById(R.id.imgBackground);
            }
        }
    }
    // --- Kết thúc Adapter ---
}