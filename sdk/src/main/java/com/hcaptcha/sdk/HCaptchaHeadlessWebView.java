package com.hcaptcha.sdk;

import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import androidx.fragment.app.FragmentActivity;

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

    HCaptchaHeadlessWebView(@NonNull final FragmentActivity activity,
                            @NonNull final HCaptchaConfig config,
                            @NonNull final HCaptchaStateListener listener,
                            @NonNull final IHCaptchaHtmlProvider htmlProvider) {
        this.config = config;
        this.listener = listener;
        final WebView webView = new HCaptchaWebView(activity);
        webView.setId(R.id.webView);
        webView.setVisibility(View.GONE);
        if (webView.getParent() == null) {
            final ViewGroup rootView = (ViewGroup) activity.getWindow().getDecorView().getRootView();
            rootView.addView(webView);
        }
        webViewHelper = new HCaptchaWebViewHelper(
                new Handler(Looper.getMainLooper()), activity, config, this, listener, webView, htmlProvider);
    }

    @Override
    public void startVerification(@NonNull FragmentActivity activity) {
        if (webViewLoaded) {
            // Safe to execute
            resetAndExecute();
        } else {
            shouldExecuteOnLoad = true;
        }
    }

    private void resetAndExecute() {
        webViewHelper.getWebView().loadUrl("javascript:resetAndExecute();");
    }

    @Override
    public void onFailure(final HCaptchaException exception) {
        final boolean silentRetry = webViewHelper.getConfig().getResetOnTimeout()
                && exception.getHCaptchaError() == HCaptchaError.SESSION_TIMEOUT;
        if (silentRetry) {
            resetAndExecute();
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
        if (shouldExecuteOnLoad) {
            shouldExecuteOnLoad = false;
            resetAndExecute();
        }
    }

    @Override
    public void onOpen() {
        listener.onOpen();
    }
}
