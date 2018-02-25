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
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import roomie.sample.db.entity.ContactEntity;
import roomie.sample.rxbus.ContactEvent;
import roomie.sample.rxbus.RxBus;
import roomie.sample.rxbus.Subscriber;

public class ContactDetailsActivity extends AppCompatActivity {
    static final String LOG_TAG = "ContactDetailsActivity";

    public static final String EXTRA_CONTACT_ID = "contact_id";
    public static final String EXTRA_CONTACT_NAME = "contact_name";

    private TextView phoneNumberLabel;
    private TextView phoneNumberView;
    private ImageView contactImage;
    private CollapsingToolbarLayout toolbarLayout;

    private RequestOptions contactImageLoadOptions;
    /* package */ ContactEntity contact;

    private Subscriber<ContactEvent> subscriber = new ContactChangeSubscriber(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        setContentView(R.layout.activity_contact_details);

        RxBus.from(this).subscribe(ContactEvent.class, subscriber);

        Intent intent = getIntent();
        String contactName = intent.getStringExtra(EXTRA_CONTACT_NAME);
        if (!TextUtils.isEmpty(contactName)) {
            setTitle(contactName);
        }

        setSupportActionBar(findViewById(R.id.toolbar));
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }

        long contactId = intent.getLongExtra(EXTRA_CONTACT_ID, 0);
        if (contactId == 0) {
            Log.w(LOG_TAG, "onCreate(): contact id must be provided to show details");

            finish();
            return;
        }

        contactImageLoadOptions = new RequestOptions()
                .centerCrop()
                .fallback(R.drawable.user_image_default_details)
                .error(R.drawable.user_image_default_details)
                .placeholder(R.drawable.user_image_default_details);

        toolbarLayout = findViewById(R.id.toolbar_layout);
        phoneNumberLabel = findViewById(R.id.phone_number_label);
        phoneNumberView = findViewById(R.id.phone_number);
        contactImage = findViewById(R.id.contact_image);
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener((view) -> {
            Intent editIntent = new Intent(this, ContactEditActivity.class);
            editIntent.putExtra(ContactEditActivity.EXTRA_CONTACT_ID, contactId);
            editIntent.putExtra(ContactEditActivity.EXTRA_CONTACT_NAME, contactName);
            startActivity(editIntent);
        });


        contact = new ContactEntity();
        loadContactDetails(contactId);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        subscriber.dispose();
    }

    /* package */  void loadContactDetails(long contactId) {
        if (contactId <= 0) {
            return; // early exit
        }

        contact.setId(contactId);
        Completable.defer(() -> Completable.fromRunnable(() -> contact.load(getApplicationContext())))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onContactDetailsLoaded, (e) -> onContactDetailsLoadError());
    }

    private void onContactDetailsLoaded() {
        if (TextUtils.isEmpty(contact.getPhoneNumber())) {
            phoneNumberLabel.setVisibility(View.GONE);
            phoneNumberView.setVisibility(View.GONE);
        } else {
            phoneNumberLabel.setVisibility(View.VISIBLE);
            phoneNumberView.setVisibility(View.VISIBLE);
        }
        phoneNumberView.setText(contact.getPhoneNumber());
        toolbarLayout.setTitle(contact.getFullName(this));

        Glide.with(this)
                .load(contact.getPhotoUri())
                .apply(contactImageLoadOptions)
                .into(contactImage);
    }

    private void onContactDetailsLoadError() {
        Log.d(LOG_TAG, "onContactDetailsLoadError(): unable to load contact details");
    }

    private static class ContactChangeSubscriber extends Subscriber<ContactEvent> {

        private ContactDetailsActivity activity;

        public ContactChangeSubscriber(ContactDetailsActivity activity) {
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

            if (event.contactId == activity.contact.getId()) {
                if (event.type != ContactEvent.EventType.DELETE) {
                    activity.loadContactDetails(event.contactId);
                } else {
                    activity.finish();
                }
            }
        }
    }
}
