package com.devskim.apw

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LabeledIntent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.MediaStore.Images
import android.support.annotation.RequiresApi
import android.support.v4.app.Fragment
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.KeyEvent
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

    private var converting_thread: Thread? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(bottomAppBar)

        makeFolder()
        initFragment()
        setEvent()

    }

    override fun onPause() {
        super.onPause()
        converting_thread?.let {
            it.interrupt()
            converting_thread = null
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP && BuildConfig.DEBUG) {
            if (inputBitmp == null) return false

            converting_thread?.let {
                it.interrupt()
                converting_thread = null
            }
            converting_thread = Thread(Runnable {
                try {
                    pagerAdapter?.let {
                        for (i in 0..it.typeCount) {
                            val filePath = String.format("/sdcard/artistic/%d.jpg", i)
                            val asset = getAssetPath(data_names[i])
                            Log.d("Skim", "start converting " + asset)
                            inputBitmp?.let {
                                tensorFlowInferenceInterface = TensorFlowInferenceInterface(assets, asset)
                                val bitmap = bitmapManager.convertBitmap(tensorFlowInferenceInterface, it)
                                bitmapManager.saveBitmap(filePath, bitmap)
                                Log.d("Skim", filePath + " save bitmap")
                            }
                        }
                    }
                } catch (e: Exception) {
                    if (e.localizedMessage != null) {
                        Log.d("Skim", e.localizedMessage)
                    }
                }
            })
            converting_thread?.start()
        }
        return super.onKeyDown(keyCode, event)
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

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK) return

        var orientation = 0
        if (requestCode == INTENT_CAMERA_REQUEST) {
            val extras = data?.getExtras()
            inputBitmp = extras?.getParcelable<Bitmap>("data")
        } else {
            val uri: Uri? = data?.data
            uri?.let {
                orientation = bitmapManager.getOrientation(this, uri)
                inputBitmp = Images.Media.getBitmap(contentResolver, uri)
            }
        }
        inputBitmp = bitmapManager.centerCropBitmap(inputBitmp, orientation)

        if (inputBitmp == null) return

        inputBitmp?.let {
            iv_selected_picture.setImageBitmap(it)
            convertingImage()
        }
    }

    private fun convertingImage() {
        converting_thread?.let {
            it.interrupt()
            converting_thread = null
        }

        converting_thread = Thread(Runnable {
            val bitmap = styleImage()
            runOnUiThread {
                iv_selected_picture.visibility = View.VISIBLE
                iv_selected_picture.setImageBitmap(bitmap)
            }
        })
        converting_thread?.start()
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

        val drawable = iv_selected_picture.drawable as BitmapDrawable
        val path = Images.Media.insertImage(contentResolver, drawable.bitmap, "Share Image", null)
        val uri = Uri.parse(path)

        val intent = getNativeShareIntent(this, uri)
        startActivity(intent)
    }

    fun getNativeShareIntent(context: Context, uri: Uri): Intent {
        val pm = context.getPackageManager()
        val sendIntent = Intent(Intent.ACTION_SEND)
        sendIntent.putExtra(Intent.EXTRA_STREAM, uri)
        sendIntent.type = "image/jpg"
        val resInfo = pm.queryIntentActivities(sendIntent, 0)
        val intentList = arrayListOf<Intent>()

        for (i in resInfo.indices) {
            val ri = resInfo.get(i)
            val packageName = ri.activityInfo.packageName
            val intent = Intent()
            intent.component = ComponentName(packageName, ri.activityInfo.name)
            intent.setPackage(packageName)
            intent.action = Intent.ACTION_SEND
            intent.putExtra(Intent.EXTRA_STREAM, uri)
            intent.type = "image/jpg"
            intentList.add(LabeledIntent(intent, packageName, ri.loadLabel(pm), ri.getIconResource()))
        }
        intentList.add(1, getSaveToGalleryIntent(context, uri))

        val openInChooser = Intent.createChooser(intentList.removeAt(0), "Share Image")
        val extraIntents = intentList.toTypedArray()
        openInChooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, extraIntents)
        return openInChooser
    }

    private fun getSaveToGalleryIntent(context: Context, uri: Uri): Intent {
//        val intent = Intent(context, MainActivity::class.java)
        val intent = Intent(Intent.ACTION_PICK)
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        return LabeledIntent(intent, BuildConfig.APPLICATION_ID, "Save to gallery", R.drawable.gallery)
    }

}
