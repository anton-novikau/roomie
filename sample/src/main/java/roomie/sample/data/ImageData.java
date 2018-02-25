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

package roomie.sample.data;


import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;

import roomie.sample.data.adapter.UriTypeAdapter;

public class ImageData implements Parcelable {
    public static final Creator<ImageData> CREATOR = new Creator<ImageData>() {
        @Override
        public ImageData createFromParcel(Parcel in) {
            return new ImageData(in);
        }

        @Override
        public ImageData[] newArray(int size) {
            return new ImageData[size];
        }
    };

    @SerializedName("large_image")
    @JsonAdapter(value = UriTypeAdapter.class)
    private Uri largeImage;

    @SerializedName("list_image")
    @JsonAdapter(value = UriTypeAdapter.class)
    private Uri listImage;

    public ImageData() {

    }

    ImageData(Parcel source) {
        largeImage = source.readParcelable(Uri.class.getClassLoader());
        listImage = source.readParcelable(Uri.class.getClassLoader());
    }

    public Uri getLargeImage() {
        return largeImage;
    }

    public Uri getListImage() {
        return listImage;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelable(largeImage, i);
        parcel.writeParcelable(listImage, i);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ImageData imageData = (ImageData) o;

        return (largeImage != null
                        ? largeImage.equals(imageData.largeImage)
                        : imageData.largeImage == null)
                && (listImage != null
                        ? listImage.equals(imageData.listImage)
                        : imageData.listImage == null);
    }

    @Override
    public int hashCode() {
        int result = largeImage != null ? largeImage.hashCode() : 0;
        result = 31 * result + (listImage != null ? listImage.hashCode() : 0);
        return result;
    }
}
