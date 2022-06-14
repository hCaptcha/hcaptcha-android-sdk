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
import android.webkit.WebView;
import android.widget.LinearLayout;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import lombok.NonNull;


/**
 * HCaptcha Dialog Fragment Class.
 *
 * Must have `public` modifier, so it can be properly recreated from instance state!
 */
public final class HCaptchaDialogFragment extends DialogFragment implements IHCaptchaVerifier {

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

    @Nullable
    private HCaptchaWebViewHelper webViewHelper;

    private LinearLayout loadingContainer;

    /**
     * Creates a new instance
     *
     * @param config   the config
     * @param listener the listener
     * @return a new instance
     */
    public static HCaptchaDialogFragment newInstance(
            @NonNull final HCaptchaConfig config,
            @NonNull final HCaptchaStateListener listener
    ) {
        final Bundle args = new Bundle();
        args.putSerializable(KEY_CONFIG, config);
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
        final View rootView = inflater.inflate(R.layout.hcaptcha_fragment, container, false);
        assert getArguments() != null;
        final HCaptchaStateListener listener;
        try {
            listener = getArguments().getParcelable(KEY_LISTENER);
        } catch (BadParcelableException e) {
            // Happens when fragment tries to reconstruct because the activity was killed
            // And thus there is no way of communicating back
            dismiss();
            return rootView;
        }
        final HCaptchaConfig config = (HCaptchaConfig) getArguments().getSerializable(KEY_CONFIG);
        final WebView webView = rootView.findViewById(R.id.webView);
        loadingContainer = rootView.findViewById(R.id.loadingContainer);
        loadingContainer.setVisibility(config.getLoading() ? View.VISIBLE : View.GONE);
        webViewHelper = new HCaptchaWebViewHelper(new Handler(Looper.getMainLooper()), requireContext(), config, this, listener, webView);
        return rootView;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (webViewHelper != null) {
            webViewHelper.destroy();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        assert webViewHelper != null;
        final Dialog dialog = getDialog();
        if (dialog != null) {
            final Window window = dialog.getWindow();
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            if (!webViewHelper.getConfig().getLoading()) {
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
        assert webViewHelper != null;
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
            }
        }
    }

    @Override
    public void onOpen() {
        assert webViewHelper != null;
        webViewHelper.getListener().onOpen();
    }

    @Override
    public void onFailure(@NonNull final HCaptchaException hCaptchaException) {
        final boolean silentRetry = webViewHelper != null
                && webViewHelper.getConfig().getResetOnTimeout()
                && hCaptchaException.getHCaptchaError() == HCaptchaError.SESSION_TIMEOUT;
        if (isAdded() && !silentRetry) {
            dismiss();
        }
        if (webViewHelper != null) {
            if (silentRetry) {
                webViewHelper.getWebView().loadUrl("javascript:resetAndExecute();");
            } else {
                webViewHelper.getListener().onFailure(hCaptchaException);
            }
        }
    }

    @Override
    public void onSuccess(final HCaptchaTokenResponse hCaptchaTokenResponse) {
        assert webViewHelper != null;
        if (isAdded()) {
            dismiss();
        }
        webViewHelper.getListener().onSuccess(hCaptchaTokenResponse);
    }

    @Override
    public void startVerification(@NonNull FragmentActivity fragmentActivity) {
        final FragmentManager fragmentManager = fragmentActivity.getSupportFragmentManager();
        final Fragment oldFragment = fragmentManager.findFragmentByTag(HCaptchaDialogFragment.TAG);
        if (oldFragment != null && oldFragment.isAdded()) {
            Log.w(TAG, "Skip. HCaptchaDialogFragment was already added.");
            return;
        }
        show(fragmentManager, HCaptchaDialogFragment.TAG);
    }
}
