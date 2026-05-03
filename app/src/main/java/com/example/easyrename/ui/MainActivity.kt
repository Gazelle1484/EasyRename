package com.example.easyrename.ui

import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.easyrename.ui.home.HomeFragment

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val container = FrameLayout(this).apply {
            id = FRAGMENT_CONTAINER_ID
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
            )
        }
        setContentView(container)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(FRAGMENT_CONTAINER_ID, HomeFragment())
                .commit()
        }
    }

    companion object {
        val FRAGMENT_CONTAINER_ID: Int = View.generateViewId()
    }
}
