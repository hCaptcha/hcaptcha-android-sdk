package com.hcaptcha.sdk;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
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

    @Getter
    @NonNull
    private final HCaptchaConfig config;

    @NonNull
    private final HCaptchaWebViewProvider provider;

    @Getter
    @NonNull
    private final HCaptchaStateListener listener;

    @Getter
    @NonNull
    private final WebView webView;

    public HCaptchaWebViewHelper(@NonNull final Context context,
                                 @NonNull final HCaptchaConfig config,
                                 @NonNull final HCaptchaWebViewProvider provider,
                                 @NonNull final HCaptchaStateListener listener,
                                 @NonNull final WebView webView) {
        this.context = context;
        this.config = config;
        this.provider = provider;
        this.listener = listener;
        this.webView = webView;
        setupWebView();
    }

    /**
     * General setup for the webview:
     * * enables javascript to be able to load and execute hcaptcha api.js
     * * loads custom html page to display challenge and/or checkbox
     */
    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    private void setupWebView() {
        final HCaptchaJSInterface jsInterface = new HCaptchaJSInterface(config, provider, provider, provider, provider);
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
