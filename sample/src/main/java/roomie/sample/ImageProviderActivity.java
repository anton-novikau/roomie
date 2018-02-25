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
import android.os.Parcel;
import android.os.Parcelable;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.annotation.UiThread;
import android.support.annotation.WorkerThread;
import android.support.v4.app.Fragment;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import com.google.gson.Gson;

import java.io.IOException;

import io.reactivex.Single;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import roomie.sample.data.ImageData;
import roomie.sample.ui.OnItemClickListener;

public class ImageProviderActivity extends AppCompatActivity {
    static final String LOG_TAG = "ImageProviderActivity";

    public static final String RESULT_EXTRA_IMAGE_DATA = "result_image_data";

    private static final String CONTACT_IMAGE_SOURCE = "https://www.dropbox.com/s/ko5az2aoszfcjvb/contact_images.json?dl=1";

    private static final String KEY_SAVED_STATE = "activity_saved_state";

    private ProgressBar progressBar;
    private ImageProviderAdapter imagesAdapter;

    private final OnItemClickListener itemClickListener = (position) -> {
        ImageData imageData = imagesAdapter.getItem(position);
        Intent result = new Intent();
        result.putExtra(RESULT_EXTRA_IMAGE_DATA, imageData);
        setResult(RESULT_OK, result);
        finish();
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_provider);

        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }

        progressBar = findViewById(R.id.progress);
        RecyclerView imagesView = findViewById(R.id.images);
        int itemsCount = getResources().getInteger(R.integer.image_provider_span_count);
        imagesView.setLayoutManager(new GridLayoutManager(this, itemsCount));
        imagesView.addItemDecoration(new DividerItemDecoration(this, GridLayoutManager.VERTICAL));
        imagesAdapter = new ImageProviderAdapter(this, itemClickListener);
        imagesView.setAdapter(imagesAdapter);

        ImageData[] images = null;
        if (savedInstanceState != null) {
            SavedState savedState = savedInstanceState.getParcelable(KEY_SAVED_STATE);
            if (savedState != null) {
                images = savedState.images;
            }
        }
        if (images == null) {
            loadImages();
        } else {
            onImagesLoaded(images);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        SavedState savedState = new SavedState();
        savedState.images = imagesAdapter.getData();
        outState.putParcelable(KEY_SAVED_STATE, savedState);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    private void loadImages() {
        progressBar.setVisibility(View.VISIBLE);
        Single.create((SingleOnSubscribe<ImageData[]>) e -> e.onSuccess(queryImages()))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onImagesLoaded);
    }

    @NonNull
    @WorkerThread
    private ImageData[] queryImages() {
        OkHttpClient httpClient = new OkHttpClient();
        try {
            Request request = new Request.Builder().url(CONTACT_IMAGE_SOURCE).build();
            Response response = httpClient.newCall(request).execute();
            ResponseBody body = response.body();
            if (body != null) {
                return new Gson().fromJson(body.string(), ImageData[].class);
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, "queryImages(): unable to fetch or parse image data", e);
        }

        return new ImageData[0];
    }

    @UiThread
    private void onImagesLoaded(@NonNull ImageData[] images) {
        progressBar.setVisibility(View.GONE);
        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(
                new ImageDataDiffCallback(imagesAdapter.getData(), images));
        imagesAdapter.setData(images);
        diff.dispatchUpdatesTo(imagesAdapter);
    }

    public static class SavedState implements Parcelable {

        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel source) {
                return new SavedState(source);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };

        ImageData[] images;

        public SavedState() {

        }

        SavedState(Parcel source) {
            images = source.createTypedArray(ImageData.CREATOR);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeTypedArray(images, flags);
        }
    }
}
