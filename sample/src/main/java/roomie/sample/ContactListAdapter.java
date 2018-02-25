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
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import java.util.Collections;
import java.util.List;

import roomie.sample.db.entity.ContactEntity;
import roomie.sample.ui.OnItemClickListener;


public class ContactListAdapter extends RecyclerView.Adapter<ContactListAdapter.Holder> {

    private final Context context;
    private final LayoutInflater inflater;
    private final OnItemClickListener clickListener;
    private final RequestOptions loadImageOptions;
    @NonNull
    private List<ContactEntity> contacts = Collections.emptyList();

    public ContactListAdapter(@NonNull Context context, @Nullable OnItemClickListener clickListener) {
        this.context = context.getApplicationContext();
        this.inflater = LayoutInflater.from(context);
        this.clickListener = clickListener;
        this.loadImageOptions = new RequestOptions()
                .error(R.drawable.user_image_default_list)
                .placeholder(R.drawable.user_image_default_list)
                .fallback(R.drawable.user_image_default_list);
    }

    @NonNull
    public List<ContactEntity> getData() {
        return contacts;
    }

    public void setData(@NonNull List<ContactEntity> data) {
        contacts = data;
    }

    public void removeAt(int position) {
        if (position < 0 || position >= contacts.size()) {
            return; // early exit
        }

        contacts.remove(position);
        if (contacts.isEmpty()) {
            notifyDataSetChanged();
        } else {
            notifyItemRemoved(position);
        }
    }

    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new Holder(inflater.inflate(R.layout.list_item_contact, parent, false), clickListener);
    }

    @Override
    public void onBindViewHolder(Holder holder, int position) {
        ContactEntity contact = contacts.get(position);
        holder.contactName.setText(contact.getFullName(context));
        holder.phoneNumber.setText(contact.getPhoneNumber());
        Glide.with(holder.contactImage)
                .load(contact.getPhotoUri())
                .apply(loadImageOptions)
                .into(holder.contactImage);
    }

    @Nullable
    public ContactEntity getItem(int position) {
        if (position < 0 || position >= contacts.size()) {
            return null;
        }

        return contacts.get(position);
    }

    @Override
    public long getItemId(int position) {
        if (position < 0 || position >= contacts.size()) {
            return 0;
        }

        return contacts.get(position).getId();
    }

    @Override
    public int getItemCount() {
        return contacts.size();
    }

    public static class Holder extends RecyclerView.ViewHolder {
        ImageView contactImage;
        TextView contactName;
        TextView phoneNumber;

        public Holder(@NonNull View itemView, @Nullable OnItemClickListener listener) {
            super(itemView);

            contactImage = itemView.findViewById(R.id.contact_image);
            contactName = itemView.findViewById(R.id.contact_name);
            phoneNumber = itemView.findViewById(R.id.phone_number);

            itemView.setOnClickListener((view) -> {
                int position = getAdapterPosition();
                if (listener != null && position != RecyclerView.NO_POSITION) {
                    listener.onItemClick(position);
                }
            });
        }
    }
}
