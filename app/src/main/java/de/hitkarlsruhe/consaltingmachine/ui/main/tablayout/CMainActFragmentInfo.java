package de.hitkarlsruhe.consaltingmachine.ui.main.tablayout;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import de.hitkarlsruhe.consaltingmachine.CMainActivity;
import de.hitkarlsruhe.consaltingmachine.IBluetoothActivityActions;
import de.hitkarlsruhe.consaltingmachine.R;
import de.hitkarlsruhe.consaltingmachine.datastructures.CMachineControlData;
import de.hitkarlsruhe.consaltingmachine.datastructures.CMeal;
import de.hitkarlsruhe.consaltingmachine.datastructures.CSensorData;

public class CMainActFragmentInfo extends Fragment implements IBluetoothFragmentActions {
    // ui variables
    private TextView tvInfo;

    public CMainActFragmentInfo() {
        // Required empty public constructor
    }

    // factory method to create a new instance
    public static CMainActFragmentInfo newInstance() {
        CMainActFragmentInfo fragment = new CMainActFragmentInfo();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
    }

    @Override
    public void onResume() {
        super.onResume();
        notifyLayoutUpdate(getContext());
    }

    // inflate layout
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragm_act_main_info, container, false);

        // create ui objects
        tvInfo = view.findViewById(R.id.fragm_act_main_info_TVInfo);

        return view;
    }

    @Override
    public void notifySpinnerUpdate(Context pContext, CMeal[] pMeal) {
    }

    @Override
    public void notifyLayoutUpdate(Context pContext) {
    }

    @Override
    public CMachineControlData requestSaltAmount() {
        return null;
    }
}