package com.example.easyrename.ui.common

import android.content.Context
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.ProgressBar

class LoadingView(context: Context) : FrameLayout(context) {

    private val progressBar = ProgressBar(context)

    init {
        visibility = GONE
        addView(
            progressBar,
            LayoutParams(
                LayoutParams.WRAP_CONTENT,
                LayoutParams.WRAP_CONTENT,
                Gravity.CENTER,
            ),
        )
    }

    fun setLoading(isLoading: Boolean) {
        visibility = if (isLoading) VISIBLE else GONE
    }
}
