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

package roomie.sample.ui;

import android.graphics.Rect;
import android.support.annotation.IntDef;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * Adds space only between items in grid.
 */
public class RecyclerViewGridSpaceDivider extends RecyclerView.ItemDecoration {
    /**
     * Orientation values from {@link GridLayoutManager} can be used instead of its own.
     */
    @IntDef({GridOrientation.HORIZONTAL, GridOrientation.VERTICAL})
    public @interface GridOrientation {
        int VERTICAL = GridLayoutManager.VERTICAL;
        int HORIZONTAL = GridLayoutManager.HORIZONTAL;
    }

    private final int offset;
    private final int itemsInLine;
    @GridOrientation
    private final int orientation;

    public RecyclerViewGridSpaceDivider(@GridOrientation int orientation, int offset, int itemsInLine) {
        this.offset = offset;
        this.itemsInLine = itemsInLine;
        this.orientation = orientation;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        int position = parent.getChildLayoutPosition(view);
        if (orientation == GridOrientation.HORIZONTAL) {
            outRect.left = position >= itemsInLine ? offset : 0; // don't add left padding to the first column
            outRect.top = position % itemsInLine != 0 ? offset : 0; // don't add top padding to the first row
        } else {
            outRect.left = position % itemsInLine != 0 ? offset : 0; // don't add left padding to the first column
            outRect.top = position >= itemsInLine ? offset : 0; // don't add top padding to the first row
        }
    }
}
