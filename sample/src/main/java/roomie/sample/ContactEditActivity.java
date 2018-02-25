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
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;

import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import roomie.sample.data.ImageData;
import roomie.sample.db.entity.ContactEntity;
import roomie.sample.rxbus.ContactEvent;
import roomie.sample.rxbus.RxBus;

public class ContactEditActivity extends AppCompatActivity {
    static final String LOG_TAG = "ContactEditActivity";

    public static final String EXTRA_CONTACT_ID = "contact_id";
    public static final String EXTRA_CONTACT_NAME = "contact_name";

    private static final int RQ_CODE_PICK_IMAGE = 100;

    private RequestOptions contactImageLoadOptions;
    private ImageView contactImage;
    private EditText firstNameView;
    private EditText lastNameView;
    private EditText phoneNumberView;

    private long contactId;
    private ContactEntity contact;
    @Nullable
    private ImageData pickedImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_edit);
        Intent intent = getIntent();
        String contactName = intent.getStringExtra(EXTRA_CONTACT_NAME);
        if (TextUtils.isEmpty(contactName)) {
            setTitle(R.string.title_activity_create_contact);
        } else {
            setTitle(contactName);
        }

        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }

        contactImageLoadOptions = new RequestOptions()
                .centerCrop()
                .fallback(R.drawable.user_image_default_details)
                .error(R.drawable.user_image_default_details)
                .placeholder(R.drawable.user_image_default_details);

        contactImage = findViewById(R.id.contact_image);
        firstNameView = findViewById(R.id.first_name);
        lastNameView = findViewById(R.id.last_name);
        phoneNumberView = findViewById(R.id.phone_number);

        contactImage.setOnClickListener(view -> pickContactImage());

        contact = new ContactEntity();


        contactId = intent.getLongExtra(EXTRA_CONTACT_ID, 0L);
        if (contactId > 0) {
            loadContactDetails();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RQ_CODE_PICK_IMAGE) {
            if (resultCode == RESULT_OK && data != null) {
                ImageData imageData = data.getParcelableExtra(ImageProviderActivity.RESULT_EXTRA_IMAGE_DATA);
                if (imageData != null) {
                    pickedImage = imageData;
                    Glide.with(this)
                            .load(imageData.getLargeImage())
                            .apply(contactImageLoadOptions)
                            .into(contactImage);
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_edit_contact_details, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_done:
                handleSaveContact();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void pickContactImage() {
        startActivityForResult(new Intent(this, ImageProviderActivity.class), RQ_CODE_PICK_IMAGE);
    }

    private void loadContactDetails() {
        contact.setId(contactId);
        Completable.defer(() -> Completable.fromRunnable(() -> contact.load(getApplicationContext())))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onContactDetailsLoaded, (e) -> onContactDetailsLoadError());
    }

    private void onContactDetailsLoaded() {
        firstNameView.setText(contact.getFirstName());
        lastNameView.setText(contact.getLastName());
        phoneNumberView.setText(contact.getPhoneNumber());

        Glide.with(this)
                .load(contact.getPhotoUri())
                .apply(contactImageLoadOptions)
                .into(contactImage);
    }

    private void onContactDetailsLoadError() {
        Log.d(LOG_TAG, "onContactDetailsLoadError(): unable to load contact details");
    }

    private void handleSaveContact() {
        contact.setFirstName(firstNameView.getText().toString().trim());
        contact.setLastName(lastNameView.getText().toString().trim());
        contact.setPhoneNumber(phoneNumberView.getText().toString().trim());
        if (pickedImage != null) {
            contact.setSmallPhotoUri(pickedImage.getListImage());
            contact.setPhotoUri(pickedImage.getLargeImage());
        }

        Completable.defer(() -> Completable.fromRunnable(() -> contact.save(getApplicationContext())))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onContactSaved, (e) -> onContactSaveError());
    }

    private void onContactSaved() {
        RxBus.from(this).publish(new ContactEvent(contact.getId(),
                contactId == 0
                        ? ContactEvent.EventType.INSERT
                        : ContactEvent.EventType.CHANGE));
        finish();
    }

    private void onContactSaveError() {
        Log.e(LOG_TAG, "onContactSaveError(): unable to save contact");
    }
}
