package com.devskim.apw.fragment

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter

class FragmentAdapter(fm: FragmentManager?) : FragmentPagerAdapter(fm) {

    private val fragments = ArrayList<Fragment>()

    override fun getItem(p0: Int): Fragment {
        return fragments.get(p0)
    }

    override fun getCount(): Int {
        return fragments.size
    }

    fun addItem(fragment: Fragment) {
        fragments.add(fragment)
    }

}