package com.hcaptcha.sdk;

import android.os.Parcel;
import android.os.Parcelable;

abstract class HCaptchaDialogListener implements Parcelable {

    abstract void onSuccess(HCaptchaTokenResponse hCaptchaTokenResponse);

    abstract void onFailure(HCaptchaException hCaptchaException);

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

    }
}
