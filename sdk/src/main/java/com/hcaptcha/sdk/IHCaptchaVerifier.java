package com.hcaptcha.sdk;

import androidx.fragment.app.FragmentActivity;

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
    void startVerification(@NonNull FragmentActivity activity);

    /**
     * Force stop verification and release resources.
     */
    void clear();
}
