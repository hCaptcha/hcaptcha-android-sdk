package com.hcaptcha.sdk;

import android.os.Handler;
import android.webkit.JavascriptInterface;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.NonNull;

import java.io.Serializable;


/**
 * The JavaScript Interface which bridges the js and the java code
 */
@AllArgsConstructor
class HCaptchaJSInterface implements Serializable {
    public static final String JS_INTERFACE_TAG = "JSInterface";

    @NonNull
    private final Handler handler;

    @NonNull
    private final HCaptchaConfig config;

    @NonNull
    private final IHCaptchaVerifier captchaVerifier;

    @JavascriptInterface
    public String getConfig() throws JsonProcessingException {
        final ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(this.config);
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
