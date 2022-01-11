package de.hitkarlsruhe.consaltingmachine.datastructures;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

// class CMachineControlData: contains variables for bluetooth machine control
// characteristic
public class CMachineControlData {

    public float mSaltConcentration;       // salt concentration in gram/liter
    public EInstructions mInstruction;     // instruction to ConSALTing Machine

    // constructor, initialize variables
    public CMachineControlData() {
        mSaltConcentration = 0.0f;
        mInstruction = EInstructions.SET_SALT_CONCENTRATION;
    }
    public CMachineControlData(float pSaltConcentration, EInstructions pInstruction) {
        mSaltConcentration = pSaltConcentration;
        mInstruction = pInstruction;
    }

    // define size of single object
    public static int getObjectSize() { return 8; }

    // convert object to byte array
    public byte[] convertToByteArray() {
        // define result array
        byte[] resultArray = new byte[getObjectSize()];

        // convert all objects to integers
        int saltConcentrationBytes = Float.floatToRawIntBits(mSaltConcentration);
        int enumBytes = (mInstruction == EInstructions.SET_SALT_CONCENTRATION ? 0:1);

        // shift single bytes out of integer variables, store bytes in resultArray (LSB first)
        for(int i = 0; i < getObjectSize(); i++) {
            if(i < 4) {
                resultArray[i] = (byte) (saltConcentrationBytes & 0x000000FF);
                saltConcentrationBytes >>= 8;
            } else {
                resultArray[i] = (byte) (enumBytes & 0x000000FF);
                enumBytes >>= 8;
            }
        }
        return resultArray;
    }

    // convert byte array to object
    public static CMachineControlData createInstanceFromByteArray(byte[] pArray) {
        // create integers for all float variables
        int saltConcentrationBytes = 0, instructionBytes = 0;

        // shift single bytes in integer fields
        for(int i = 0; i < getObjectSize(); i++) {
            if(i < 4) {
                saltConcentrationBytes >>= 8;
                saltConcentrationBytes &= 0x00FFFFFF;
                saltConcentrationBytes |= (pArray[i] << 24);
            }
            else {
                instructionBytes >>= 8;
                instructionBytes &= 0x00FFFFFF;
                instructionBytes |= (pArray[i] << 24);
            }
        }
        return new CMachineControlData(Float.intBitsToFloat(saltConcentrationBytes),
                instructionBytes == 0   ? EInstructions.SET_SALT_CONCENTRATION
                                        : EInstructions.RESET_SALT_AMOUNT);
    }
}
