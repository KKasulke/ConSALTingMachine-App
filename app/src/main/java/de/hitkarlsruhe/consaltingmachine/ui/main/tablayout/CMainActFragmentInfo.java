package de.hitkarlsruhe.consaltingmachine.ui.main.tablayout;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import de.hitkarlsruhe.consaltingmachine.R;
import de.hitkarlsruhe.consaltingmachine.datastructures.CMachineControlData;
import de.hitkarlsruhe.consaltingmachine.datastructures.CMeal;

public class CMainActFragmentInfo extends Fragment implements IFragmentActions {
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