package com.hcaptcha.sdk;

import android.os.Parcel;
import android.os.Parcelable;

abstract class HCaptchaStateListener implements Parcelable {

    abstract void onSuccess(HCaptchaTokenResponse tokenResponse);

    abstract void onFailure(HCaptchaException exception);

    abstract void onOpen();

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        // nothing to persist
    }
}
