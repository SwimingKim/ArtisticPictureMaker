package com.devskim.apw.fragment

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.devskim.apw.R
import kotlinx.android.synthetic.main.fragment_image.*

class ImageFragment : Fragment() {

    private var drawable: Drawable? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_image, container, false)
        Log.d("Skim", "Create")

        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        iv_selected_image.setImageDrawable(drawable)
    }

    fun setImageDrawable(drawable: Drawable) {
        this.drawable = drawable
    }

}