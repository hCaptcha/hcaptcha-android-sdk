package com.hcaptcha.sdk;

import android.os.Handler;
import android.util.Log;
import android.webkit.JavascriptInterface;
import androidx.annotation.Nullable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;

import java.io.Serializable;


/**
 * The JavaScript Interface which bridges the js and the java code
 */
class HCaptchaJSInterface implements Serializable {
    public static final String JS_INTERFACE_TAG = "JSInterface";

    @NonNull
    private final Handler handler;

    @Nullable
    private final String config;

    @NonNull
    private final IHCaptchaVerifier captchaVerifier;

    HCaptchaJSInterface(final Handler handler, final HCaptchaConfig config,
                        final IHCaptchaVerifier captchaVerifier) {
        this.handler = handler;
        this.captchaVerifier = captchaVerifier;
        String configJson = null;
        try {
            final ObjectMapper objectMapper = new ObjectMapper();
            configJson = objectMapper.writeValueAsString(config);
        } catch (JsonProcessingException e) {
            Log.w(JS_INTERFACE_TAG, "Cannot prepare config for passing to WebView."
                    + " A fallback config will be used");
        }
        this.config = configJson;
    }

    @JavascriptInterface
    public String getConfig() {
        return this.config;
    }

    @JavascriptInterface
    public void onPass(final String token) {
        HCaptchaLog.d("JSInterface.onPass");
        handler.post(new Runnable() {
            @Override
            public void run() {
                captchaVerifier.onSuccess(token);
            }
        });
    }

    @JavascriptInterface
    public void onError(final int errCode) {
        HCaptchaLog.d("JSInterface.onError %d", errCode);
        final HCaptchaError error = HCaptchaError.fromId(errCode);
        handler.post(new Runnable() {
            @Override
            public void run() {
                captchaVerifier.onFailure(new HCaptchaException(error));
            }
        });
    }

    @JavascriptInterface
    public void onLoaded() {
        HCaptchaLog.d("JSInterface.onLoaded");
        handler.post(new Runnable() {
            @Override
            public void run() {
                captchaVerifier.onLoaded();
            }
        });
    }

    @JavascriptInterface
    public void onOpen() {
        HCaptchaLog.d("JSInterface.onOpen");
        handler.post(new Runnable() {
            @Override
            public void run() {
                captchaVerifier.onOpen();
            }
        });
    }
}
