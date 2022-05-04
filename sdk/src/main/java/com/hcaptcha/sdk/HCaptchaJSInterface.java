package com.hcaptcha.sdk;

import android.webkit.JavascriptInterface;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hcaptcha.sdk.tasks.OnFailureListener;
import com.hcaptcha.sdk.tasks.OnLoadedListener;
import com.hcaptcha.sdk.tasks.OnOpenListener;
import com.hcaptcha.sdk.tasks.OnSuccessListener;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;


/**
 * The JavaScript Interface which bridges the js and the java code
 */
@Data
@AllArgsConstructor
class HCaptchaJSInterface implements Serializable {

    public static final String JS_INTERFACE_TAG = "JSInterface";

    private final HCaptchaConfig hCaptchaConfig;

    private final OnLoadedListener onLoadedListener;

    private final OnOpenListener onOpenListener;

    private final OnSuccessListener<HCaptchaTokenResponse> onSuccessListener;

    private final OnFailureListener onFailureListener;

    @JavascriptInterface
    public String getConfig() throws JsonProcessingException {
        final ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(this.hCaptchaConfig);
    }

    @JavascriptInterface
    public void onPass(final String token) {
        onSuccessListener.onSuccess(new HCaptchaTokenResponse(token));
    }

    @JavascriptInterface
    public void onError(final int errCode) {
        final HCaptchaError error = HCaptchaError.fromId(errCode);
        onFailureListener.onFailure(new HCaptchaException(error));
    }

    @JavascriptInterface
    public void onLoaded() {
        this.onLoadedListener.onLoaded();
    }

    @JavascriptInterface
    public void onOpen() {
        this.onOpenListener.onOpen();
    }
}
