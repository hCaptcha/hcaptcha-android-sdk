package com.hcaptcha.sdk;

import androidx.fragment.app.FragmentActivity;

import com.hcaptcha.sdk.tasks.OnLoadedListener;
import com.hcaptcha.sdk.tasks.OnOpenListener;
import com.hcaptcha.sdk.tasks.OnSuccessListener;
import lombok.NonNull;

interface IHCaptchaVerifier extends
        OnLoadedListener,
        OnOpenListener,
        OnSuccessListener<String> {

    /**
     * Starts the human verification process.
     */
    void startVerification(@NonNull FragmentActivity activity);

    /**
     * Called whenever there is a hCaptcha error or user closed the challenge dialog
     *
     * @param error the error
     */
    void onError(HCaptchaError error);
}
