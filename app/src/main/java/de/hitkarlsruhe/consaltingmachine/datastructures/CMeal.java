package de.hitkarlsruhe.consaltingmachine.datastructures;

import android.os.Parcel;
import android.os.Parcelable;

public class CMeal implements Parcelable {
    // attributes
    public String mName;
    public float mSaltAmount;

    // constructor
    public CMeal() {
        mName = "";
        mSaltAmount = 0.0f;
    }

    // constructor with arguments
    public CMeal(String pName, float pSaltAmount) {
        mName = pName;
        mSaltAmount = pSaltAmount;
    }

    // ========================================================
    // FUNCTIONS REQUIRED FOR PARCELABLE IMPLEMENTATION
    // ========================================================
    protected CMeal(Parcel in) {
        mName = in.readString();
        mSaltAmount = in.readFloat();
    }

    public static final Creator<CMeal> CREATOR = new Creator<CMeal>() {
        @Override
        public CMeal createFromParcel(Parcel in) {
            return new CMeal(in);
        }

        @Override
        public CMeal[] newArray(int size) {
            return new CMeal[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mName);
        dest.writeFloat(mSaltAmount);
    }
}
