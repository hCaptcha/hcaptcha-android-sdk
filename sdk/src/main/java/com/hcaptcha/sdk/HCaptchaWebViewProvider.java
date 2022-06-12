package com.hcaptcha.sdk;

import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
    @Nullable
    WebView getWebView();
    void startVerification(@NonNull FragmentActivity activity);
}
