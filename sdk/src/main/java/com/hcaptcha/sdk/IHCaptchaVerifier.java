package com.hcaptcha.sdk;

import android.app.Activity;
import androidx.annotation.Nullable;

import com.hcaptcha.sdk.tasks.OnFailureListener;
import com.hcaptcha.sdk.tasks.OnLoadedListener;
import com.hcaptcha.sdk.tasks.OnOpenListener;
import com.hcaptcha.sdk.tasks.OnSuccessListener;
import lombok.NonNull;

interface IHCaptchaVerifier extends
        OnLoadedListener,
        OnOpenListener,
        OnSuccessListener<String>,
        OnFailureListener {

    /**
     * Starts the human verification process.
     *
     * @param activity The activity to start verification in
     * @param verifyParams Optional verification parameters (phone prefix, phone number, etc.)
     */
    void startVerification(@NonNull Activity activity, @Nullable HCaptchaVerifyParams verifyParams);

    /**
     * Force stop verification and release resources.
     */
    void reset();
}
