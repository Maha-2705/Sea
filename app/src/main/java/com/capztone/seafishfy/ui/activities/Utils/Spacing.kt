package com.capztone.seafishfy.ui.activities.Utils

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class Spacing(private val spacing: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        outRect.left = spacing
        outRect.right = spacing
        outRect.bottom = spacing

        // Add top margin only for the first item to avoid double spacing between items
        if (parent.getChildAdapterPosition(view) == 0) {
            outRect.top = spacing
        }
    }
}
