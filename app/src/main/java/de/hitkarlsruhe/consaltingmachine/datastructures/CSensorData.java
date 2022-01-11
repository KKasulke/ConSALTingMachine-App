package de.hitkarlsruhe.consaltingmachine.datastructures;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class CSensorData {
    public float mBatteryState;        // battery state (in %)
    public float mSaltAmount;          // remaining salt amount in ConSALTing Machine
    public float mPressure;            // detected pressure of pressure sensor

    // constructor
    public CSensorData() {
        mBatteryState = 0.0f;
        mSaltAmount = 0.0f;
        mPressure = 0.0f;
    }
    // constructor with arguments
    public CSensorData(float pBatteryState, float pSaltAmount, float pPressure) {
        mBatteryState = pBatteryState;
        mSaltAmount = pSaltAmount;
        mPressure = pPressure;
    }

    // define size of single object
    public static int getObjectSize() { return 12; }

    // convert object to byte array
    public byte[] convertToByteArray() {
        // define result array
        byte[] resultArray = new byte[getObjectSize()];

        // convert all objects to integers
        int batteryStateBytes = Float.floatToRawIntBits(mBatteryState);
        int saltAmountBytes = Float.floatToRawIntBits(mSaltAmount);
        int pressureBytes = Float.floatToRawIntBits(mPressure);

        // shift single bytes out of integer variables, store bytes in resultArray (LSB first)
        for(int i = 0; i < getObjectSize(); i++) {
            if(i < 4) {
                resultArray[i] = (byte) (batteryStateBytes & 0x000000FF);
                batteryStateBytes >>= 8;
            } else if(i < 8) {
                resultArray[i] = (byte) (saltAmountBytes & 0x000000FF);
                saltAmountBytes >>= 8;
            } else {
                resultArray[i] = (byte) (pressureBytes & 0x000000FF);
                pressureBytes >>= 8;
            }
        }
        return resultArray;
    }

    // convert byte array to object
    public static CSensorData createInstanceFromByteArray(byte[] pArray) {
        // create integers for all float variables
        int batteryStateBytes = 0, saltAmountBytes = 0, pressureBytes = 0;

        // shift single bytes in integer fields
        for(int i = 0; i < getObjectSize(); i++) {
            if(i < 4) {
                batteryStateBytes >>= 8;
                batteryStateBytes &= 0x00FFFFFF;
                batteryStateBytes |= (pArray[i] << 24);
            }
            else if (i < 8) {
                saltAmountBytes >>= 8;
                saltAmountBytes &= 0x00FFFFFF;
                saltAmountBytes |= (pArray[i] << 24);
            }
            else {
                pressureBytes >>= 8;
                pressureBytes &= 0x00FFFFFF;
                pressureBytes |= (pArray[i] << 24);
            }
        }
        return new CSensorData(Float.intBitsToFloat(batteryStateBytes),
                Float.intBitsToFloat(saltAmountBytes),
                Float.intBitsToFloat(pressureBytes));
    }

    public void setDataFromByteArray(byte[] pArray) {
        // create integers for all float variables
        int batteryStateBytes = 0, saltAmountBytes = 0, pressureBytes = 0;

        // shift single bytes in integer fields
        for(int i = 0; i < getObjectSize(); i++) {
            if(i < 4) {
                batteryStateBytes >>= 8;
                batteryStateBytes &= 0x00FFFFFF;
                batteryStateBytes |= (pArray[i] << 24);
            }
            else if (i < 8) {
                saltAmountBytes >>= 8;
                saltAmountBytes &= 0x00FFFFFF;
                saltAmountBytes |= (pArray[i] << 24);
            }
            else {
                pressureBytes >>= 8;
                pressureBytes &= 0x00FFFFFF;
                pressureBytes |= (pArray[i] << 24);
            }
        }
        mBatteryState = Float.intBitsToFloat(batteryStateBytes);
        mSaltAmount = Float.intBitsToFloat(saltAmountBytes);
        mPressure = Float.intBitsToFloat(pressureBytes);
    }
}
