package com.example.couple_app.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.couple_app.R;
import com.example.couple_app.models.Image;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter hiển thị danh sách ảnh trong RecyclerView
 * Trách nhiệm: Chỉ hiển thị dữ liệu và xử lý sự kiện UI
 */
public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImageViewHolder> {
    private static final String TAG = "ImageAdapter";

    private Context context;
    private List<Image> images;
    private OnImageClickListener onImageClickListener;

    /**
     * Interface để xử lý sự kiện click vào ảnh
     */
    public interface OnImageClickListener {
        void onImageClick(Image image);
    }

    public ImageAdapter(List<Image> images, Context context) {
        this.images = images != null ? images : new ArrayList<>();
        this.context = context;
    }

    public void setOnImageClickListener(OnImageClickListener listener) {
        this.onImageClickListener = listener;
    }

    public void setImages(List<Image> images) {
        this.images = images != null ? images : new ArrayList<>();
        Log.d(TAG, "setImages called with " + this.images.size() + " images");
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        Image image = images.get(position);
        Log.d(TAG, "Binding image at position " + position + ": " + image.getImageUrl());

        // Load ảnh bằng Glide
        Glide.with(holder.ivImage.getContext())
                .load(image.getImageUrl())
                .placeholder(R.drawable.user_icon)
                .error(R.drawable.user_icon)
                .centerCrop()
                .into(holder.ivImage);

        // Xử lý click vào ảnh
        holder.ivImage.setOnClickListener(v -> {
            if (onImageClickListener != null) {
                onImageClickListener.onImageClick(image);
            }
        });
    }

    @Override
    public int getItemCount() {
        return images.size();
    }

    /**
     * ViewHolder chứa các view trong mỗi item
     */
    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.iv_image);
        }
    }
}

