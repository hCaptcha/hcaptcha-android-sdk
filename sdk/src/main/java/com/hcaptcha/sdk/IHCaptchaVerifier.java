package com.hcaptcha.sdk;

import androidx.fragment.app.FragmentActivity;

import com.hcaptcha.sdk.tasks.OnEventListener;
import com.hcaptcha.sdk.tasks.OnFailureListener;
import com.hcaptcha.sdk.tasks.OnSuccessListener;
import lombok.NonNull;

interface IHCaptchaVerifier extends
        OnEventListener,
        OnSuccessListener<HCaptchaTokenResponse>,
        OnFailureListener {

    /**
     * Starts the human verification process.
     */
    void startVerification(@NonNull FragmentActivity activity);
}
