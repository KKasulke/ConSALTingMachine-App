package de.hitkarlsruhe.consaltingmachine.ui.main.tablayout;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

public class CMainActPagerAdapter extends FragmentStatePagerAdapter {
    public CMainActFragmentConfig mFragmConfig;
    public CMainActFragmentSensorData mFragmSensorData;
    public CMainActFragmentInfo mFragmInfo;

    public CMainActPagerAdapter(@NonNull FragmentManager fm, int behavior) {
        super(fm, behavior);

        // create three fragments
        mFragmConfig = CMainActFragmentConfig.newInstance();
        mFragmSensorData = CMainActFragmentSensorData.newInstance();
        mFragmInfo = CMainActFragmentInfo.newInstance();
    }

    // return Fragment at selected position
    @NonNull
    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return mFragmConfig;
            case 1:
                return mFragmSensorData;
            case 2:
                return mFragmInfo;
            default:
                return null;
        }
    }

    // return number of fragments
    @Override
    public int getCount() { return 3; }
}
