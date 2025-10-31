package com.example.unimarket.view.home

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MenuGridSpacingDecoration(private val spacing: Int) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val layoutManager = parent.layoutManager as? GridLayoutManager ?: return
        val position = parent.getChildAdapterPosition(view)
        if (position == RecyclerView.NO_POSITION) return

        val spanCount = layoutManager.spanCount.coerceAtLeast(1)
        val column = position % spanCount

        val halfSpacing = spacing / 2
        outRect.left = if (column == 0) 0 else halfSpacing
        outRect.right = if (column == spanCount - 1) 0 else halfSpacing

        outRect.top = 0
        outRect.bottom = spacing
    }
}
