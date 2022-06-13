package com.hcaptcha.sdk;

import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import lombok.Getter;
import lombok.NonNull;
import androidx.fragment.app.FragmentActivity;

final class HCaptchaHeadlessWebView implements HCaptchaWebViewProvider {

    @NonNull
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Getter
    @NonNull
    private final HCaptchaConfig config;

    @NonNull
    private final HCaptchaStateListener listener;

    @NonNull
    private final HCaptchaWebViewHelper webViewHelper;

    public HCaptchaHeadlessWebView(@NonNull FragmentActivity activity,
                                   @NonNull final HCaptchaConfig config,
                                   @NonNull final HCaptchaStateListener listener) {
        this.config = config;
        this.listener = listener;
        final WebView webView = new WebView(activity);
        webView.setId(R.id.webView);
        webView.setVisibility(View.GONE);
        if (webView.getParent() == null) {
            final ViewGroup rootView = (ViewGroup) activity.getWindow().getDecorView().getRootView();
            rootView.addView(webView);
        }
        webViewHelper = new HCaptchaWebViewHelper(activity, config, this, listener, webView);
    }

    @Override
    public void verifyWithHCaptcha(@NonNull FragmentActivity activity) {
        resetAndExecute();
    }

    private void resetAndExecute() {
        webViewHelper.getWebView().loadUrl("javascript:resetAndExecute();");
    }

    @Override
    public @NonNull HCaptchaConfig getConfig() {
        return config;
    }

    @Override
    public void onFailure(final HCaptchaException hCaptchaException) {
        final boolean silentRetry = webViewHelper.getConfig().getResetOnTimeout()
                && hCaptchaException.getHCaptchaError() == HCaptchaError.SESSION_TIMEOUT;
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (silentRetry) {
                    resetAndExecute();
                } else {
                    listener.onFailure(hCaptchaException);
                }
            }
        });
    }

    @Override
    public void onSuccess(final HCaptchaTokenResponse hCaptchaTokenResponse) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                listener.onSuccess(hCaptchaTokenResponse);
            }
        });
    }

    @Override
    public void onLoaded() {
        // Do nothing when hCaptcha is loaded.
    }

    @Override
    public void onOpen() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                listener.onOpen();
            }
        });
    }
}
