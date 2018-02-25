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

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import roomie.api.DatabaseProvider;
import roomie.sample.db.RoomieDatabaseProvider;
import roomie.sample.db.entity.ContactEntity;
import roomie.sample.rxbus.ContactEvent;
import roomie.sample.rxbus.RxBus;
import roomie.sample.rxbus.Subscriber;
import roomie.sample.ui.OnItemClickListener;
import roomie.sample.ui.SwipeToDeleteCallback;

import static roomie.sample.db.entity.ContactEntityHelper.queryAll;

public class ContactListActivity extends AppCompatActivity {
    static final String LOG_TAG = "ContactListActivity";

    private DatabaseProvider databaseProvider;
    private ContactListAdapter contactListAdapter;

    private final Subscriber<ContactEvent> subscriber = new ContactChangeSubscriber(this);

    private final OnItemClickListener itemClickListener = (position) -> {
        ContactEntity contact = contactListAdapter.getItem(position);
        if (contact != null) {
            Intent intent = new Intent(this, ContactDetailsActivity.class);
            intent.putExtra(ContactDetailsActivity.EXTRA_CONTACT_ID, contact.getId());
            String fullName = contact.getFullName(this);
            intent.putExtra(ContactDetailsActivity.EXTRA_CONTACT_NAME, fullName);
            startActivity(intent);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_list);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        databaseProvider = RoomieDatabaseProvider.from(this);
        RxBus.from(this).subscribe(ContactEvent.class, subscriber);

        RecyclerView contactListView = findViewById(R.id.contact_list);
        findViewById(R.id.fab).setOnClickListener(view -> openAddContact());

        contactListAdapter = new ContactListAdapter(this, itemClickListener);
        contactListView.addItemDecoration(new DividerItemDecoration(this, GridLayoutManager.VERTICAL));
        contactListView.setAdapter(contactListAdapter);
        ItemTouchHelper helper = new ItemTouchHelper(new SwipeToDeleteCallback(this) {
            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                ContactEntity contact = contactListAdapter.getItem(position);
                if (contact != null) {
                    contactListAdapter.removeAt(position);
                    handleContactRemoved(contact);
                }
            }
        });
        helper.attachToRecyclerView(contactListView);

        loadContacts();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        subscriber.dispose();
    }

    /* package */ void loadContacts() {
        Single.create((SingleOnSubscribe<List<ContactEntity>>) e -> e.onSuccess(queryAll(databaseProvider)))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onContactsLoaded);
    }

    private void openAddContact() {
        startActivity(new Intent(this, ContactEditActivity.class));
    }

    private void onContactsLoaded(@NonNull List<ContactEntity> contacts) {
        Log.d("CONTACTS", "contacts loaded = " + contacts);
        List<ContactEntity> oldContacts = contactListAdapter.getData();
        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new ContactDiffCallback(oldContacts, contacts));
        contactListAdapter.setData(contacts);
        contactListAdapter.notifyDataSetChanged();
        diff.dispatchUpdatesTo(contactListAdapter);
    }

    private void handleContactRemoved(ContactEntity contact) {
        Completable.defer(() -> Completable.fromRunnable(() -> contact.delete(getApplicationContext())))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onContactDeleted, (e) -> onContactDeleteError());
    }

    private void onContactDeleted() {
        Log.d(LOG_TAG, "onContactDeleted()");
    }

    private void onContactDeleteError() {
        Log.d(LOG_TAG, "onContactDeleteError()");
    }

    private static class ContactChangeSubscriber extends Subscriber<ContactEvent> {

        private ContactListActivity activity;

        public ContactChangeSubscriber(ContactListActivity activity) {
            this.activity = activity;
        }

        @Override
        public void release() {
            activity = null;
        }

        @Override
        public void accept(ContactEvent event) {
            if (activity == null) {
                return; // early exit
            }

            activity.loadContacts();
        }
    }
}
