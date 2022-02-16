package de.hitkarlsruhe.consaltingmachine.fsm_connection;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.List;

import de.hitkarlsruhe.consaltingmachine.CMainActivity;
import de.hitkarlsruhe.consaltingmachine.EEvents;
import de.hitkarlsruhe.consaltingmachine.datastructures.CMachineControlData;
import de.hitkarlsruhe.consaltingmachine.datastructures.CSensorData;
import de.hitkarlsruhe.consaltingmachine.datastructures.EInstructions;
import de.hitkarlsruhe.consaltingmachine.ui.main.tablayout.CMainActPagerAdapter;
import de.hitkarlsruhe.consaltingmachine.ui.main.tablayout.IFragmentActions;

public class CFSMActionsConnection {
    // object of activity, helps to run methods on UI thread
    CMainActivity mActivity;
    Context mContext;

    // object of PagerAdapter, for accessing UI
    CMainActPagerAdapter mPagerAdapter;

    // interface for updating UI
    IFragmentActions mFragmentActions;

    // object of finite state machine
    CFSMConnection mFSM;

    // Variables for bluetooth scan
    private final int MAX_SCAN_DELAY = 10000;   // maximum delay
    private final String bleScanString = "ConSALTing Machine";  // bluetooth search string
    // uuids for service and characteristics
    private final String SERVICE_UUID = "1ae5a3bc-bd4f-4440-ae95-b4a0d576313f";
    private final String CHARACTERISTIC_SENSORDATA_UUID = "2bdedda6-0dce-4c9e-b2bd-383633f7d38a";
    private final String CHARACTERISTIC_MACHINE_CONTROL_UUID = "b96ebc3f-23ed-4de6-b6e4-092230831a32";
    private boolean scanning;                   // boolean variable contains scanning state
    private Handler handler = new Handler();    // stop scanning after MAX_SCAN_DELAY
    BluetoothAdapter blAdapter;          // Get Object of Bluetooth-Adapter
    BluetoothLeScanner scanner;                 // scanner object for starting and stopping scan
    BluetoothDevice mDevice;                    // Object for ConSALTing Machine
    BluetoothGatt bleGatt;                      // Object for Gatt ConSALTing Machine
    BluetoothGattCharacteristic mCharacteristic_SensorData;     // BLE-Characteristic for receiving SensorData
    BluetoothGattCharacteristic mCharacteristic_MachineControl; // BLE-Characteristic for sending machine control events

    // data structures for bluetooth communication
    CSensorData mSensorData;
    CMachineControlData mMachineControlData;

    // object for closing dialogs from CMainActivity
    public AlertDialog mDialog;

    // variable for storing current target salt concentration
    float mTargetSaltConcentration;
    float tempSaltAmount;

