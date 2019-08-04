package com.devskim.apw

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import com.devskim.apw.fragment.ImageFragment
import com.devskim.apw.fragment.PagerAdapter
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.layout_main.*
import org.tensorflow.contrib.android.TensorFlowInferenceInterface
import java.io.File

class MainActivity : AppCompatActivity() {

    private var tensorFlowInferenceInterface: TensorFlowInferenceInterface? = null
    private var pagerAdapter: PagerAdapter? = null

    private val bitmapManager = BitmapManager()
    private val data_names: ArrayList<String> = ArrayList<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.statusBarColor = Color.BLACK
        setSupportActionBar(bottomAppBar)

        val file_path = File("sdcard/artistic/")
        if (!file_path.exists()) {
            file_path.mkdirs()
        }

        val fragments = ArrayList<Fragment>()

        var inputBitmp: Bitmap? = null
        val file = File("sdcard/DCIM/Camera")
        for (f in file.listFiles()) {
            if (f.name.indexOf(".jpg") == -1) continue

            inputBitmp = bitmapManager.getBitmap(f.path)
            iv_selected_picture.setImageBitmap(inputBitmp)
        }

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
                Thread(Runnable {
                    val bitmap = inputBitmp?.let { styleImage(it, p0) }
                    runOnUiThread {
                        iv_selected_picture.setImageBitmap(bitmap)
                    }
                }).start()
            }
        })
        view_pager.currentItem = 0

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_appbar, menu)
        return true
    }

    private fun styleImage(inputBitmp: Bitmap, postion: Int): Bitmap? {
        try {
            pagerAdapter?.let {
                val p = it.getItemId(postion)
                val name = getAssetPath(data_names[p.toInt()])
                tensorFlowInferenceInterface = TensorFlowInferenceInterface(assets, name)
                return bitmapManager.convertBitmap(tensorFlowInferenceInterface, inputBitmp)
            }
        } catch (e: Exception) {
            Log.d("Skim", e.localizedMessage)
        }
        return null
    }

    fun getAssetPath(assetName: String): String {
        return String.format("file:///android_asset/%s", assetName)
    }

}
