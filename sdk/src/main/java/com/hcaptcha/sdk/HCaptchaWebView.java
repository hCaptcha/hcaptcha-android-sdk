package com.hcaptcha.sdk;

import android.content.Context;
import android.os.Build;
import android.os.Looper;
import android.util.AttributeSet;
import android.webkit.WebView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

public class HCaptchaWebView extends WebView {
    public HCaptchaWebView(@NonNull Context context) {
        super(context);
    }

    public HCaptchaWebView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public HCaptchaWebView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public HCaptchaWebView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    /**
     * Workaround for crash in WebViewChromium
     * Details:
     *  - https://stackoverflow.com/questions/58519749
     *  - https://github.com/zulip/zulip-mobile/issues/4051#issuecomment-616855833
     */
    @Override
    public boolean onCheckIsTextEditor() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            return super.onCheckIsTextEditor();
        } else {
            return false;
        }
    }
}
