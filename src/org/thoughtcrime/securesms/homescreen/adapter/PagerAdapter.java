package org.thoughtcrime.securesms.homescreen.adapter;


import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import org.thoughtcrime.securesms.homescreen.fragments.ChatsFragment;
import org.thoughtcrime.securesms.homescreen.fragments.DoctorsFragment;

public class PagerAdapter extends FragmentStatePagerAdapter {
    int mNumOfTabs;

    public PagerAdapter(FragmentManager fm, int NumOfTabs) {
        super(fm);
        this.mNumOfTabs = NumOfTabs;
    }

    @Override
    public Fragment getItem(int position) {

        switch (position) {
            case 0:
                ChatsFragment chatsTab = new ChatsFragment();
                return chatsTab;
            case 1:
                DoctorsFragment doctorsTab = new DoctorsFragment();
                return doctorsTab;
            default:
                ChatsFragment defaultTab = new ChatsFragment();
                return defaultTab;
        }
    }

    @Override
    public int getCount() {
        return mNumOfTabs;
    }
}