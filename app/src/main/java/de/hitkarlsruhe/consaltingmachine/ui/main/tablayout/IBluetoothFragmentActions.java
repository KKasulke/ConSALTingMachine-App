package de.hitkarlsruhe.consaltingmachine.ui.main.tablayout;

import android.bluetooth.BluetoothGattService;
import android.content.Context;

import java.util.List;

import de.hitkarlsruhe.consaltingmachine.datastructures.CMachineControlData;
import de.hitkarlsruhe.consaltingmachine.datastructures.CMeal;

public interface IBluetoothFragmentActions {
    void notifySpinnerUpdate(Context pContext, CMeal[] pMeal);
    void notifyLayoutUpdate(Context pContext);
    CMachineControlData requestSaltAmount();
}
