package com.hcaptcha.sdk;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.BadParcelableException;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.*;
import android.webkit.*;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import com.hcaptcha.sdk.tasks.OnFailureListener;
import com.hcaptcha.sdk.tasks.OnLoadedListener;
import com.hcaptcha.sdk.tasks.OnSuccessListener;

import static com.hcaptcha.sdk.HCaptchaJSInterface.JS_INTERFACE_TAG;


/**
 * HCaptcha Dialog Fragment Class
 */
public class HCaptchaDialogFragment extends DialogFragment implements
        OnLoadedListener,
        OnSuccessListener<HCaptchaTokenResponse>,
        OnFailureListener {

    /**
     * Static TAG String
     */
    public static final String TAG = HCaptchaDialogFragment.class.getSimpleName();

    /**
     * Key for passing config to the dialog fragment
     */
    public static final String KEY_CONFIG = "hCaptchaConfig";

    /**
     * Key for passing listener to the dialog fragment
     */
    public static final String KEY_LISTENER = "hCaptchaDialogListener";

    private final Handler handler = new Handler(Looper.getMainLooper());

    private HCaptchaJSInterface hCaptchaJsInterface;

    private boolean showLoader;

    private HCaptchaDialogListener hCaptchaDialogListener;

    private View rootView;

    private WebView webView;

    private LinearLayout loadingContainer;

    /**
     * Creates a new instance
     *
     * @param hCaptchaConfig the config
     * @param hCaptchaDialogListener the listener
     * @return a new instance
     */
    public static HCaptchaDialogFragment newInstance(
            @NonNull final HCaptchaConfig hCaptchaConfig,
            @NonNull final HCaptchaDialogListener hCaptchaDialogListener
    ) {
        final Bundle args = new Bundle();
        args.putSerializable(KEY_CONFIG, hCaptchaConfig);
        args.putParcelable(KEY_LISTENER, hCaptchaDialogListener);
        final HCaptchaDialogFragment hCaptchaDialogFragment = new HCaptchaDialogFragment();
        hCaptchaDialogFragment.setArguments(args);
        return hCaptchaDialogFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        assert getArguments() != null;
        try {
            hCaptchaDialogListener = getArguments().getParcelable(KEY_LISTENER);
        } catch (BadParcelableException e) {
            // Happens when fragment tries to reconstruct because the activity was killed
            // And thus there is no way of communicating back
            dismiss();
            return;
        }
        final HCaptchaConfig hCaptchaConfig = (HCaptchaConfig) getArguments().getSerializable(KEY_CONFIG);
        this.hCaptchaJsInterface = new HCaptchaJSInterface(hCaptchaConfig, this, this, this);
        this.showLoader = hCaptchaConfig.getLoading();
        setStyle(STYLE_NO_FRAME, R.style.HCaptchaDialogTheme);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.hcaptcha_fragment, container, false);
        loadingContainer = rootView.findViewById(R.id.loadingContainer);
        loadingContainer.setVisibility(showLoader ? View.VISIBLE : View.GONE);
        webView = rootView.findViewById(R.id.webView);
        setupWebView(webView);
        return rootView;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (webView != null) {
            // Since web view is attached to main activity
            // it will leak until the activity is closed and not upon dialog dismiss
            // To properly clean up upon dialog destroy: remove all views and destroy webview to prevent leaking
            webView.removeJavascriptInterface(JS_INTERFACE_TAG);
            ((ViewGroup) rootView).removeAllViews();
            webView.destroy();
            webView = null;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        final Dialog dialog = getDialog();
        if (dialog != null) {
            final Window window = dialog.getWindow();
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            if (!showLoader) {
                // Remove dialog shadow to appear completely invisible
                window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            }
        }
    }

    /**
     * General setup for the webview:
     * * enables javascript to be able to load and execute hcaptcha api.js
     * * loads custom html page to display challenge and/or checkbox
     *
     * @param webView the webview
     */
    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    private void setupWebView(final WebView webView) {
        final WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setLoadWithOverviewMode(true);
        webView.setBackgroundColor(Color.TRANSPARENT);
        webView.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null);
        webView.addJavascriptInterface(hCaptchaJsInterface, JS_INTERFACE_TAG);
        webView.loadUrl("file:///android_asset/hcaptcha-form.html");
    }

    @Override
    public void onCancel(@NonNull DialogInterface dialogInterface) {
        // User canceled the dialog through either `back` button or an outside touch
        super.onCancel(dialogInterface);
        this.onFailure(new HCaptchaException(HCaptchaError.CHALLENGE_CLOSED));
    }

    @Override
    public void onLoaded() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (showLoader) {
                    loadingContainer.animate().alpha(0.0f).setDuration(200)
                            .setListener(
                                    new AnimatorListenerAdapter() {
                                        @Override
                                        public void onAnimationEnd(Animator animation) {
                                            loadingContainer.setVisibility(View.GONE);
                                        }
                                    });
                } else {
                    // Add back dialog shadow in case the checkbox or challenge is shown
                    final Dialog dialog = getDialog();
                    if (dialog != null) {
                        dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                    }
                }
            }
        });
    }

    @Override
    public void onFailure(@NonNull final HCaptchaException hCaptchaException) {
        if (isAdded()) {
            dismiss();
        }
        handler.post(new Runnable() {
            @Override
            public void run() {
                hCaptchaDialogListener.onFailure(hCaptchaException);
            }
        });
    }

    @Override
    public void onSuccess(final HCaptchaTokenResponse hCaptchaTokenResponse) {
        if (isAdded()) {
            dismiss();
        }
        handler.post(new Runnable() {
            @Override
            public void run() {
                hCaptchaDialogListener.onSuccess(hCaptchaTokenResponse);
            }
        });
    }
}
