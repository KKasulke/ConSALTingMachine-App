package de.hitkarlsruhe.consaltingmachine.filesystemaccess;

import android.content.Context;

import de.hitkarlsruhe.consaltingmachine.datastructures.CMeal;

public interface IConfigFileReaderCallback {
    void onReadingConfigFileFinished(Context mContext, CMeal[] pMealArray);
}
