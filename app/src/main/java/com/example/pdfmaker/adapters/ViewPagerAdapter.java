package com.example.pdfmaker.adapters;

import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.LinkedList;

public class ViewPagerAdapter extends FragmentStateAdapter {
    private final LinkedList<Fragment> fragments = new LinkedList<>();
    private final LinkedList<String> titles = new LinkedList<>();

    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }
    public void addFragment(Fragment fragment , String title){
        fragments.add(fragment);
        titles.add(title);
    }
    public String getFragmentTitle(int position){
        return titles.get(position);
    }
    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return fragments.get(position);
    }

    @Override
    public int getItemCount() {
        return fragments.size();
    }

}
