package de.hitkarlsruhe.consaltingmachine;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import com.google.android.material.tabs.TabLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;

import de.hitkarlsruhe.consaltingmachine.fsm_connection.CFSMConnection;
import de.hitkarlsruhe.consaltingmachine.datastructures.CMeal;
import de.hitkarlsruhe.consaltingmachine.filesystemaccess.CAsynchronousConfigFileReader;
import de.hitkarlsruhe.consaltingmachine.filesystemaccess.CAsynchronousConfigFileWriter;
import de.hitkarlsruhe.consaltingmachine.filesystemaccess.IConfigFileReaderCallback;
import de.hitkarlsruhe.consaltingmachine.ui.main.tablayout.CMainActPagerAdapter;
import de.hitkarlsruhe.consaltingmachine.ui.main.tablayout.IFragmentActions;

/* class CMainActivity
    @ToDo:
        ==> Icon?
        ==> Beschreibung in Info-Tab?
        ==> Default-Salz-Werte für Reis/Nudeln/Kartoffeln/...?
        ==> Salz-Offset?
 */

public class CMainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback,
                                                                IConfigFileReaderCallback,
        IActivityActions {
    // define initial configuration file content:
    CMeal[] mMealArray = {
            new CMeal("Nudeln",5.0f),
            new CMeal("Reis", 10.0f),
            new CMeal("Kartoffeln", 15.0f)
    };

    // object of bluetooth adapter
    BluetoothAdapter blAdapter;                 // Get Object of Bluetooth-Adapter

    // Object for FSM
    CFSMConnection mFSM;

    // integer to compare result code after
    // asking user to enable bluetooth
    final int REQUEST_ENABLE_BLE = 1;
    final int REQUEST_PERM_LOCATION = 2;
    final int REQUEST_PERM_STORAGE = 3;

    // determine if locationPermission and storage permission has been checked
    boolean locationPermissionChecked = false;
    boolean storagePermissionChecked = false;

    // Object for closing permission dialogs on onPause
    AlertDialog mDialogLocationEnable;
    AlertDialog mDialogLocationPermission;

    // asynchronous functions which provide access to config file
    CAsynchronousConfigFileReader mFileReader;
    CAsynchronousConfigFileWriter mFileWriter;

    // global UI variables
    public TabLayout tabLayout;
    public ViewPager viewPager;
    public CMainActPagerAdapter mMainActPagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // disable night mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        // define toolbar with app name
        Toolbar mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);

        // Define tab layout
        tabLayout = findViewById(R.id.act_main_tablayout);
        tabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

        // add 3 tabs to the tab layout:
        String[] tabNames = {"Konfiguration", "Sensordaten", "Info"};
        for(int i = 0; i < 3; i++) {
            tabLayout.addTab(tabLayout.newTab().setText(tabNames[i]));
        }

        // define ViewPager and set pager adapter
        viewPager = findViewById(R.id.act_main_viewPager);
        mMainActPagerAdapter = new CMainActPagerAdapter(getSupportFragmentManager(),
                FragmentStatePagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
        viewPager.setAdapter(mMainActPagerAdapter);

        // allow Tab-Scrolling within viewPager
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));

        // add listener for tab selection
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                // if new tab is selected -> load tab in viewPager
                viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) { }

            @Override
            public void onTabReselected(TabLayout.Tab tab) { }
        });

        // get Bluetooth adapter
        blAdapter = BluetoothAdapter.getDefaultAdapter();

        // check that bluetooth is available on the device
        if(blAdapter == null) {
            // print error message, stop app
            AlertDialog.Builder builder = new AlertDialog.Builder(CMainActivity.this);
            builder.setTitle("Bluetooth nicht verfügbar");
            builder.setMessage("Bluetooth ist auf diesem Gerät nicht verfügbar!"
                + " Diese App kann daher nicht verwendet werden");
            builder.setPositiveButton("App beenden", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // request location permission at runtime
                    dialog.dismiss();
                    finish();
                }
            });
            builder.setCancelable(false);
            builder.create().show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("ConSALTingMachine", "onResume");

        if(!checkAllPermissionsStartFSM()) {
            return;
        }

        // request location permission and storage permission once at runtime
        // this will also start config file reading
        if(!locationPermissionChecked) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERM_LOCATION);
        }
        if(!storagePermissionChecked) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERM_STORAGE);
        }

        // give event to fsm if object exists
        if(mFSM != null) {
            mFSM.dispatchEvent(EEvents.ON_RESUME, CMainActivity.this);
        }

        // try to update Layout
        ((IFragmentActions)mMainActPagerAdapter.mFragmConfig).notifySpinnerUpdate(CMainActivity.this, mMealArray);
        ((IFragmentActions)mMainActPagerAdapter.mFragmConfig).notifyLayoutUpdate(CMainActivity.this);
        ((IFragmentActions)mMainActPagerAdapter.mFragmSensorData).notifyLayoutUpdate(CMainActivity.this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("ConSALTingMachine", "onPause");

        // close all opened dialogs
        if(mDialogLocationEnable != null) {
            mDialogLocationEnable.dismiss();
            mDialogLocationEnable = null;
        }
        if(mDialogLocationPermission != null) {
            mDialogLocationPermission.dismiss();
            mDialogLocationPermission = null;
        }

        // close all opened dialogs from FSM
        if(mFSM != null && mFSM.mActions != null && mFSM.mActions.mDialog != null) {
            mFSM.mActions.mDialog.dismiss();
        }

        // notify FSM to disconnect
        if(mFSM != null) {
            mFSM.dispatchEvent(EEvents.ON_PAUSE, CMainActivity.this);
        }
    }

    boolean checkAllPermissionsStartFSM() {
        Log.d("ConSALTingMachine", "checkAllPermissions");
        // bluetooth enabled
        if(!blAdapter.isEnabled()) {
            // ask user to enable bluetooth
            Intent enableBLEIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBLEIntent, REQUEST_ENABLE_BLE);
            return false;
        }

        // location permission
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            askForLocationPermissionInfoDialog();
            return false;
        }

        // location enabled
        if(!checkLocationEnabled(CMainActivity.this)) {
            askUserToEnableLocationServicesInfoDialog();
            return false;
        }

        // storage
        if(checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERM_STORAGE);
            return false;
        }

        // all permissions granted, start FSM (only once!)
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(mFSM == null) {
                    mFSM = new CFSMConnection(CMainActivity.this, mMainActPagerAdapter, CMainActivity.this);
                }
            }
        });
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("ConSALTingMachine", "onActivityResult; resultCode: " + resultCode);
        if(requestCode == REQUEST_ENABLE_BLE) {
            switch (resultCode) {
                case RESULT_OK:
                    // Bluetooth is enabled now; check all permissions
                    checkAllPermissionsStartFSM();
                    break;
                case RESULT_CANCELED:
                    // Bluetooth not enabled ==> ask again
                    Intent enableBLEIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBLEIntent, REQUEST_ENABLE_BLE);
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.d("ConSALTingMachine", "onRequestPermissionResult; requestCode: " + requestCode);
        if(requestCode == REQUEST_PERM_LOCATION && grantResults.length == 1) {
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // set boolean variable
                locationPermissionChecked = true;

                // check for location services enabled
                if(!checkLocationEnabled(CMainActivity.this)) {
                    // ask user to enable location services
                    askUserToEnableLocationServicesInfoDialog();
                }
                else {
                    checkAllPermissionsStartFSM();
                }
            }
            else {
                askForLocationPermissionInfoDialog();
            }
        }
        else if(requestCode == REQUEST_PERM_LOCATION) {
            // length is 0, check for permission
            if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                // set boolean variable
                locationPermissionChecked = true;

                // check for location services enabled
                if(!checkLocationEnabled(CMainActivity.this)) {
                    // ask user to enable location services
                    askUserToEnableLocationServicesInfoDialog();
                }
                else {
                    checkAllPermissionsStartFSM();
                }
            } else {
                // permission not granted, ask again
                askForLocationPermissionInfoDialog();
            }
        }
        else if(requestCode == REQUEST_PERM_STORAGE && grantResults.length == 1) {
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // set boolean variable
                storagePermissionChecked = true;

                // initialize objects for accessing config file
                if(mFileReader == null || mFileWriter == null) {
                    mFileReader = new CAsynchronousConfigFileReader(this, CMainActivity.this, this);
                    mFileWriter = new CAsynchronousConfigFileWriter(CMainActivity.this, mMealArray);
                }

                // check if config file exists
                if(mFileWriter.checkConfigFileExists(CMainActivity.this)) {
                    // read config file
                    new Thread(mFileReader).start();
                } else {
                    // write config file
                    new Thread(mFileWriter).start();

                    // update UI with initial values
                    ((IFragmentActions)mMainActPagerAdapter.getItem(tabLayout.getSelectedTabPosition()))
                            .notifySpinnerUpdate(CMainActivity.this, mMealArray);
                }
                checkAllPermissionsStartFSM();
            }
            else {
                // ask again for permission
                ActivityCompat.requestPermissions(this, new String[]
                        {Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERM_STORAGE);
            }
        } else if(requestCode == REQUEST_PERM_STORAGE) {
            // length is 0, check permission
            if(checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                // set boolean variable
                storagePermissionChecked = true;

                // initialize objects for accessing config file
                if(mFileReader == null || mFileWriter == null) {
                    mFileReader = new CAsynchronousConfigFileReader(this, CMainActivity.this, this);
                    mFileWriter = new CAsynchronousConfigFileWriter(CMainActivity.this, mMealArray);
                }
                // check if config file exists
                if(mFileWriter.checkConfigFileExists(CMainActivity.this)) {
                    // read config file
                    new Thread(mFileReader).start();
                } else {
                    // write config file
                    new Thread(mFileWriter).start();

                    // update UI with initial values
                    ((IFragmentActions)mMainActPagerAdapter.getItem(tabLayout.getSelectedTabPosition()))
                            .notifySpinnerUpdate(CMainActivity.this, mMealArray);
                }
                checkAllPermissionsStartFSM();
            }
        }
    }

    // check, if location services enabled
    public static boolean checkLocationEnabled(Context context) {
        int locationMode = 0;

        try {
            locationMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);

        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
            return false;
        }

        return locationMode != Settings.Secure.LOCATION_MODE_OFF;
    }

    public void askForLocationPermissionInfoDialog() {
        // show message dialog and explain, why permission is required
        if(mDialogLocationPermission == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(CMainActivity.this);
            builder.setTitle("Standortdienste erforderlich");
            builder.setMessage("Die Suche nach Bluetooth-Low-Energy-Geräten ist nur möglich," +
                    " sofern der App die Berechtigung" +
                    " ACCESS_FINE_LOCATION erteilt wurde. Bitte erteile daher im nächsten Dialog die" +
                    " erforderliche Berechtigung.");
            builder.setPositiveButton("Weiter", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // request location permission at runtime
                    dialog.dismiss();
                    mDialogLocationPermission = null;
                    ActivityCompat.requestPermissions(CMainActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERM_LOCATION);
                }
            });
            builder.setCancelable(false);
            mDialogLocationPermission = builder.create();
            mDialogLocationPermission.show();
        }
    }

    public void askUserToEnableLocationServicesInfoDialog() {
        // show message dialog and explain, why location services are required
        if(mDialogLocationEnable == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(CMainActivity.this);
            builder.setTitle("Standortdienste aktivieren");
            builder.setMessage("Das Sicherheitskonzept von Android erfordert zusätzlich die Aktivierung der"
                    + " Standorddienste, andernfalls ist keine Suche nach Bluetooth-Low-Energy-Geräten"
                    + " möglich. Bitte aktiviere auf der folgenden Bildschirmseite die Standorddienste.");
            builder.setPositiveButton("Weiter", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // request location permission at runtime
                    dialog.dismiss();
                    mDialogLocationEnable = null;
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                }
            });
            builder.setCancelable(false);
            mDialogLocationEnable = builder.create();
            mDialogLocationEnable.show();
        }
    }

    @Override
    public void onReadingConfigFileFinished(Context mContext, CMeal[] pMealArray) {
        // store result
        mMealArray = pMealArray;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((IFragmentActions)mMainActPagerAdapter.mFragmConfig).notifySpinnerUpdate(mContext, pMealArray);
            }
        });
    }

    @Override
    public void passEvent(EEvents mEvent) {
        if(mFSM != null) {
            mFSM.dispatchEvent(mEvent, CMainActivity.this);
        }
    }
}