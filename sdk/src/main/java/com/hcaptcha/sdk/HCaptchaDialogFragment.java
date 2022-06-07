package com.hcaptcha.sdk;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.BadParcelableException;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.*;
import android.webkit.*;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;


/**
 * HCaptcha Dialog Fragment Class
 */
public class HCaptchaDialogFragment extends DialogFragment implements
        HCaptchaWebViewProvider {

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

    private HCaptchaConfig config;

    private boolean showLoader;

    private boolean resetOnTimeout;

    @NonNull
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

        this.config = hCaptchaConfig;
        this.showLoader = hCaptchaConfig.getLoading();
        this.resetOnTimeout = hCaptchaConfig.getResetOnTimeout();
        setStyle(STYLE_NO_FRAME, R.style.HCaptchaDialogTheme);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.hcaptcha_fragment, container, false);
        loadingContainer = rootView.findViewById(R.id.loadingContainer);
        loadingContainer.setVisibility(showLoader ? View.VISIBLE : View.GONE);
        webView = rootView.findViewById(R.id.webView);
        HCaptchaWebViewHelper.prepare(config, this);
        return rootView;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (webView != null) {
            HCaptchaWebViewHelper.finish(this);
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
                window.setDimAmount(0);
            }
        }
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
    public void onOpen() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                hCaptchaDialogListener.onOpen();
            }
        });
    }

    @Override
    public void onFailure(@NonNull final HCaptchaException hCaptchaException) {
        final boolean silentRetry = resetOnTimeout && hCaptchaException.getHCaptchaError() == HCaptchaError.SESSION_TIMEOUT;
        if (isAdded() && !silentRetry) {
            dismiss();
        }
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (silentRetry) {
                    // Re-open the challenge
                    webView.loadUrl("javascript:resetAndExecute();");
                } else {
                    hCaptchaDialogListener.onFailure(hCaptchaException);
                }
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

    @Override
    public WebView getWebView() {
        return webView;
    }

    @Override
    public void startVerification(@NonNull FragmentActivity fragmentActivity) {
        FragmentManager fragmentManager = fragmentActivity.getSupportFragmentManager();
        Fragment oldFragment = fragmentManager.findFragmentByTag(HCaptchaDialogFragment.TAG);

        if (oldFragment != null && oldFragment.isAdded()) {
            Log.w(TAG, "HCaptchaDialogFragment already added, skip");
            return;
        }

        show(fragmentManager, HCaptchaDialogFragment.TAG);
    }
}
