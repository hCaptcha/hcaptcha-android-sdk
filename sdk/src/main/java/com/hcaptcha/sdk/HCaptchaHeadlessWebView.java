package com.hcaptcha.sdk;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.webkit.WebView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

public class HCaptchaHeadlessWebView implements HCaptchaWebViewProvider {

    @NonNull
    private final Handler handler = new Handler(Looper.getMainLooper());

    @NonNull
    private final HCaptchaDialogListener listener;

    @NonNull
    private final HCaptchaConfig config;

    @NonNull
    private final WebView webView;

    public HCaptchaHeadlessWebView(@NonNull Context context,
                                   @NonNull HCaptchaConfig config,
                                   @NonNull HCaptchaDialogListener listener) {
        this.webView = new WebView(context);
        this.webView.setVisibility(View.GONE);
        this.listener = listener;
        this.config = config;
    }

    @Override
    public void startVerification(@NonNull FragmentActivity activity) {
        ViewGroup rootView = (ViewGroup) activity.getWindow().getDecorView().getRootView();
        rootView.addView(webView);

        HCaptchaWebViewHelper.prepare(config, this);
    }

    @Override
    public WebView getWebView() {
        return webView;
    }

    @Override
    public void onFailure(HCaptchaException hCaptchaException) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                HCaptchaWebViewHelper.finish(HCaptchaHeadlessWebView.this);
                listener.onFailure(hCaptchaException);
            }
        });
    }

    @Override
    public void onLoaded() {
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

    @Override
    public void onSuccess(HCaptchaTokenResponse hCaptchaTokenResponse) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                HCaptchaWebViewHelper.finish(HCaptchaHeadlessWebView.this);
                listener.onSuccess(hCaptchaTokenResponse);
            }
        });
    }
}
