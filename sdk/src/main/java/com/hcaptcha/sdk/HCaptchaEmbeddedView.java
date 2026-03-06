package com.hcaptcha.sdk;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.view.ViewGroup;
import android.view.ViewParent;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

final class HCaptchaEmbeddedView implements IHCaptchaVerifier {

    @NonNull
    private final HCaptchaStateListener listener;

    @NonNull
    private final HCaptchaWebViewHelper webViewHelper;

    @NonNull
    private final ViewGroup challengeContainer;

    @Nullable
    private HCaptchaVerifyParams verifyParams;

    private boolean readyForInteraction = false;

    HCaptchaEmbeddedView(@NonNull final Activity activity,
                         @NonNull final HCaptchaConfig config,
                         @NonNull final HCaptchaInternalConfig internalConfig,
                         @NonNull final HCaptchaStateListener listener,
                         @NonNull final ViewGroup challengeContainer) {
        this.listener = listener;
        this.challengeContainer = challengeContainer;
        final HCaptchaWebView webView = new HCaptchaWebView(activity);
        webViewHelper = new HCaptchaWebViewHelper(new Handler(Looper.getMainLooper()),
                activity, config, internalConfig, this, webView);
    }

    private void attachWebView() {
        final HCaptchaWebView webView = webViewHelper.getWebView();
        final ViewParent parent = webView.getParent();
        if (parent instanceof ViewGroup && parent != challengeContainer) {
            ((ViewGroup) parent).removeView(webView);
        }
        if (webView.getParent() == null) {
            challengeContainer.addView(webView, new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            ));
        }
    }

    private void detachWebView() {
        final HCaptchaWebView webView = webViewHelper.getWebView();
        final ViewParent parent = webView.getParent();
        if (parent instanceof ViewGroup) {
            ((ViewGroup) parent).removeView(webView);
        }
    }

    @Override
    public void onLoaded() {
        listener.onLoaded();
        final HCaptchaConfig config = webViewHelper.getConfig();
        final boolean isInvisible = config.getSize() == HCaptchaSize.INVISIBLE;

        if (!isInvisible) {
            readyForInteraction = true;
        }

        if (isInvisible && !config.isHeadlessMode()) {
            webViewHelper.resetAndExecute(verifyParams);
        } else {
            webViewHelper.setVerifyParams(verifyParams);
        }
    }

    @Override
    public void onOpen() {
        readyForInteraction = true;
        listener.onOpen();
    }

    @Override
    public void onSuccess(String token) {
        listener.onSuccess(token);
    }

    @Override
    public void onFailure(@NonNull HCaptchaException exception) {
        if (webViewHelper.shouldRetry(exception)) {
            webViewHelper.resetAndExecute(verifyParams);
            return;
        }
        listener.onFailure(exception);
    }

    @Override
    public void startVerification(@NonNull Activity activity, @Nullable HCaptchaVerifyParams params) {
        this.verifyParams = params;

        if (readyForInteraction) {
            webViewHelper.setVerifyParams(params);
        }

        attachWebView();
    }

    @Override
    public void reset() {
        readyForInteraction = false;
        verifyParams = null;
        webViewHelper.reset();
        detachWebView();
    }

    @Override
    public void destroy() {
        readyForInteraction = false;
        verifyParams = null;
        webViewHelper.destroy();
    }
}
