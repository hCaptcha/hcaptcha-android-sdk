package com.hcaptcha.sdk;

import android.app.Activity;

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
     */
    void startVerification(@NonNull Activity activity);

    /**
     * Force stop verification and release resources.
     */
    void reset();
}
