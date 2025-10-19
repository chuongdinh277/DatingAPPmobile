package com.example.btl_mobileapp.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.btl_mobileapp.R;
import com.example.btl_mobileapp.activities.showImageActivity;
import com.example.btl_mobileapp.models.Image;
import com.google.firebase.database.annotations.NotNull;

import java.util.List;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ImageViewHolder> {

    private Context context;

    private List<Image> images;

    public ImageAdapter(List<Image> images, Context context) {
        this.images = images;
        this.context = context;
    }

    public void setImages(List<Image> images ) {
        this.images = images;
        this.context = context;
        notifyDataSetChanged();
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;

        public ImageViewHolder(@NotNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.iv_image);
        }
    }

    @NotNull
    @Override
    public ImageAdapter.ImageViewHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_image, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NotNull ImageViewHolder holder, int position) {
        Image image = images.get(position);

        Glide.with(holder.ivImage.getContext())
                .load(image.getImageUrl())
                .placeholder(R.drawable.user_icon)
                .error(R.drawable.user_icon)
                .into(holder.ivImage);

        holder.ivImage.setOnClickListener(v -> {
            Intent intent = new Intent(context, showImageActivity.class);
            intent.putExtra("imageUrl", image.getImageUrl());
            intent.putExtra("imageId", image.getImageId());
            intent.putExtra("coupleId", image.getCoupleId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return images.size();
    }

}
