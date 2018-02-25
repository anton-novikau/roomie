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

package roomie.sample.db.entity;


import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import roomie.api.Column;
import roomie.api.DatabaseType;
import roomie.api.Entity;
import roomie.api.PrimaryKey;
import roomie.sample.R;
import roomie.sample.db.adapter.UriTypeAdapter;

@Entity(table = "contacts")
public class ContactEntity extends BaseEntity<ContactEntity> {

    @Column(name = "_ID")
    @PrimaryKey
    long id;
    @Column(name = "FIRST_NAME")
    String firstName;
    @Column(name = "LAST_NAME")
    String lastName;
    @Column(name = "PHONE_NUMBER")
    String phoneNumber;
    @Column(name = "PHOTO_URI", adapter = UriTypeAdapter.class, databaseType = DatabaseType.TEXT)
    Uri photoUri;
    @Column(name = "SMALL_PHOTO_URI", adapter = UriTypeAdapter.class, databaseType = DatabaseType.TEXT)
    Uri smallPhotoUri;

    public ContactEntity() {
        super(new ContactEntityHelper());
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public Uri getPhotoUri() {
        return photoUri;
    }

    public void setPhotoUri(Uri photoUri) {
        this.photoUri = photoUri;
    }

    public Uri getSmallPhotoUri() {
        return smallPhotoUri;
    }

    public void setSmallPhotoUri(Uri smallPhotoUri) {
        this.smallPhotoUri = smallPhotoUri;
    }

    public String getFullName(Context context) {
        if (TextUtils.isEmpty(firstName)) {
            return lastName;
        } else if (TextUtils.isEmpty(lastName)) {
            return firstName;
        } else {
            return context.getString(R.string.contact_full_name, firstName, lastName);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ContactEntity that = (ContactEntity) o;

        if (id != that.id) {
            return false;
        }
        if (firstName != null ? !firstName.equals(that.firstName) : that.firstName != null) {
            return false;
        }
        if (lastName != null ? !lastName.equals(that.lastName) : that.lastName != null) {
            return false;
        }
        if (phoneNumber != null ? !phoneNumber.equals(that.phoneNumber) : that.phoneNumber != null) {
            return false;
        }
        if (photoUri != null ? !photoUri.equals(that.photoUri) : that.photoUri != null) {
            return false;
        }
        return smallPhotoUri != null ? smallPhotoUri.equals(that.smallPhotoUri) : that.smallPhotoUri == null;
    }

    @Override
    public int hashCode() {
        return (int) (id ^ (id >>> 32));
    }
}
