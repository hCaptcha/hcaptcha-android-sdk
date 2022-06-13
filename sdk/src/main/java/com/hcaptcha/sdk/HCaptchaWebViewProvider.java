package com.hcaptcha.sdk;

import lombok.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.hcaptcha.sdk.tasks.OnFailureListener;
import com.hcaptcha.sdk.tasks.OnLoadedListener;
import com.hcaptcha.sdk.tasks.OnOpenListener;
import com.hcaptcha.sdk.tasks.OnSuccessListener;

interface HCaptchaWebViewProvider extends
        OnLoadedListener,
        OnOpenListener,
        OnSuccessListener<HCaptchaTokenResponse>,
        OnFailureListener {

    @NonNull
    HCaptchaConfig getConfig();

    void verifyWithHCaptcha(@NonNull FragmentActivity activity);

}
