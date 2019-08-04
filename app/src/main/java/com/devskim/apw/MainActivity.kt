package com.devskim.apw

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.provider.MediaStore
import android.provider.MediaStore.Images
import android.support.v4.app.Fragment
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.devskim.apw.fragment.BottomSheetDialog
import com.devskim.apw.fragment.ImageFragment
import com.devskim.apw.fragment.PagerAdapter
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.layout_main.*
import org.tensorflow.contrib.android.TensorFlowInferenceInterface
import java.io.File


class MainActivity : AppCompatActivity(), View.OnClickListener {

    private var tensorFlowInferenceInterface: TensorFlowInferenceInterface? = null
    private var pagerAdapter: PagerAdapter? = null
    private var inputBitmp: Bitmap? = null
    private var bottomDialog: BottomSheetDialog? = null

    private val bitmapManager = BitmapManager()
    private val data_names: ArrayList<String> = ArrayList<String>()

    private val INTENT_CAMERA_REQUEST = 101
    private val INTENT_GALLERY_REQUEST = 102

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.statusBarColor = Color.BLACK
        setSupportActionBar(bottomAppBar)

        makeFolder()
        initFragment()
        setEvent()

    }

    private fun setEvent() {

        bt_prev.setOnClickListener(this)
        bt_next.setOnClickListener(this)
        bt_share.setOnClickListener(this)
        iv_preview_image.setOnClickListener(this)

        bottomDialog = BottomSheetDialog().apply {
            addNotifier(object : BottomSheetDialog.UploadChooserNotifierInterface {
                override fun camerOnClik() {
                    showCamera()
                    bottomDialog?.dismiss()
                }

                override fun galleryOnClick() {
                    showGallery()
                    bottomDialog?.dismiss()
                }
            })
        }

    }

    override fun onClick(v: View?) {
        when (v) {
            iv_preview_image -> bottomDialog?.show(supportFragmentManager, "")
            bt_prev -> {
                view_pager.currentItem -= 1
                pagerAdapter?.notifyDataSetChanged()
            }
            bt_next -> {
                view_pager.currentItem += 1
                pagerAdapter?.notifyDataSetChanged()
            }
            bt_share -> shareIntent()
        }
    }


    private fun makeFolder() {

        val file_path = File("sdcard/artistic/")
        if (!file_path.exists()) {
            file_path.mkdirs()
        }

    }

    private fun initFragment() {
        val fragments = ArrayList<Fragment>()

        val asset_files = assets.list("")
        for (asset in asset_files) {
            if (asset.indexOf(".pb") == -1) continue

            val image_name = asset.replace("_float.pb", "").toLowerCase()
            data_names.add(asset)
            val fragment = ImageFragment()

            val information = getString(resources.getIdentifier(image_name, "string", packageName))
            val drawable = bitmapManager.getImageByName(image_name, this)
            fragment.setData(drawable, information)
            fragments.add(fragment)
        }

        pagerAdapter = PagerAdapter(supportFragmentManager, fragments)
        view_pager.adapter = pagerAdapter
        view_pager.currentItem = fragments.size * 10
        view_pager.setOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(p0: Int) {
            }

            override fun onPageScrolled(p0: Int, p1: Float, p2: Int) {
            }

            override fun onPageSelected(p0: Int) {
                convertingImage()
            }
        })
        view_pager.currentItem = fragments.size * 10
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode != Activity.RESULT_OK) return

        if (requestCode == INTENT_CAMERA_REQUEST) {
            val extras = data?.getExtras()
            inputBitmp = extras?.getParcelable<Bitmap>("data")
        } else {
            inputBitmp = Images.Media.getBitmap(contentResolver, data?.getData())
        }
        inputBitmp = bitmapManager.centerCropBitmap(inputBitmp)

        if (inputBitmp == null) return

        inputBitmp?.let {
            iv_selected_picture.setImageBitmap(it)
            convertingImage()
        }
    }

    private fun convertingImage() {
        Thread(Runnable {
            val bitmap = styleImage()
            runOnUiThread {
                iv_selected_picture.visibility = View.VISIBLE
                iv_selected_picture.setImageBitmap(bitmap)
            }
        }).start()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_appbar, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_camera -> showCamera()
            R.id.menu_gallery -> showGallery()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showCamera() {

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(intent, INTENT_CAMERA_REQUEST)

    }

    private fun showGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, INTENT_GALLERY_REQUEST)
    }

    private fun styleImage(): Bitmap? {
        runOnUiThread {
            pb_converting.visibility = View.VISIBLE
        }
        try {
            pagerAdapter?.let {
                val p = it.getItemId(view_pager.currentItem).toInt()
                val name = getAssetPath(data_names[p])

                inputBitmp?.let {
                    tensorFlowInferenceInterface = TensorFlowInferenceInterface(assets, name)
                    val bitmap = bitmapManager.convertBitmap(tensorFlowInferenceInterface, it)
                    runOnUiThread {
                        pb_converting.visibility = View.GONE
                    }
                    return bitmap
                }

            }
        } catch (e: Exception) {
            if (e.localizedMessage != null) {
                Log.d("Skim", e.localizedMessage)
            }
        }
        runOnUiThread {
            pb_converting.visibility = View.GONE
        }
        return null
    }

    fun getAssetPath(assetName: String): String {
        return String.format("file:///android_asset/%s", assetName)
    }

    private fun shareIntent() {
        if (inputBitmp == null) return

        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "image/png"
        intent.putExtra(Intent.EXTRA_STREAM, inputBitmp)
        startActivity(Intent.createChooser(intent, "Share"))
    }

}
