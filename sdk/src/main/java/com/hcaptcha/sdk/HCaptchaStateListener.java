package com.hcaptcha.sdk;

import android.os.Parcel;
import android.os.Parcelable;

abstract class HCaptchaStateListener implements Parcelable {

    abstract void onSuccess(String token);

    abstract void onFailure(HCaptchaException exception);

    abstract void onOpen();

    abstract boolean shouldRetry(HCaptchaError error);

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        // nothing to persist
    }
}
