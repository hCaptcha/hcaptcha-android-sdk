package com.hcaptcha.sdk;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.webkit.ConsoleMessage;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.NonNull;

final class HCaptchaWebViewHelper {
    private static final String SMSTO_SCHEME = "smsto:";
    private static final String SMS_BODY_EXTRA = "sms_body";

    @NonNull
    private final Context context;

    @NonNull
    @Getter
    private final HCaptchaConfig config;

    @NonNull
    private final IHCaptchaVerifier captchaVerifier;

    @Getter
    @NonNull
    private final HCaptchaWebView webView;

    @NonNull
    private final IHCaptchaHtmlProvider htmlProvider;

    HCaptchaWebViewHelper(@NonNull final Handler handler,
                          @NonNull final Context context,
                          @NonNull final HCaptchaConfig config,
                          @NonNull final HCaptchaInternalConfig internalConfig,
                          @NonNull final IHCaptchaVerifier captchaVerifier,
                          @NonNull final HCaptchaWebView webView) {
        this.context = context;
        this.config = config;
        this.captchaVerifier = captchaVerifier;
        this.webView = webView;
        this.htmlProvider = internalConfig.getHtmlProvider();
        setupWebView(handler);
    }

    /**
     * General setup for the webview:
     * * enables javascript to be able to load and execute hcaptcha api.js
     * * loads custom html page to display challenge and/or checkbox
     */
    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    private void setupWebView(@NonNull final Handler handler) {
        HCaptchaLog.d("WebViewHelper.setupWebView");

        webView.setId(R.id.webView);

        final HCaptchaJSInterface jsInterface = new HCaptchaJSInterface(handler, config, captchaVerifier);
        final HCaptchaDebugInfo debugInfo = new HCaptchaDebugInfo(context);
        final WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setGeolocationEnabled(false);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setSupportMultipleWindows(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            // Allow media playback to start without a user gesture.
            settings.setMediaPlaybackRequiresUserGesture(false);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webView.setWebViewClient(new HCaptchaWebClient(handler));
        }
        webView.setWebChromeClient(new HCaptchaWebChromeClient());
        webView.setBackgroundColor(Color.TRANSPARENT);
        if (config.getDisableHardwareAcceleration()) {
            webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }
        webView.addJavascriptInterface(jsInterface, HCaptchaJSInterface.JS_INTERFACE_TAG);
        webView.addJavascriptInterface(debugInfo, HCaptchaDebugInfo.JS_INTERFACE_TAG);
        webView.loadDataWithBaseURL(config.getBaseUrl(), htmlProvider.getHtml(), "text/html", "UTF-8", null);
        HCaptchaLog.d("WebViewHelper.loadData. Hardware acceleration enabled: %b", webView.isHardwareAccelerated());
    }

    public void destroy() {
        HCaptchaLog.d("WebViewHelper.destroy");

        webView.removeJavascriptInterface(HCaptchaJSInterface.JS_INTERFACE_TAG);
        webView.removeJavascriptInterface(HCaptchaDebugInfo.JS_INTERFACE_TAG);
        final ViewParent parent = webView.getParent();
        if (parent instanceof ViewGroup) {
            ((ViewGroup) parent).removeView(webView);
        } else {
            HCaptchaLog.w("webView.getParent() is null or not a ViewGroup instance");
        }
        webView.destroy();
    }

    public void setVerifyParams(@Nullable HCaptchaVerifyParams params) {
        final HCaptchaVerifyParams verifyParams = params != null ? params :  new HCaptchaVerifyParams();

        // This allows backwards compatibility for deprecated config.rqdata.
        final String rqdata = verifyParams.getRqdata();
        if (rqdata == null || rqdata.isEmpty()) {
            final String configRqdata = config.getRqdata();
            if (configRqdata != null && !configRqdata.isEmpty()) {
                verifyParams.setRqdata(configRqdata);
            }
        }

        if (verifyParams.equals(new HCaptchaVerifyParams())) {
            return;
        }

        try {
            final ObjectMapper objectMapper = new ObjectMapper();
            final String data = objectMapper.writeValueAsString(verifyParams);
            webView.loadUrl("javascript:setData(" + data + ");");
        } catch (Exception e) {
            HCaptchaLog.w("Failed to call javascript:setData(): " + e.getMessage());
        }
    }

    void resetAndExecute(@Nullable HCaptchaVerifyParams verifyParams) {
        this.reset();
        this.setVerifyParams(verifyParams);
        this.execute();
    }

    void execute() {
        try {
            webView.loadUrl("javascript:execute();");
        } catch (Exception e) {
            HCaptchaLog.w("Failed to call javascript:execute(): " + e.getMessage());
        }
    }

    void reset() {
        try {
            webView.loadUrl("javascript:reset();");
        } catch (Exception e) {
            HCaptchaLog.w("Failed to call javascript:reset(): " + e.getMessage());
        }
    }

    public boolean shouldRetry(HCaptchaException exception) {
        return config.getRetryPredicate().shouldRetry(config, exception);
    }

