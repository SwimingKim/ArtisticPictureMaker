package com.devskim.apw.fragment

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.devskim.apw.R
import kotlinx.android.synthetic.main.fragment_image.*

class ImageFragment : Fragment() {

    private var information: String? = null
    private var drawable: Drawable? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View?
            = inflater.inflate(R.layout.fragment_image, container, false)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        iv_selected_image.setImageDrawable(drawable)
        tx_info.text = information
    }

    fun setData(drawable: Drawable, information: String) {
        this.drawable = drawable
        this.information = information
    }

}