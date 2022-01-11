package de.hitkarlsruhe.consaltingmachine.filesystemaccess;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import de.hitkarlsruhe.consaltingmachine.datastructures.CMeal;

public class CAsynchronousConfigFileWriter implements Runnable {
    // attributes
    Context mContext;
    CMeal[] mMealArray;

    // constructor
    public CAsynchronousConfigFileWriter(Context pContext, CMeal[] pMealArray) {
        mContext = pContext;
        mMealArray = pMealArray;
    }

    public boolean checkConfigFileExists(Context mContext) {
        File f = new File(Environment.getExternalStorageDirectory(), ".ConSALTingMachineConfiguration.txt");
        return (f.exists() && (!f.isDirectory()));
    }

    @Override
    public void run() {
        // open file .ConSALTingMachineConfiguration.txt
        File configFile = new File(Environment.getExternalStorageDirectory(), ".ConSALTingMachineConfiguration.txt");

        // convert content to string
        StringBuilder mFileContentBuffer = new StringBuilder();
        for(CMeal meal : mMealArray) {
            mFileContentBuffer.append(meal.mName);
            mFileContentBuffer.append(";;");
            mFileContentBuffer.append(meal.mSaltAmount);
            mFileContentBuffer.append("\n");
        }

        // write object to file
        try {
            FileWriter out = new FileWriter(configFile);
            out.write(mFileContentBuffer.toString());
            out.close();
        } catch (IOException e) {
            Log.e("ConSALTingMachine","Writing config file is not possible: " + e.getMessage());
        }
    }
}
