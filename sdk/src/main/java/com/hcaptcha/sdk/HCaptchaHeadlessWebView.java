package com.hcaptcha.sdk;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

final class HCaptchaHeadlessWebView implements HCaptchaWebViewProvider {

    @NonNull
    private final Handler handler = new Handler(Looper.getMainLooper());

    @NonNull
    private final HCaptchaStateListener listener;

    @NonNull
    private final HCaptchaWebViewHelper webViewHelper;

    private WebView webView;

    public HCaptchaHeadlessWebView(@NonNull Context context,
                                   @NonNull HCaptchaConfig config,
                                   @NonNull HCaptchaStateListener listener) {
        this.listener = listener;
        this.webViewHelper = new HCaptchaWebViewHelper(context, config, this);
    }

    @Override
    public void startVerification(@NonNull FragmentActivity activity) {
        this.webView = new WebView(activity);
        this.webView.setId(R.id.webView);
        this.webView.setVisibility(View.GONE);

        final ViewGroup rootView = (ViewGroup) activity.getWindow().getDecorView().getRootView();
        rootView.addView(webView);

        webViewHelper.setup();
    }

    @Override
    @NonNull
    public WebView getWebView() {
        return webView;
    }

    @Override
    public void onFailure(final HCaptchaException hCaptchaException) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                webViewHelper.cleanup();
                listener.onFailure(hCaptchaException);
            }
        });
    }

    @Override
    public void onSuccess(final HCaptchaTokenResponse hCaptchaTokenResponse) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                webViewHelper.cleanup();
                listener.onSuccess(hCaptchaTokenResponse);
            }
        });
    }

    @Override
    public void onLoaded() {
        // this callback make no sense for invisible WebView
    }

    @Override
    public void onOpen() {
        // this callback make no sense for invisible WebView
    }
}
