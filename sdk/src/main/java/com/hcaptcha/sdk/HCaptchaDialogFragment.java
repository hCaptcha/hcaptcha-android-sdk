package com.hcaptcha.sdk;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.AndroidRuntimeException;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

/**
 * HCaptcha Dialog Fragment Class.
 *
 * Must have `public` modifier, so it can be properly recreated from instance state!
 */
public final class HCaptchaDialogFragment extends DialogFragment implements IHCaptchaVerifier {

    /**
     * Static TAG String
     */
    private static final String TAG = HCaptchaDialogFragment.class.getSimpleName();

    /**
     * Key for passing config to the dialog fragment
     */
    static final String KEY_CONFIG = "hCaptchaConfig";

    /**
     * Key for passing internal config to the dialog fragment
     */
    static final String KEY_INTERNAL_CONFIG = "hCaptchaInternalConfig";

    /**
     * Key for passing listener to the dialog fragment
     */
    static final String KEY_LISTENER = "hCaptchaDialogListener";

    @Nullable
    private HCaptchaWebViewHelper webViewHelper;

    private LinearLayout loadingContainer;

    private float defaultDimAmount = 0.6f;

    /**
     * Creates a new instance
     *
     * @param config   the config
     * @param listener the listener
     * @return a new instance
     */
    public static HCaptchaDialogFragment newInstance(
            @lombok.NonNull final HCaptchaConfig config,
            @lombok.NonNull final HCaptchaInternalConfig internalConfig,
            @lombok.NonNull final HCaptchaStateListener listener
    ) {
        HCaptchaLog.d("DialogFragment.newInstance");
        final Bundle args = new Bundle();
        args.putSerializable(KEY_CONFIG, config);
        args.putSerializable(KEY_INTERNAL_CONFIG, internalConfig);
        args.putParcelable(KEY_LISTENER, listener);
        final HCaptchaDialogFragment hCaptchaDialogFragment = new HCaptchaDialogFragment();
        hCaptchaDialogFragment.setArguments(args);
        return hCaptchaDialogFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NO_FRAME, R.style.HCaptchaDialogTheme);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        HCaptchaLog.d("DialogFragment.onCreateView");
        HCaptchaStateListener listener = null;
        View rootView = null;
        try {
            final Bundle args = getArguments();
            assert args != null;
            listener = HCaptchaCompat.getParcelable(args, KEY_LISTENER, HCaptchaStateListener.class);
            assert listener != null;
            final HCaptchaConfig config = HCaptchaCompat.getSerializable(args, KEY_CONFIG, HCaptchaConfig.class);
            assert config != null;
            final HCaptchaInternalConfig internalConfig = HCaptchaCompat.getSerializable(args,
                    KEY_INTERNAL_CONFIG, HCaptchaInternalConfig.class);
            assert internalConfig != null;

            rootView = inflater.inflate(R.layout.hcaptcha_fragment, container, false);
            rootView.setFocusableInTouchMode(true);
            rootView.requestFocus();
            rootView.setOnKeyListener((view, keyCode, event) -> {
                if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_BACK) {
                    return webViewHelper != null && webViewHelper.shouldRetry(
                            new HCaptchaException(HCaptchaError.CHALLENGE_CLOSED));
                }
                return false;
            });
            HCaptchaLog.d("DialogFragment.onCreateView inflated");

            final WebView webView = rootView.findViewById(R.id.webView);
            loadingContainer = rootView.findViewById(R.id.loadingContainer);
            loadingContainer.setVisibility(config.getLoading() ? View.VISIBLE : View.GONE);
            webViewHelper = new HCaptchaWebViewHelper(new Handler(Looper.getMainLooper()),
                    requireContext(), config, internalConfig, this, listener, webView);
        } catch (AndroidRuntimeException | ClassCastException e) {
            HCaptchaLog.w("Cannot create view. Dismissing dialog...");
            // Happens when fragment tries to reconstruct because the activity was killed
            // And thus there is no way of communicating back
            dismiss();
            if (listener != null) {
                listener.onFailure(new HCaptchaException(HCaptchaError.ERROR));
            }
        }
        return rootView;
    }

    @Override
    public void onDestroy() {
        HCaptchaLog.d("DialogFragment.onDestroy");
        super.onDestroy();
        if (webViewHelper != null) {
            webViewHelper.destroy();
        }
    }

    @Override
    public void onStart() {
        HCaptchaLog.d("DialogFragment.onStart");
        super.onStart();
        final Dialog dialog = getDialog();
        if (dialog != null && webViewHelper != null) {
            final Window window = dialog.getWindow();
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            defaultDimAmount = window.getAttributes().dimAmount;
            if (!webViewHelper.getConfig().getLoading()) {
                // Remove dialog shadow to appear completely invisible
                window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                window.setDimAmount(0);
            }
        }
    }

    @Override
    public void onCancel(@NonNull DialogInterface dialogInterface) {
        HCaptchaLog.d("DialogFragment.onCancel");
        // User canceled the dialog through either `back` button or an outside touch
        super.onCancel(dialogInterface);
        this.onFailure(new HCaptchaException(HCaptchaError.CHALLENGE_CLOSED));
    }

    private void hideLoadingContainer() {
        if (webViewHelper.getConfig().getLoading()) {
            loadingContainer.animate().alpha(0.0f).setDuration(200).setListener(
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
                dialog.getWindow().setDimAmount(defaultDimAmount);
            }
        }
    }

    @Override
    public void onLoaded() {
        assert webViewHelper != null;

        if (webViewHelper.getConfig().getSize() != HCaptchaSize.INVISIBLE) {
            hideLoadingContainer();
        }
    }

    @Override
    public void onOpen() {
        assert webViewHelper != null;

        if (webViewHelper.getConfig().getSize() == HCaptchaSize.INVISIBLE) {
            hideLoadingContainer();
        }

        webViewHelper.getListener().onOpen();
    }

    @Override
    public void onFailure(@NonNull final HCaptchaException exception) {
        final boolean silentRetry = webViewHelper != null && webViewHelper.shouldRetry(exception);
        if (isAdded() && !silentRetry) {
            dismissAllowingStateLoss();
        }
        if (webViewHelper != null) {
            if (silentRetry) {
                webViewHelper.resetAndExecute();
            } else {
                webViewHelper.getListener().onFailure(exception);
            }
        }
    }

    @Override
    public void onSuccess(final String token) {
        assert webViewHelper != null;
        if (isAdded()) {
            dismissAllowingStateLoss();
        }
        webViewHelper.getListener().onSuccess(token);
    }

    @Override
    public void startVerification(@NonNull FragmentActivity fragmentActivity) {
        final FragmentManager fragmentManager = fragmentActivity.getSupportFragmentManager();
        final Fragment oldFragment = fragmentManager.findFragmentByTag(HCaptchaDialogFragment.TAG);
        if (oldFragment != null && oldFragment.isAdded()) {
            HCaptchaLog.w("DialogFragment was already added.");
            return;
        }

        try {
            show(fragmentManager, HCaptchaDialogFragment.TAG);
        } catch (IllegalStateException e) {
            HCaptchaLog.w("DialogFragment.startVerification " + e.getMessage());
            // https://stackoverflow.com/q/14262312/902217
            // Happens if Fragment is stopped i.e. activity is about to destroy on show call
            if (webViewHelper != null) {
                webViewHelper.getListener().onFailure(new HCaptchaException(HCaptchaError.ERROR));
            }
        }
    }

    @Override
    public void reset() {
        assert webViewHelper != null;
        webViewHelper.reset();
        if (isAdded()) {
            dismissAllowingStateLoss();
        }
    }
}