    /**
     * Hands an {@code sms:} link from the MFA "inbound SMS" challenge over to the user's
     * messaging app, pre-filled with the hCaptcha number and the one-time code.
     *
     * <p>Android has no in-app composer that reports back whether the message was sent
     * ({@code SmsManager} needs {@code SEND_SMS}, which Google Play restricts to default SMS
     * handlers), so the hand-off is unavoidable. What it can do is keep the challenge reachable:
     * the messaging app is started inside the host app's task, so {@code back} returns straight
     * to the still-open challenge and its Confirm button.
     *
     * @param url the URL the WebView tried to navigate to
     * @return true when the link was an SMS link and the navigation should be cancelled
     */
    private boolean openSmsComposer(@NonNull final String url) {
        final HCaptchaSmsLink link = HCaptchaSmsLink.parse(url);
        if (link == null) {
            return false;
        }

        final String recipient = link.getRecipient();
        final String body = link.getBody();
        // The recipient and the body are kept out of the log on purpose: the body carries the
        // one-time verification code.
        HCaptchaLog.d("[webview] sms link intercepted, recipient: %b body: %b",
                recipient != null, body != null);

        if (recipient != null && startActivitySafely(smsComposerIntent(recipient, body))) {
            return true;
        }

        // Messaging apps that only understand the raw link with its `?body=` query.
        if (startActivitySafely(new Intent(Intent.ACTION_VIEW, Uri.parse(url)))) {
            return true;
        }

        captchaVerifier.onFailure(new HCaptchaException(
                HCaptchaError.INTERNAL_ERROR, "Messaging app cannot be launched"));
        return true;
    }

    /**
     * Builds the documented pre-filled compose intent: {@code ACTION_SENDTO} on a {@code smsto:}
     * URI, with the message in the {@code sms_body} extra. The body is passed through byte-exact
     * because the backend matches the code against the message it receives.
     */
    private Intent smsComposerIntent(@NonNull final String recipient, @Nullable final String body) {
        final Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse(SMSTO_SCHEME + recipient));
        if (body != null) {
            intent.putExtra(SMS_BODY_EXTRA, body);
            // AOSP Messaging and several OEM clients read EXTRA_TEXT instead.
            intent.putExtra(Intent.EXTRA_TEXT, body);
        }
        return intent;
    }

    private boolean startActivitySafely(@NonNull final Intent intent) {
        // FLAG_ACTIVITY_NEW_TASK puts the messaging app in a task of its own, so `back` from it
        // lands on the launcher rather than on the challenge. It is only needed when there is no
        // Activity to start from; every SDK render mode passes one.
        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }

        try {
            context.startActivity(intent);
            HCaptchaLog.d("[webview] messaging app launched via %s", intent.getAction());
            return true;
        } catch (Exception e) {
            HCaptchaLog.w("[webview] messaging app launch failed: " + e.getMessage());
            return false;
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private class HCaptchaWebClient extends WebViewClient {

        @NonNull
        private final Handler handler;

        HCaptchaWebClient(@NonNull Handler handler) {
            this.handler = handler;
        }

        private String stripUrl(String url) {
            return url.split("[?#]")[0] + "...";
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            return openSmsComposer(request.getUrl().toString());
        }

        @Override
        public WebResourceResponse shouldInterceptRequest (final WebView view, final WebResourceRequest request) {
            final Uri requestUri = request.getUrl();
            if (requestUri != null && requestUri.getScheme() != null && requestUri.getScheme().equals("http")) {
                handler.post(() -> {
                    webView.removeJavascriptInterface(HCaptchaJSInterface.JS_INTERFACE_TAG);
                    webView.removeJavascriptInterface(HCaptchaDebugInfo.JS_INTERFACE_TAG);
                    captchaVerifier.onFailure(new HCaptchaException(HCaptchaError.INSECURE_HTTP_REQUEST_ERROR,
                            "Insecure resource " + requestUri + " requested"));
                });
            }
            return super.shouldInterceptRequest(view, request);
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            HCaptchaLog.d("[webview] onPageStarted " + stripUrl(url));
        }

        @Override
        public void onLoadResource(WebView view, String url) {
            HCaptchaLog.d("[webview] onLoadResource " + stripUrl(url));
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            HCaptchaLog.d("[webview] onPageFinished " + stripUrl(url));
        }

        @Override
        @SuppressWarnings("java:S1874") // another onReceivedError with non-deprecated signature requires 23 API level
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);
            HCaptchaLog.d("[webview] onReceivedError \"%s\" (%d)", description, errorCode);
        }
    }

    private class HCaptchaWebChromeClient extends WebChromeClient {
        @Override
        public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
            if (isUserGesture) {
                try {
                    final WebView.HitTestResult result = view.getHitTestResult();
                    if (result.getType() == WebView.HitTestResult.SRC_ANCHOR_TYPE) {
                        final Uri url = Uri.parse(result.getExtra());
                        final Intent intent = new Intent(Intent.ACTION_VIEW, url);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(intent);
                        HCaptchaLog.d("[webview] opened target=_blank link in browser: " + url);
                        return true;
                    }
                } catch (Exception e) {
                    HCaptchaLog.w("[webview] failed to open target=_blank link in browser: " + e.getMessage());
                }
            }
            return super.onCreateWindow(view, isDialog, isUserGesture, resultMsg);
        }

        @Override
        public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
            HCaptchaLog.d("[webview] onConsoleMessage " + consoleMessage.message());
            return true;
        }

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            HCaptchaLog.d("[webview] onProgressChanged %d%%", newProgress);
        }

        @Override
        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        public void onPermissionRequest(final PermissionRequest request) {
            // Grant only the requested capture resource; audio is never requested, which
            // avoids forcing host apps to declare RECORD_AUDIO.
            boolean wantsCapture = false;
            for (final String resource : request.getResources()) {
                if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(resource)) {
                    wantsCapture = true;
                    break;
                }
            }
            if (wantsCapture) {
                // The requested surface only composites on a hardware layer; the default
                // software layer (disableHardwareAcceleration) would render it blank.
                webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
                request.grant(new String[]{PermissionRequest.RESOURCE_VIDEO_CAPTURE});
                HCaptchaLog.d("[webview] onPermissionRequest granted");
            } else {
                request.deny();
                HCaptchaLog.d("[webview] onPermissionRequest denied");
            }
        }
    }
}
