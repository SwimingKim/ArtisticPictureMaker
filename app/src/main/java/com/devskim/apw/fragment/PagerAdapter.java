package com.devskim.apw.fragment;

import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import java.util.ArrayList;

public class PagerAdapter extends FragmentPagerAdapter {

    private ArrayList<Fragment> fragments;

    public PagerAdapter(FragmentManager fm, ArrayList<Fragment> fragments) {
        super(fm);
        this.fragments = fragments;
    }

    @Override
    public Fragment getItem(int i) {
        return fragments.get((int) getItemId(i));
    }

    @Override
    public int getItemPosition(@NonNull Object object) {
        return fragments.indexOf(object);
    }

    @Override
    public long getItemId(int position) {
        return position % fragments.size();
    }

    @Override
    public int getCount() {
        return Integer.MAX_VALUE;
    }

    public int getTypeCount() {
        return fragments.size();
    }

}
