package com.devskim.apw.fragment

import android.os.Bundle
import android.support.design.widget.BottomSheetDialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.devskim.apw.R
import kotlinx.android.synthetic.main.layout_dialog.*

class BottomSheetDialog : BottomSheetDialogFragment() {


    interface UploadChooserNotifierInterface {
        fun camerOnClik()
        fun galleryOnClick()
    }

    var uploadChooserNotifierInterface: UploadChooserNotifierInterface? = null
    fun addNotifier(listener: UploadChooserNotifierInterface) {
        uploadChooserNotifierInterface = listener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.layout_dialog, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        setupListener()
    }

    private fun setupListener() {
        upload_camera.setOnClickListener {
            uploadChooserNotifierInterface?.camerOnClik()
        }
        upload_gallery.setOnClickListener {
            uploadChooserNotifierInterface?.galleryOnClick()
        }
    }


}