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

import android.support.annotation.NonNull;
import android.support.v7.util.DiffUtil;

import roomie.sample.data.ImageData;


public class ImageDataDiffCallback extends DiffUtil.Callback {

    @NonNull
    private final ImageData[] oldList;
    @NonNull
    private final ImageData[] newList;

    public ImageDataDiffCallback(@NonNull ImageData[] oldList, @NonNull ImageData[] newList) {
        this.oldList = oldList;
        this.newList = newList;
    }

    @Override
    public int getOldListSize() {
        return oldList.length;
    }

    @Override
    public int getNewListSize() {
        return newList.length;
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        return oldList[oldItemPosition] == newList[newItemPosition];
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        return oldList[oldItemPosition].equals(newList[newItemPosition]);
    }
}
