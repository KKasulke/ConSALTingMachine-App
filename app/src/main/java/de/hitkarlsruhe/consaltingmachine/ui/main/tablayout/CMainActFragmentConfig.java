package de.hitkarlsruhe.consaltingmachine.ui.main.tablayout;

import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.material.slider.Slider;

import java.util.ArrayList;
import java.util.List;

import de.hitkarlsruhe.consaltingmachine.CMainActivity;
import de.hitkarlsruhe.consaltingmachine.EEvents;
import de.hitkarlsruhe.consaltingmachine.IBluetoothActivityActions;
import de.hitkarlsruhe.consaltingmachine.R;
import de.hitkarlsruhe.consaltingmachine.datastructures.CMachineControlData;
import de.hitkarlsruhe.consaltingmachine.datastructures.CMeal;
import de.hitkarlsruhe.consaltingmachine.datastructures.EInstructions;

public class CMainActFragmentConfig extends Fragment implements IBluetoothFragmentActions {
    // Salt offset which is multiplied with slider value to increase or decrease desired salt amount
    public final float SALT_OFFSET = 5.0f;

    // variables which help to set the layout if the UI elements are not initialized
    public String mTVConnectionStateText;
    public String mTVTargetConfigurationText;
    public int mTVConnectionStateColor;
    public int mTVTargetConfigurationColor;

    // declare UI variables
    private TextView mTVConnectionState;
    private ImageButton mBConnection;
    private TextView mTVTargetConfiguration;
    private ImageButton mBSynchronize;
    private Spinner mSpinner;
    private TextView mTVSaltConcState;
    private Slider mSlider;
    private Button mBResetSaltAmount;

    // adapters for spinner
    ArrayList<String> mMealListStrings;
    ArrayAdapter<String> mSpinnerArrayAdapter;
    CMeal[] mMealList;

    // interface which allows access of CMainActivity
    IBluetoothActivityActions mActivityActions;

    public CMainActFragmentConfig() {
        // Required empty public constructor
    }

    // factory method to create a new instance
    public static CMainActFragmentConfig newInstance() {
        CMainActFragmentConfig fragment = new CMainActFragmentConfig();
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
        if(mMealListStrings == null && (savedInstanceState == null || savedInstanceState.getStringArrayList("mealArrayListString") == null)) {
            mMealListStrings = new ArrayList<String>();
            mMealList = null;
        }
        else if(mMealListStrings == null ) {
            mMealListStrings = savedInstanceState.getStringArrayList("mealArrayListString");
            mMealList = (CMeal[]) savedInstanceState.getParcelableArray("mealArray");
        }

        if(mTVConnectionStateText == null || mTVConnectionStateText.compareTo("") == 0) {
            mTVConnectionStateText = "getrennt";
            mTVConnectionStateColor = 0xFFFF0000;
        }
        if(mTVTargetConfigurationText == null || mTVTargetConfigurationText.compareTo("") == 0) {
            mTVTargetConfigurationText = "nicht aktuell";
            mTVTargetConfigurationColor = 0xFFFF0000;
        }
    }

