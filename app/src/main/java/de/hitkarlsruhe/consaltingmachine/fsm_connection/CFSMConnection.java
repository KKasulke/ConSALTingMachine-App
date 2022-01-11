package de.hitkarlsruhe.consaltingmachine.fsm_connection;

import android.content.Context;
import android.util.Log;

import de.hitkarlsruhe.consaltingmachine.CMainActivity;
import de.hitkarlsruhe.consaltingmachine.EEvents;
import de.hitkarlsruhe.consaltingmachine.ui.main.tablayout.CMainActPagerAdapter;

public class CFSMConnection {
    // state variables
    EConnectionStates mConnState;
    ETargetConfigurationStates mTargetConfigState;
    EEvents mNextActionIfConnected;

    // variable for tracking machine control events
    // (necessary to handle events in target configuration fsm)
    public EEvents mPreviousMachineControlEvent;

    // Action-class (must be accessible from CMainActivity for closing dialogs)
    public CFSMActionsConnection mActions;

    // constructor
    public CFSMConnection(CMainActivity pActivity, CMainActPagerAdapter pPagerAdapter, Context mContext) {
        Log.d("ConSALTingMachine", "FSM constructor");
        mConnState = EConnectionStates.DISCONNECTED;
        mTargetConfigState = ETargetConfigurationStates.NOT_UPDATED;
        mPreviousMachineControlEvent = EEvents.DEFAULT_IGNORE;
        mNextActionIfConnected = null;
        mActions = new CFSMActionsConnection(mContext,this, pActivity, pPagerAdapter);
        mActions.entryStateDisconnected();
    }

    public void dispatchEvent(EEvents mEvent, Context mContext) {
        Log.d("ConSALTingMachine", "New Event received: " + mEvent.toString() +
                "; STATE = " + mConnState.toString());
        switch (mConnState) {
            case DISCONNECTED:
                onDisconnected(mEvent, mContext);
                break;
            case SEARCHING:
                onSearching(mEvent, mContext);
                break;
            case CONNECTING:
                onConnecting(mEvent, mContext);
                break;
            case CONNECTED:
                onConnected(mEvent, mContext);
                break;
            default:
                // switch back to default state
                mConnState = EConnectionStates.DISCONNECTED;
                onDisconnected(mEvent, mContext);
                break;
        }

        switch (mTargetConfigState) {
            case NOT_UPDATED:
                onTargetConfigNotUpdated(mEvent);
                break;
            case UPDATED:
                onTargetConfigUpdated(mEvent);
                break;
            default:
                // switch back to default state
                mTargetConfigState = ETargetConfigurationStates.NOT_UPDATED;
                break;
        }
    }

    private void onDisconnected(EEvents mEvent, Context mContext) {
        switch (mEvent) {
            case BLE_BEGIN_SEARCH:
                // change state
                mActions.exitStateDisconnected();

                // if device is not found yet, switch to searching state
                if(mActions.mDevice == null) {
                    mConnState = EConnectionStates.SEARCHING;
                    mActions.entryStateSearching();
                } else {    // device is already found, switch to connecting state
                    mConnState = EConnectionStates.CONNECTING;
                    mActions.entryStateConnecting();
                }
                return;
            case BLE_DEVICE_CONNECTION_ERROR:
                // show dialog and ask user to try to connect again, if connection
                // was interrupted in connected state
                mActions.handleBLEConnectionErrorConnected();
                return;
            case UI_EVENT_CONFIGURATION_UPDATE:
            case UI_EVENT_UPDATE_SENSOR_DATA:
            case UI_EVENT_RESET_SALT_AMOUNT:
            case ON_RESUME:
                // events can not be processed in Disconnected-State
                // add event to nextAction and try to connect
                mNextActionIfConnected = mEvent;
                dispatchEvent(EEvents.BLE_BEGIN_SEARCH, mContext);
                return;
            default:
                return;
        }
    }

    private void onSearching(EEvents mEvent, Context mContext) {
        switch (mEvent) {
            case ON_PAUSE:
                // stop scanning and switch to disconnect state
                mActions.exitStateSearching();
                mConnState = EConnectionStates.DISCONNECTED;
                mActions.entryStateDisconnected();
                return;
            case BLE_DEVICE_FOUND:
                // switch state
                mActions.exitStateSearching();
                mConnState = EConnectionStates.CONNECTING;
                mActions.entryStateConnecting();
                return;
            case BLE_SEARCH_TIMEOUT:
            case BLE_DEVICE_DISCONNECTED:
            case BLE_DEVICE_CONNECTION_ERROR:
                mActions.handleBLESearchTimeout();
                mActions.exitStateSearching();
                mConnState = EConnectionStates.DISCONNECTED;
                mActions.entryStateDisconnected();
                return;
            default:
                return;
        }
    }

