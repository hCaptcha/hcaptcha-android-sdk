package com.hcaptcha.sdk;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.webkit.WebSettings;
import android.webkit.WebView;

import lombok.Getter;
import lombok.NonNull;

final class HCaptchaWebViewHelper {
    static final String HCAPTCHA_URL = "file:///android_asset/hcaptcha-form.html";

    @NonNull
    private final Context context;

    @NonNull
    @Getter
    private final HCaptchaConfig config;

    @NonNull
    private final IHCaptchaVerifier captchaVerifier;

    @Getter
    @NonNull
    private final HCaptchaStateListener listener;

    @Getter
    @NonNull
    private final WebView webView;

    HCaptchaWebViewHelper(@NonNull final Handler handler,
                          @NonNull final Context context,
                          @NonNull final HCaptchaConfig config,
                          @NonNull final IHCaptchaVerifier captchaVerifier,
                          @NonNull final HCaptchaStateListener listener,
                          @NonNull final WebView webView) {
        this.context = context;
        this.config = config;
        this.captchaVerifier = captchaVerifier;
        this.listener = listener;
        this.webView = webView;
        setupWebView(handler);
    }

    /**
     * General setup for the webview:
     * * enables javascript to be able to load and execute hcaptcha api.js
     * * loads custom html page to display challenge and/or checkbox
     */
    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    private void setupWebView(@NonNull final Handler handler) {
        final HCaptchaJSInterface jsInterface = new HCaptchaJSInterface(handler, config, captchaVerifier);
        final HCaptchaDebugInfo debugInfo = new HCaptchaDebugInfo(context);
        final WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        webView.setBackgroundColor(Color.TRANSPARENT);
        webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        webView.addJavascriptInterface(jsInterface, HCaptchaJSInterface.JS_INTERFACE_TAG);
        webView.addJavascriptInterface(debugInfo, HCaptchaDebugInfo.JS_INTERFACE_TAG);
        webView.loadUrl(HCAPTCHA_URL);
    }

    public void destroy() {
        webView.removeJavascriptInterface(HCaptchaJSInterface.JS_INTERFACE_TAG);
        webView.removeJavascriptInterface(HCaptchaDebugInfo.JS_INTERFACE_TAG);
        final ViewParent parent = webView.getParent();
        if (parent instanceof ViewGroup) {
            ((ViewGroup) parent).removeView(webView);
        } else {
            Log.w(HCaptcha.TAG, "webView.getParent() is null or not a ViewGroup instance");
        }
        webView.destroy();
    }
}