    // inflate layout
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d("ConSALTingMachine", "FragmentConfig View created.");

        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragm_act_main_config, container, false);

        // DEFINE UI ELEMENTS
        // textviews
        mTVConnectionState = v.findViewById(R.id.fragm_act_main_config_TVDeviceCOMContent);
        mTVConnectionState.setText(mTVConnectionStateText);
        mTVConnectionState.setTextColor(mTVConnectionStateColor);
        mTVTargetConfiguration = v.findViewById(R.id.fragm_act_main_config_TVConfigContent);
        mTVTargetConfiguration.setText(mTVTargetConfigurationText);
        mTVTargetConfiguration.setTextColor(mTVTargetConfigurationColor);
        mTVSaltConcState = v.findViewById(R.id.fragm_act_main_config_TVChangeSaltConcContent);

        // buttons
        mBConnection = v.findViewById(R.id.fragm_act_main_config_BSearch);
        mBSynchronize = v.findViewById(R.id.fragm_act_main_config_BSynchronize);
        mBResetSaltAmount = v.findViewById(R.id.fragm_act_main_config_BResetSalt);

        // spinner
        mSpinner = v.findViewById(R.id.fragm_act_main_config_SpSelMeal);
        mSpinnerArrayAdapter = new ArrayAdapter<String>(getContext(), R.layout.spinner_layout, mMealListStrings);
        mSpinnerArrayAdapter.setDropDownViewResource(R.layout.spinner_layout);
        mSpinner.setAdapter(mSpinnerArrayAdapter);
        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mActivityActions.passEvent(EEvents.UI_EVENT_TARGET_CONFIGURATION_CHANGED);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        mSlider = v.findViewById(R.id.fragm_act_main_config_SLChangeSaltConc);
        mSlider.addOnChangeListener(new Slider.OnChangeListener() {
            @Override
            public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
                if(value < -1.5f) {
                    mTVSaltConcState.setText("sehr gering");
                } else if (mSlider.getValue() < -0.5f) {
                    mTVSaltConcState.setText("verringert");
                } else if(mSlider.getValue() < 0.5f) {
                    mTVSaltConcState.setText("normal");
                } else if(mSlider.getValue() < 1.5f) {
                    mTVSaltConcState.setText("erhöht");
                } else {
                    mTVSaltConcState.setText("stark");
                }

                // give event to fsm
                mActivityActions.passEvent(EEvents.UI_EVENT_TARGET_CONFIGURATION_CHANGED);
            }
        });

        // Listener for buttons
        mBConnection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mActivityActions.passEvent(EEvents.BLE_BEGIN_SEARCH);
            }
        });
        mBSynchronize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mActivityActions.passEvent(EEvents.UI_EVENT_CONFIGURATION_UPDATE);
            }
        });
        mBResetSaltAmount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mActivityActions.passEvent(EEvents.UI_EVENT_RESET_SALT_AMOUNT);
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
        // check if object is already created
        if(mMealListStrings != null) {
            // remove all previous elements
            mMealListStrings.clear();
        }
        // mMealList is not created yet ==> create new empty array list
        else {
            mMealListStrings = new ArrayList<>();
        }

        // Update ArrayList
        for (CMeal mMeal : pMeal) {
            mMealListStrings.add(mMeal.mName);
        }

        // update meal variable
        mMealList = pMeal;

        // notify list adapter and update UI if UI is already initialized
        if(mSpinnerArrayAdapter != null) {
            mSpinnerArrayAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void notifyLayoutUpdate(Context pContext) {
        if(mTVConnectionState != null && mTVTargetConfiguration != null) {
            mTVConnectionState.setText(mTVConnectionStateText);
            mTVConnectionState.setTextColor(mTVConnectionStateColor);
            mTVTargetConfiguration.setText(mTVTargetConfigurationText);
            mTVTargetConfiguration.setTextColor(mTVTargetConfigurationColor);
        }
    }

    @Override
    public CMachineControlData requestSaltAmount() {
        if(mSlider != null && mSpinnerArrayAdapter != null) {
            // compute desired salt amount
            float saltAmount = mMealList[mSpinner.getSelectedItemPosition()].mSaltAmount
                    + mSlider.getValue()*SALT_OFFSET;

            // set salt amount to 0 if it is negative
            if(saltAmount < 0.0f) {
                saltAmount = 0.0f;
            }

            return new CMachineControlData(saltAmount, EInstructions.SET_SALT_CONCENTRATION);
        }
        else return null;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if(mMealListStrings != null && mMealListStrings.size() > 0) {
            outState.putStringArrayList("mealArrayListString", mMealListStrings);
            outState.putParcelableArray("mealArray", mMealList);
        }
    }
}