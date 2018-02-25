/*
 * Copyright 2018 Anton Novikau
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package roomie.sample;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import roomie.sample.data.ImageData;
import roomie.sample.ui.OnItemClickListener;


public class ImageProviderAdapter extends RecyclerView.Adapter<ImageProviderAdapter.Holder> {

    private Context context;
    @NonNull
    private final LayoutInflater inflater;
    private final RequestOptions requestOptions;
    @Nullable
    private OnItemClickListener clickListener;

    public ImageProviderAdapter(@NonNull Context context, @Nullable OnItemClickListener clickListener) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.clickListener = clickListener;
        this.requestOptions = new RequestOptions()
                .placeholder(R.drawable.user_image_default_list)
                .error(R.drawable.user_image_default_list)
                .fallback(R.drawable.user_image_default_list);
    }
    @NonNull
    private ImageData[] images = new ImageData[0];

    public ImageData[] getData() {
        return images;
    }

    public void setData(@NonNull ImageData[] data) {
        this.images = data;
    }

    @Nullable
    public ImageData getItem(int position) {
        return position >= 0 && position < images.length ? images[position] : null;
    }

    @Override
    public int getItemCount() {
        return images.length;
    }

    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new Holder(inflater.inflate(R.layout.grid_item_image, parent, false), clickListener);
    }

    @Override
    public void onBindViewHolder(Holder holder, int position) {
        ImageData imageData = images[position];
        Glide.with(context).load(imageData.getListImage())
                .apply(requestOptions)
                .into(holder.image);
    }

    public static class Holder extends RecyclerView.ViewHolder {
        ImageView image;

        public Holder(@NonNull View itemView, @Nullable OnItemClickListener clickListener) {
            super(itemView);
            image = (ImageView) itemView;
            itemView.setOnClickListener(view -> {
                int position = getAdapterPosition();
                if (clickListener != null && position != RecyclerView.NO_POSITION) {
                    clickListener.onItemClick(position);
                }
            });
        }
    }
}
