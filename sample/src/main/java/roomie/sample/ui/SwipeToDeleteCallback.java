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

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.View;

import roomie.sample.R;


public abstract class SwipeToDeleteCallback extends ItemTouchHelper.SimpleCallback {

    private final Drawable deleteIcon;
    private final int intrinsicIconWidth;
    private final int intrinsicIconHeight;
    private final ColorDrawable backgroundColor;

    @SuppressWarnings("ConstantConditions")
    public SwipeToDeleteCallback(Context context) {
        super(0, ItemTouchHelper.START);

        deleteIcon = ContextCompat.getDrawable(context, R.drawable.ic_delete);
        intrinsicIconWidth = deleteIcon.getIntrinsicWidth();
        intrinsicIconHeight = deleteIcon.getIntrinsicHeight();

        backgroundColor = new ColorDrawable();
        backgroundColor.setColor(ContextCompat.getColor(context, R.color.colorAccent));
    }


    @Override
    public boolean onMove(RecyclerView recyclerView,
            RecyclerView.ViewHolder viewHolder,
            RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onChildDraw(Canvas c,
            RecyclerView recyclerView,
            RecyclerView.ViewHolder viewHolder,
            float dX,
            float dY,
            int actionState,
            boolean isCurrentlyActive) {
        View itemView = viewHolder.itemView;
        int itemHeight = itemView.getBottom() - itemView.getTop();

        // Draw the delete background
        backgroundColor.setBounds(itemView.getRight() + (int) dX,
                itemView.getTop(),
                itemView.getRight(),
                itemView.getBottom());
        backgroundColor.draw(c);


        // Calculate position of delete icon
        int deleteIconTop = itemView.getTop() + (itemHeight - intrinsicIconHeight) / 2;
        int deleteIconMargin = (itemHeight - intrinsicIconHeight) / 2;
        int deleteIconLeft = itemView.getRight() - deleteIconMargin - intrinsicIconWidth;
        int deleteIconRight = itemView.getRight() - deleteIconMargin;
        int deleteIconBottom = deleteIconTop + intrinsicIconHeight;

        // Draw the delete icon
        c.save();
        c.clipRect(itemView.getRight() + (int) dX, itemView.getTop(), itemView.getRight(), itemView.getBottom());
        deleteIcon.setBounds(deleteIconLeft, deleteIconTop, deleteIconRight, deleteIconBottom);
        deleteIcon.draw(c);
        c.restore();

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }
}
