package de.hitkarlsruhe.consaltingmachine.filesystemaccess;

import android.app.Activity;
import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

import de.hitkarlsruhe.consaltingmachine.datastructures.CMeal;

public class CAsynchronousConfigFileReader implements Runnable {
    // attributes
    Activity mActivity;
    Context mContext;

    // callback interface
    IConfigFileReaderCallback mConfigFileReaderCallbackInterface;

    // create empty array for results
    CMeal[] mMealArray = null;

    // constructor
    public CAsynchronousConfigFileReader(Activity pActivity, Context pContext, IConfigFileReaderCallback pCallback) {
        mActivity = pActivity;
        mContext = pContext;
        mConfigFileReaderCallbackInterface = pCallback;
    }

    public boolean checkConfigFileExists(Context mContext) {
        File f = new File(Environment.getExternalStorageDirectory(), ".ConSALTingMachineConfiguration.txt");
        return (f.exists() && (!f.isDirectory()));
    }

    @Override
    public void run() {
        // open file ConSALTingMachineConfiguration.txt
        File configFile = new File(Environment.getExternalStorageDirectory(), ".ConSALTingMachineConfiguration.txt");
        BufferedReader in = null;
        String line;
        ArrayList<String> mStrings = new ArrayList<>();

        try {
            in = new BufferedReader(new FileReader(configFile));
            while ((line = in.readLine()) != null) {
                mStrings.add(line);
            }
            in.close();

            // allocate memory
            mMealArray = new CMeal[mStrings.size()];

            // parse content
            for(int loopIndex = 0; loopIndex < mStrings.size(); loopIndex++) {
                String[] result = mStrings.get(loopIndex).split(";;");
                mMealArray[loopIndex] = new CMeal(result[0].trim(), Float.parseFloat(result[1].trim()));
            }
        } catch (Exception e) {
            Log.e("ConSALTingMachine", "Reading config file is not possible: " + e.getMessage());
        }

        // start callback method from ui thread
        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConfigFileReaderCallbackInterface.onReadingConfigFileFinished(mContext, mMealArray);
            }
        });
    }
}
