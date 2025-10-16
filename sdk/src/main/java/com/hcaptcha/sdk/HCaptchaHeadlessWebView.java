package com.hcaptcha.sdk;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import androidx.annotation.Nullable;

import lombok.Getter;
import lombok.NonNull;

final class HCaptchaHeadlessWebView implements IHCaptchaVerifier {
    @Getter
    @NonNull
    private final HCaptchaConfig config;

    @NonNull
    private final HCaptchaStateListener listener;

    @NonNull
    private final HCaptchaWebViewHelper webViewHelper;

    private boolean webViewLoaded;
    private boolean shouldExecuteOnLoad;
    private boolean shouldResetOnLoad;

    @Nullable
    private HCaptchaVerifyParams verifyParams;

    HCaptchaHeadlessWebView(@NonNull final Activity activity,
                            @NonNull final HCaptchaConfig config,
                            @NonNull final HCaptchaInternalConfig internalConfig,
                            @NonNull final HCaptchaStateListener listener) {
        HCaptchaLog.d("HeadlessWebView.init");
        this.config = config;
        this.listener = listener;
        final HCaptchaWebView webView = new HCaptchaWebView(activity);
        webView.setId(R.id.webView);
        webView.setVisibility(View.GONE);
        if (webView.getParent() == null) {
            final ViewGroup rootView = (ViewGroup) activity.getWindow().getDecorView().getRootView();
            rootView.addView(webView);
        }
        webViewHelper = new HCaptchaWebViewHelper(
                new Handler(Looper.getMainLooper()), activity, config, internalConfig, this, webView);
    }

    private void execute() {
        if (this.verifyParams != null) {
            webViewHelper.reset();
            webViewHelper.setVerifyParams(this.verifyParams);
        }

        webViewHelper.execute();
    }

    @Override
    public void startVerification(@NonNull Activity activity, @Nullable HCaptchaVerifyParams params) {
        this.verifyParams = params;
        if (webViewLoaded) {
            this.execute();
        } else {
            shouldExecuteOnLoad = true;
        }
    }

    @Override
    public void onFailure(@NonNull final HCaptchaException exception) {
        final boolean silentRetry = webViewHelper.shouldRetry(exception);
        if (silentRetry) {
            this.execute();
        } else {
            listener.onFailure(exception);
        }
    }

    @Override
    public void onSuccess(final String token) {
        listener.onSuccess(token);
    }

    @Override
    public void onLoaded() {
        webViewLoaded = true;
        if (shouldResetOnLoad) {
            shouldResetOnLoad = false;
            reset();
        } else if (shouldExecuteOnLoad) {
            shouldExecuteOnLoad = false;
            this.execute();
        }
    }

    @Override
    public void onOpen() {
        listener.onOpen();
    }

    @Override
    public void reset() {
        if (webViewLoaded) {
            webViewHelper.reset();
            final WebView webView = webViewHelper.getWebView();
            if (webView.getParent() != null) {
                ((ViewGroup) webView.getParent()).removeView(webView);
            }
        } else {
            shouldResetOnLoad = true;
        }
    }
}
