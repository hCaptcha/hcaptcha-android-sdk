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

import androidx.annotation.NonNull;

final class HCaptchaWebViewHelper {
    static final String HCAPTCHA_URL = "file:///android_asset/hcaptcha-form.html";

    @NonNull
    private final HCaptchaWebViewProvider provider;

    @NonNull
    private final HCaptchaJSInterface jsInterface;

    @NonNull
    private final HCaptchaDebugInfo debugInfo;

    HCaptchaWebViewHelper(@NonNull Context context,
                          @NonNull HCaptchaConfig config,
                          @NonNull HCaptchaWebViewProvider provider) {
        this.provider = provider;
        this.jsInterface = new HCaptchaJSInterface(config, provider, provider, provider, provider);
        this.debugInfo = new HCaptchaDebugInfo(context);
    }

    /**
     * General setup for the webview:
     * * enables javascript to be able to load and execute hcaptcha api.js
     * * loads custom html page to display challenge and/or checkbox
     */
    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    public void setup() {
        final WebView webView = provider.getWebView();
        if (webView == null) {
            Log.w(HCaptcha.TAG, "WebView doesn't ready for setup");
            return;
        }
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

    public void cleanup() {
        final WebView webView = provider.getWebView();
        if (webView == null) {
            Log.w(HCaptcha.TAG, "WebView doesn't ready for cleanup");
            return;
        }
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
