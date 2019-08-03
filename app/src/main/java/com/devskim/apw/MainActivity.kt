package com.devskim.apw

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.devskim.apw.fragment.ImageFragment
import com.devskim.apw.fragment.PagerAdapter
import kotlinx.android.synthetic.main.activity_main.*
import org.tensorflow.contrib.android.TensorFlowInferenceInterface
import java.io.File

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.statusBarColor = Color.BLACK

        val file_path = File("sdcard/artistic/")
        if (!file_path.exists()) {
            file_path.mkdirs()
        }

        val fragments = ArrayList<Fragment>()
        val bitmapManager = BitmapManager()

        var inputBitmp: Bitmap? = null
        val file = File("sdcard/DCIM/Camera")
        for (f in file.listFiles()) {
            if (f.name.indexOf(".jpg") == -1) continue

            inputBitmp = bitmapManager.getBitmap(f.path)
        }

        var tensorFlowInferenceInterface: TensorFlowInferenceInterface
        var outputBitmap: Bitmap
        val asset_files = assets.list("")
        for (asset in asset_files) {
//            for (asset in asset_files) {
            if (asset.indexOf(".pb") == -1) continue

            try {
//                    tensorFlowInferenceInterface = TensorFlowInferenceInterface(assets, getAssetPath(asset))
//                    outputBitmap = bitmapManager.stylizeImage(tensorFlowInferenceInterface, inputBitmp)
//
//                    val file_name = file_path.path + File.separator + asset.toString() + ".png"
//                    Log.d("Skim", file_name)
//                    bitmapManager.saveBitmap(file_name, outputBitmap)
            } catch (e: Exception) {
                Log.d("Skim", e.localizedMessage)
            }

            val image_name = asset.replace("_float.pb", "").toLowerCase()
            val fragment = ImageFragment()

            val information = getString(resources.getIdentifier(image_name, "string", packageName))
            val drawable = bitmapManager.getImageByName(image_name, this)
            fragment.setData(drawable, information)
            fragments.add(fragment)

        }

        val pagerAdapter = PagerAdapter(supportFragmentManager, fragments)
        view_pager.adapter = pagerAdapter
        view_pager.currentItem = fragments.size * 10

    }

    fun getAssetPath(assetName: String): String {
        return String.format("file:///android_asset/%s", assetName)
    }

}