    private void onConnecting(EEvents mEvent, Context mContext) {
        switch (mEvent) {
            case ON_PAUSE:
            case BLE_DEVICE_DISCONNECTED:
                // switch state
                mActions.exitStateConnecting(true);
                mConnState = EConnectionStates.DISCONNECTED;
                mActions.entryStateDisconnected();
                return;
            case BLE_DEVICE_CONNECTED:
                // switch state
                mActions.exitStateConnecting(false);
                mConnState = EConnectionStates.CONNECTED;
                mActions.entryStateConnected();
                // check for next action and try to process next action
                if (mNextActionIfConnected != null && mNextActionIfConnected != EEvents.DEFAULT_IGNORE) {
                    // call next action in connected state and remove action
                    onConnected(mNextActionIfConnected, mContext);
                    mNextActionIfConnected = null;
                }
                return;
            case BLE_DEVICE_CONNECTION_ERROR:
                // print message
                mActions.handleBLEConnectingError();

                // change state
                mActions.exitStateConnecting(true);
                mConnState = EConnectionStates.DISCONNECTED;
                mActions.entryStateDisconnected();
                return;
            default:
                return;
        }
    }
    private void onConnected(EEvents mEvent, Context mContext) {
        switch (mEvent) {
            case ON_PAUSE:
            case BLE_DEVICE_DISCONNECTED:
                // switch state
                mActions.exitStateConnected();
                mConnState = EConnectionStates.DISCONNECTED;
                mActions.entryStateDisconnected();
                return;
            case BLE_DEVICE_CONNECTION_ERROR:
                // show message dialog
                mActions.handleBLEConnectionErrorConnected();

                // switch state
                mActions.exitStateConnected();
                mConnState = EConnectionStates.DISCONNECTED;
                mActions.entryStateDisconnected();
                return;
            case UI_EVENT_CONFIGURATION_UPDATE:
                // transmit current meal selection
                mActions.handleConfigurationUpdate();
                return;
            case UI_EVENT_RESET_SALT_AMOUNT:
                // show dialog and ask user to confirm instruction
                mActions.showDialogConfirmResetSaltAmount();
                return;
            case UI_EVENT_UPDATE_SENSOR_DATA:
                mActions.requestReadSensorData();
                return;
            case SENSOR_DATA_RECEIVED:
                mActions.handleSensorDataReceived();
                return;
            case MACHINE_CONTROL_DATA_TRANSMITTED_SUCCESSFUL:
                // print success message
                mActions.handleMachineControlDataTransmittedSuccessful();
                return;
            case MACHINE_CONTROL_DATA_TRANSMISSION_ERROR:
                mActions.handleMachineControlDataTransmissionError();
                return;
            default:
                return;
        }
    }

    private void onTargetConfigNotUpdated(EEvents mEvent) {
        switch (mEvent) {
            case MACHINE_CONTROL_DATA_TRANSMITTED_SUCCESSFUL:
                // check previous machine control Event
                if(mPreviousMachineControlEvent == EEvents.UI_EVENT_CONFIGURATION_UPDATE) {
                    mTargetConfigState = ETargetConfigurationStates.UPDATED;
                    mActions.entryTargetConfigUpdated();
                }
                // do not switch state after salt amount reset
                break;
            case UI_EVENT_TARGET_CONFIGURATION_CHANGED:
                if(mActions.handleUIChangedCheckSaltUpdated()) {
                    // change state
                    mTargetConfigState = ETargetConfigurationStates.UPDATED;
                    mActions.entryTargetConfigUpdated();
                }
                break;
            case UI_EVENT_RESET_SALT_AMOUNT:
            case UI_EVENT_CONFIGURATION_UPDATE:
                mPreviousMachineControlEvent = mEvent;
                break;
            default:
                break;
        }
    }

    private void onTargetConfigUpdated(EEvents mEvent) {
        switch (mEvent) {
            case UI_EVENT_TARGET_CONFIGURATION_CHANGED:
                if(!mActions.handleUIChangedCheckSaltUpdated()) {
                    // change state
                    mTargetConfigState = ETargetConfigurationStates.NOT_UPDATED;
                    mActions.entryTargetConfigNotUpdated();
                }
                break;
            case UI_EVENT_RESET_SALT_AMOUNT:
            case UI_EVENT_CONFIGURATION_UPDATE:
                mPreviousMachineControlEvent = mEvent;
                break;
            default:
                break;
        }
    }
}