    public CFSMActionsConnection(Context pContext, CFSMConnection pFSM, CMainActivity pActivity, CMainActPagerAdapter pPagerAdapter) {
        mContext = pContext;
        mFSM = pFSM;
        mActivity = pActivity;
        mPagerAdapter = pPagerAdapter;
        blAdapter = BluetoothAdapter.getDefaultAdapter();

        // initialize target salt concentration variable
        mTargetSaltConcentration = -1.0f;
        tempSaltAmount = 0.0f;

        // check that bluetooth is available on the device
        if(blAdapter == null) {
            // print error message, stop app
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            builder.setTitle("Bluetooth nicht verfügbar");
            builder.setMessage("Bluetooth ist auf diesem Gerät nicht verfügbar!"
                    + " Diese App kann daher nicht verwendet werden");
            builder.setPositiveButton("App beenden", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    mDialog = null;
                    mActivity.finish();
                }
            });
            builder.setCancelable(false);
            builder.create().show();
            mDialog = builder.create();
            mDialog.show();
        }
    }

    void entryStateDisconnected() {
        // Update UI FragmentConfig
        mPagerAdapter.mFragmConfig.mTVConnectionStateText = "getrennt";
        mPagerAdapter.mFragmConfig.mTVConnectionStateColor = 0xFFFF0000;

        // Update UI FragmentSensorData
        mPagerAdapter.mFragmSensorData.mTVConnectionStateText = "getrennt";
        mPagerAdapter.mFragmSensorData.mTVConnectionStateColor = 0xFFFF0000;
        mPagerAdapter.mFragmSensorData.mTVSaltAmountText = "---";
        mPagerAdapter.mFragmSensorData.mTVPressureSensorText = "---";

        // notify UI update
        ((IFragmentActions)mPagerAdapter.mFragmConfig).notifyLayoutUpdate(mContext);
        ((IFragmentActions)mPagerAdapter.mFragmSensorData).notifyLayoutUpdate(mContext);
        ((IFragmentActions)mPagerAdapter.mFragmInfo).notifyLayoutUpdate(mContext);
    }

    void exitStateDisconnected() {

    }
    void entryStateSearching() {
        // Update UI FragmentConfig
        mPagerAdapter.mFragmConfig.mTVConnectionStateText = "Suche läuft...";
        mPagerAdapter.mFragmConfig.mTVConnectionStateColor = 0xFF0000FF;

        // Update UI FragmentSensorData
        mPagerAdapter.mFragmSensorData.mTVConnectionStateText = "Suche läuft...";
        mPagerAdapter.mFragmSensorData.mTVConnectionStateColor = 0xFF0000FF;
        mPagerAdapter.mFragmSensorData.mTVSaltAmountText = "---";
        mPagerAdapter.mFragmSensorData.mTVPressureSensorText = "---";

        // notify UI update
        ((IFragmentActions)mPagerAdapter.mFragmConfig).notifyLayoutUpdate(mContext);
        ((IFragmentActions)mPagerAdapter.mFragmSensorData).notifyLayoutUpdate(mContext);
        ((IFragmentActions)mPagerAdapter.mFragmInfo).notifyLayoutUpdate(mContext);

        // start searching
        startScanning();
    }
    void exitStateSearching() {
        // check that search is stopped
        if(scanning) {
            scanner.stopScan(leScanCallback);
            scanning = false;
        }
    }
    void entryStateConnecting() {
        // Update UI FragmentConfig
        mPagerAdapter.mFragmConfig.mTVConnectionStateText = "verbinden...";
        mPagerAdapter.mFragmConfig.mTVConnectionStateColor = 0xFF0000FF;

        // Update UI FragmentSensorData
        mPagerAdapter.mFragmSensorData.mTVConnectionStateText = "verbinden...";
        mPagerAdapter.mFragmSensorData.mTVConnectionStateColor = 0xFF0000FF;
        mPagerAdapter.mFragmSensorData.mTVSaltAmountText = "---";
        mPagerAdapter.mFragmSensorData.mTVPressureSensorText = "---";

        // notify UI update
        ((IFragmentActions)mPagerAdapter.mFragmConfig).notifyLayoutUpdate(mContext);
        ((IFragmentActions)mPagerAdapter.mFragmSensorData).notifyLayoutUpdate(mContext);
        ((IFragmentActions)mPagerAdapter.mFragmInfo).notifyLayoutUpdate(mContext);

        // connect to GATT
        connectToGatt();
    }
    void exitStateConnecting(boolean closeConnection) {
        if(closeConnection) {
            closeConnection();
        }
    }
    void entryStateConnected() {
        // Update UI FragmentConfig
        mPagerAdapter.mFragmConfig.mTVConnectionStateText = "verbunden";
        mPagerAdapter.mFragmConfig.mTVConnectionStateColor = 0xFF00A05A;

        // Update UI FragmentSensorData
        mPagerAdapter.mFragmSensorData.mTVConnectionStateText = "verbunden";
        mPagerAdapter.mFragmSensorData.mTVConnectionStateColor = 0xFF00A05A;

        // notify UI update
        ((IFragmentActions)mPagerAdapter.mFragmConfig).notifyLayoutUpdate(mContext);
        ((IFragmentActions)mPagerAdapter.mFragmSensorData).notifyLayoutUpdate(mContext);
        ((IFragmentActions)mPagerAdapter.mFragmInfo).notifyLayoutUpdate(mContext);

        mFSM.dispatchEvent(EEvents.UI_EVENT_UPDATE_SENSOR_DATA, mContext);
    }
    void exitStateConnected() {
        // disconnect always
        closeConnection();
    }

    void handleBLESearchTimeout() {
        // print error message
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle("ConSALTingMachine nicht gefunden");
        builder.setMessage("Die ConSALTingMachine wurde nicht gefunden. Stelle sicher," +
                        " dass das Gerät eingeschaltet ist, sich in der Nähe befindet" +
                        " und der Akku ausreichend geladen ist.");
        builder.setPositiveButton("Erneut versuchen", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // check again
                dialog.dismiss();
                mDialog = null;
                mFSM.dispatchEvent(EEvents.BLE_BEGIN_SEARCH, mContext);
            }
        });
        builder.setNegativeButton("Abbrechen", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // maintain disconnected state
                dialog.dismiss();
                mDialog = null;
            }
        });
        builder.setCancelable(false);
        mDialog = builder.create();
        mDialog.show();
    }

    void handleBLEConnectingError() {
        // print error message
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle("BLE-Verbindungsfehler");
        builder.setMessage("Verbindungsfehler: erforderliche BLE-Characteristics nicht gefunden.");
        builder.setPositiveButton("Erneut versuchen", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // close dialog
                dialog.dismiss();
                mDialog = null;
                // remove current device
                mDevice = null;

                mFSM.dispatchEvent(EEvents.BLE_BEGIN_SEARCH, mContext);
            }
        });
        builder.setNegativeButton("Abbrechen", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // maintain disconnected state
                dialog.dismiss();
                mDialog = null;
            }
        });
        builder.setCancelable(false);
        mDialog = builder.create();
        mDialog.show();
    }

     void handleBLEConnectionErrorConnected() {
         // print error message
         AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
         builder.setTitle("BLE");
         builder.setMessage("Verbindungsfehler");
         builder.setPositiveButton("Erneut verbinden", new DialogInterface.OnClickListener() {
             @Override
             public void onClick(DialogInterface dialog, int which) {
                 // close dialog
                 dialog.dismiss();
                 mDialog = null;
                 // remove current device
                 mDevice = null;

                 mFSM.dispatchEvent(EEvents.BLE_BEGIN_SEARCH, mContext);
             }
         });
         builder.setNegativeButton("Abbrechen", new DialogInterface.OnClickListener() {
             @Override
             public void onClick(DialogInterface dialog, int which) {
                 // maintain disconnected state
                 dialog.dismiss();
                 mDialog = null;
             }
         });
         builder.setCancelable(false);
         mDialog = builder.create();
         mDialog.show();
     }

     void handleConfigurationUpdate() {
         // transmit new settings
         CMachineControlData mCtrlData = ((IFragmentActions)mPagerAdapter.mFragmConfig).requestSaltAmount();
         tempSaltAmount = mCtrlData.mSaltConcentration;
         requestTransmitMachineControlData(mCtrlData);
     }

     void showDialogConfirmResetSaltAmount() {
         AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
         builder.setTitle("Salzfüllstand zurücksetzen");
         builder.setMessage("Möchtest du wirklich den Salzfüllstand zurücksetzen?");
         builder.setPositiveButton("Weiter...", new DialogInterface.OnClickListener() {
             @Override
             public void onClick(DialogInterface dialog, int which) {
                 // close dialog
                 dialog.dismiss();
                 mDialog = null;

                 // transmit instruction to ConSALTingMachine
                 requestTransmitMachineControlData(new CMachineControlData(0.0f, EInstructions.RESET_SALT_AMOUNT));
             }
         });
         builder.setNegativeButton("Abbrechen", new DialogInterface.OnClickListener() {
             @Override
             public void onClick(DialogInterface dialog, int which) {
                 // maintain disconnected state
                 dialog.dismiss();
                 mDialog = null;
             }
         });
         builder.setCancelable(false);
         mDialog = builder.create();
         mDialog.show();
     }

     void handleSensorDataReceived() {
         // round values and update UI FragmentSensorData
         DecimalFormat df = new DecimalFormat("#.#");
         df.setRoundingMode(RoundingMode.HALF_UP);
         mPagerAdapter.mFragmSensorData.mTVSaltAmountText = df.format(mSensorData.mSaltAmount);
         mPagerAdapter.mFragmSensorData.mTVPressureSensorText = df.format(mSensorData.mPressure);

         // notify UI Update
         ((IFragmentActions)mPagerAdapter.mFragmSensorData).notifyLayoutUpdate(mContext);
     }

     void handleMachineControlDataTransmittedSuccessful() {
         Toast.makeText(mContext, "Übertragung erfolgreich!", Toast.LENGTH_SHORT).show();
     }

    void handleMachineControlDataTransmissionError() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle("Übertragungsfehler");
        builder.setMessage("Die Daten konnten nicht übertragen werden. Erneut versuchen?");
        builder.setPositiveButton("Weiter...", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // close dialog
                dialog.dismiss();
                mDialog = null;

                // transmit instruction to ConSALTingMachine
                handleConfigurationUpdate();
            }
        });
        builder.setNegativeButton("Abbrechen", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // maintain disconnected state
                dialog.dismiss();
                mDialog = null;
            }
        });
        builder.setCancelable(false);
        mDialog = builder.create();
        mDialog.show();
    }

    // ==============================================
    // BLUETOOTH FUNCTIONS
    // ==============================================

    public boolean startScanning() {
        // check if Bluetooth Adapter is available
        if(blAdapter != null) {
            // Get BluetoothLEScanner-Object
            scanner = blAdapter.getBluetoothLeScanner();
            if (scanner == null) {
                return false;
            } else {
                // clear existing device object
                mDevice = null;

                // start scan
                scanBLEDevice();
                Log.d("ConSALTingMachine", "BLE-Scan started");
                return true;
            }
        }
        else {
            return false;
        }
    }

    // start scan for BLE Devices
    // !!! Object mDevice has to be cleared before: mDevice = null; !!!
    private void scanBLEDevice() {
        if(!scanning) {
            // define handler to stop scan after maximum delay
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    // maximum delay is reached, stop scan if scanner is still active
                    if(scanning) {
                        scanning = false;
                        scanner.stopScan(leScanCallback);

                        // give event from UI thread
                        mActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mFSM.dispatchEvent(EEvents.BLE_SEARCH_TIMEOUT, mContext);
                            }
                        });
                    }
                }
            }, MAX_SCAN_DELAY);

            // start scan only if device is not found before
            if(mDevice == null) {
                scanning = true;
                Log.d("ConSALTingMachine", "Device == null -> Scan started");
                scanner.startScan(leScanCallback);
            }
        }
        else {
            // scanner is active; stop scan
            scanning = false;
            scanner.stopScan(leScanCallback);
        }
    }

    // scan callback, is executed if device was found
    private ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

            // check for valid device name and print device name
            if(result.getDevice() != null && result.getDevice().getUuids() != null) {
                Log.d("ConSALTingMachine", "Bluetooth device found: " + result.getDevice().getUuids().toString());
            }

            // check for Device name ConSALTing Machine
            if(result.getDevice() != null && result.getDevice().getName() != null && result.getDevice().getName().compareTo(bleScanString) == 0) {
                // Device found: store Device Object
                mDevice = result.getDevice();

                // stop scan
                scanning = false;
                scanner.stopScan(leScanCallback);

                // give event from UI-thread
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mFSM.dispatchEvent(EEvents.BLE_DEVICE_FOUND, mContext);
                    }
                });
            }
        }
    };

    public boolean connectToGatt() {
        // declare GATT Callback
        final BluetoothGattCallback bleGattCallback = new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);
                if(newState == BluetoothProfile.STATE_CONNECTED) {
                    // connected to GATT Server ==> request gatt services and give BLE_DEVICE_CONNECTED
                    // event from services discovered callback

                    bleGatt.discoverServices();
                }
                else if(newState == BluetoothProfile.STATE_DISCONNECTED) {
                    // disconnected from the GATT Server ==> give new event from UI Thread
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mFSM.dispatchEvent(EEvents.BLE_DEVICE_DISCONNECTED, mContext);
                        }
                    });
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);
                mCharacteristic_SensorData = null;
                mCharacteristic_MachineControl = null;
                if(status == BluetoothGatt.GATT_SUCCESS) {
                    for(BluetoothGattService mService : bleGatt.getServices()) {
                        if(mService.getUuid().toString().compareTo(SERVICE_UUID) == 0) {
                            for(BluetoothGattCharacteristic mLoopCharacteristic : mService.getCharacteristics()) {
                                if(mLoopCharacteristic.getUuid().toString().compareTo(CHARACTERISTIC_SENSORDATA_UUID) == 0) {
                                    Log.d("ConSALTingMachine", "SensorData Characteristic found!");
                                    mCharacteristic_SensorData = mLoopCharacteristic;
                                }
                                if(mLoopCharacteristic.getUuid().toString().compareTo(CHARACTERISTIC_MACHINE_CONTROL_UUID) == 0) {
                                    Log.d("ConSALTingMachine", "MachineControl Characteristic found!");
                                    mCharacteristic_MachineControl = mLoopCharacteristic;
                                }
                                // exit from for loop if both characteristics were found
                                if(mCharacteristic_SensorData != null && mCharacteristic_MachineControl != null) {
                                    break;
                                }
                            }
                            // exit from for loop if both characteristics were found
                            if(mCharacteristic_SensorData != null && mCharacteristic_MachineControl != null) {

                                // both characteristics found ==> give new event from UI Thread
                                mActivity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        mFSM.dispatchEvent(EEvents.BLE_DEVICE_CONNECTED, mContext);
                                    }
                                });
                                break;
                            }
                        }
                    }

                    // check for both characteristics found, otherwise give error event
                    if(mCharacteristic_MachineControl == null || mCharacteristic_SensorData == null) {
                        mActivity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mFSM.dispatchEvent(EEvents.BLE_DEVICE_CONNECTION_ERROR, mContext);
                            }
                        });
                    }

                    // request notifications from ConSALTingMachine
                    // -- not required anymore --
                    // bleGatt.setCharacteristicNotification(mCharacteristic_SensorData, true);
                }
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicRead(gatt, characteristic, status);
                byte[] mSensorValues = characteristic.getValue();

                // update sensor data object
                mSensorData = null;
                mSensorData = CSensorData.createInstanceFromByteArray(mSensorValues);

                // give event from UI Thread
                mActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mFSM.dispatchEvent(EEvents.SENSOR_DATA_RECEIVED, mContext);
                    }
                });
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicWrite(gatt, characteristic, status);
                if (characteristic.getUuid().toString().compareTo(CHARACTERISTIC_MACHINE_CONTROL_UUID) == 0) {
                    switch (status) {
                        case BluetoothGatt.GATT_SUCCESS:
                            // give successful event from UI Thread
                            mActivity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    // update target salt concentration variable
                                    if(mFSM.mPreviousMachineControlEvent == EEvents.UI_EVENT_CONFIGURATION_UPDATE) {
                                        mTargetSaltConcentration = tempSaltAmount;
                                    }

                                    // give event
                                    mFSM.dispatchEvent(EEvents.MACHINE_CONTROL_DATA_TRANSMITTED_SUCCESSFUL, mContext);
                                }
                            });
                            break;
                        case BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH:
                            mActivity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mFSM.dispatchEvent(EEvents.MACHINE_CONTROL_DATA_TRANSMISSION_ERROR, mContext);
                                    Log.e("ConSALTingMachine", "GATT_INVALID_ATTRIBUTE_LENGTH");
                                }
                            });
                            break;
                        case BluetoothGatt.GATT_WRITE_NOT_PERMITTED:
                            mActivity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mFSM.dispatchEvent(EEvents.MACHINE_CONTROL_DATA_TRANSMISSION_ERROR, mContext);
                                    Log.e("ConSALTingMachine", "GATT_WRITE_NOT_PERMITTED");
                                }
                            });
                            break;
                        default:
                            mActivity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mFSM.dispatchEvent(EEvents.MACHINE_CONTROL_DATA_TRANSMISSION_ERROR, mContext);
                                    Log.e("ConSALTingMachine", "Error write operation: " + Integer.toString(status));
                                }
                            });
                            break;
                    }
                }
            }
        };

        // connect to GATT Server
        bleGatt = mDevice.connectGatt(mContext, false, bleGattCallback);
        return true;
    }

    public boolean requestReadSensorData() {
        if(bleGatt != null && mCharacteristic_SensorData != null) {
            bleGatt.readCharacteristic(mCharacteristic_SensorData);
            return true;
        }
        else {
            return false;
        }
    }

    public void requestTransmitMachineControlData(CMachineControlData pMachineControlData) {
        mCharacteristic_MachineControl.setValue(pMachineControlData.convertToByteArray());
        mCharacteristic_MachineControl.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
        if(bleGatt != null) {
            bleGatt.writeCharacteristic(mCharacteristic_MachineControl);
        }
        else {
            // give connection error event
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mFSM.dispatchEvent(EEvents.BLE_DEVICE_CONNECTION_ERROR, mContext);
                }
            });
        }
    }

    public void closeConnection() {
        if(bleGatt != null) {
            bleGatt.close();
            bleGatt = null;
        }

        // new event will be generated if callback method for ConnectionStateChange is called
    }

    private List<BluetoothGattService> getGattServices() {
        // get Gatt Services
        if(bleGatt == null) {
            return null;
        }
        else {
            return bleGatt.getServices();
        }
    }

    // ==============================================
    // FUNCTIONS FOR TARGET CONFIGURATION FSM
    // ==============================================

    void entryTargetConfigUpdated() {
        // update layout
        mPagerAdapter.mFragmConfig.mTVTargetConfigurationText = "aktuell";
        mPagerAdapter.mFragmConfig.mTVTargetConfigurationColor = 0xFF00A05A;
        // notify UI update
        ((IFragmentActions)mPagerAdapter.mFragmConfig).notifyLayoutUpdate(mContext);
    }
    void entryTargetConfigNotUpdated() {
        // update layout
        mPagerAdapter.mFragmConfig.mTVTargetConfigurationText = "nicht aktuell";
        mPagerAdapter.mFragmConfig.mTVTargetConfigurationColor = 0xFFFF0000;

        // notify UI update
        ((IFragmentActions)mPagerAdapter.mFragmConfig).notifyLayoutUpdate(mContext);
    }

    boolean handleUIChangedCheckSaltUpdated() {
        // check current configuration
        if(((IFragmentActions)mPagerAdapter.mFragmConfig).
                requestSaltAmount().mSaltConcentration == mTargetSaltConcentration) {
            return true;
        } else {
            return false;
        }
    }
}
