package de.hitkarlsruhe.consaltingmachine;

public enum EEvents {
    DEFAULT_IGNORE,                             // ignore this event

    // Events from UI: Buttons, Spinner, Slider ...
    UI_EVENT_TARGET_CONFIGURATION_CHANGED,      // user selected meal or changed salt amount
    UI_EVENT_CONFIGURATION_UPDATE,              // update button: send current meal selection to target
    UI_EVENT_RESET_SALT_AMOUNT,                 // button "reset salt amount" pressed
    UI_EVENT_UPDATE_SENSOR_DATA,                // button "update sensor data" pressed

    // Events from activity lifecycle
    ON_PAUSE,                                   // onPause-Event of Android app
    ON_RESUME,                                  // onResume-Event of Android app

    // Bluetooth connection events
    BLE_BEGIN_SEARCH,                           // search for BLE-Devices started
    BLE_DEVICE_FOUND,                           // ConSALTingMachine found
    BLE_SEARCH_TIMEOUT,                         // BLE-Search timeout (10 seconds)
    BLE_DEVICE_CONNECTED,                       // connected to ConSALTingMachine
    BLE_DEVICE_DISCONNECTED,                    // BLE connection lost
    BLE_DEVICE_CONNECTION_ERROR,                // BLE connection error
    SENSOR_DATA_RECEIVED,                       // sensor data received from target
    MACHINE_CONTROL_DATA_TRANSMITTED_SUCCESSFUL,    // machine control data transmission successful
    MACHINE_CONTROL_DATA_TRANSMISSION_ERROR     // error while transmitting machine control data
}