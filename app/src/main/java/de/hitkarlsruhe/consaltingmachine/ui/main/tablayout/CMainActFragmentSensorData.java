package de.hitkarlsruhe.consaltingmachine.ui.main.tablayout;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import de.hitkarlsruhe.consaltingmachine.CMainActivity;
import de.hitkarlsruhe.consaltingmachine.EEvents;
import de.hitkarlsruhe.consaltingmachine.IBluetoothActivityActions;
import de.hitkarlsruhe.consaltingmachine.R;
import de.hitkarlsruhe.consaltingmachine.datastructures.CMachineControlData;
import de.hitkarlsruhe.consaltingmachine.datastructures.CMeal;

public class CMainActFragmentSensorData extends Fragment implements IBluetoothFragmentActions {
    // variables which help to set the layout if the UI elements are not initialized
    public String mTVConnectionStateText;
    public int mTVConnectionStateColor;
    public String mTVBatteryLevelText;
    public String mTVSaltAmountText;
    public String mTVPressureSensorText;

    private ImageButton mBConnection;
    private TextView mTVConnectionState;
    private TextView mTVBatteryLevel;
    private TextView mTVSaltAmount;
    private TextView mTVPressureSensor;
    private Button mBUpdateSensorData;

    // interface which allows to access CMainActivity
    IBluetoothActivityActions mActivityActions;

    public CMainActFragmentSensorData() {
        // Required empty public constructor
    }

    // factory method to create a new instance
    public static CMainActFragmentSensorData newInstance() {
        CMainActFragmentSensorData fragment = new CMainActFragmentSensorData();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mActivityActions = ((CMainActivity)context);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(mTVConnectionStateText == null || mTVConnectionStateText.compareTo("") == 0) {
            mTVConnectionStateText = "getrennt";
            mTVConnectionStateColor = 0xFFFF0000;
        }
        if(mTVBatteryLevelText == null || mTVBatteryLevelText.compareTo("") == 0) {
            mTVBatteryLevelText = "---";
        }
        if(mTVSaltAmountText == null || mTVSaltAmountText.compareTo("") == 0) {
            mTVSaltAmountText = "---";
        }
        if(mTVPressureSensorText == null || mTVPressureSensorText.compareTo("") == 0) {
            mTVPressureSensorText = "---";
        }
    }

    // inflate layout
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragm_act_main_sensordata, container, false);

        // Initialize UI
        mBConnection = v.findViewById(R.id.fragm_act_main_sensordata_BSynchronize);
        mTVConnectionState = v.findViewById(R.id.fragm_act_main_sensordata_TVDeviceCOMContent);
        mTVBatteryLevel = v.findViewById(R.id.fragm_act_main_sensordata_TVbatteryContent);
        mTVSaltAmount = v.findViewById(R.id.fragm_act_main_sensordata_TVSaltAmountContent);
        mTVPressureSensor = v.findViewById(R.id.fragm_act_main_sensordata_TVPressureContent);
        mBUpdateSensorData = v.findViewById(R.id.fragm_act_main_sensordata_BReqData);

        mBConnection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mActivityActions.passEvent(EEvents.BLE_BEGIN_SEARCH);
            }
        });

        mBUpdateSensorData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mActivityActions.passEvent(EEvents.UI_EVENT_UPDATE_SENSOR_DATA);
            }
        });

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        notifyLayoutUpdate(getContext());
    }

    @Override
    public void notifySpinnerUpdate(Context pContext, CMeal[] pMeal) {
        // method is not required in this fragment
    }

    @Override
    public void notifyLayoutUpdate(Context pContext) {
        if(mTVConnectionState != null
                && mTVBatteryLevel != null
                && mTVSaltAmount != null
                && mTVPressureSensor != null) {
            mTVConnectionState.setText(mTVConnectionStateText);
            mTVConnectionState.setTextColor(mTVConnectionStateColor);
            mTVBatteryLevel.setText(mTVBatteryLevelText);
            mTVSaltAmount.setText(mTVSaltAmountText);
            mTVPressureSensor.setText(mTVPressureSensorText);
        }
    }

    @Override
    public CMachineControlData requestSaltAmount() {
        return null;
    }
}