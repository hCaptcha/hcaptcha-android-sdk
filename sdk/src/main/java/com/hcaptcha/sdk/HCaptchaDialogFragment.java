package com.hcaptcha.sdk;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.BadParcelableException;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.InflateException;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
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

    @NonNull
    private HCaptchaStateListener listener;

    private LinearLayout loadingContainer;

    private float defaultDimAmount = 0.6f;

    private boolean readyForInteraction = false;

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
    public View onCreateView(@Nullable LayoutInflater inflater,
                             @Nullable final ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        HCaptchaLog.d("DialogFragment.onCreateView");
        try {
            final Bundle args = getArguments();
            listener = HCaptchaCompat.getParcelable(args, KEY_LISTENER, HCaptchaStateListener.class);
            final HCaptchaConfig config = HCaptchaCompat.getSerializable(args, KEY_CONFIG, HCaptchaConfig.class);
            final HCaptchaInternalConfig internalConfig = HCaptchaCompat.getSerializable(args,
                    KEY_INTERNAL_CONFIG, HCaptchaInternalConfig.class);

            if (listener == null || config == null || internalConfig == null) {
                // According to Firebase Analytics, there are cases where Bundle args are empty.
                // > 90% of these cases occur on Android 6, and the count of crashes <<< the count of sessions.
                // This is a quick fix to prevent crashes in production
                throw new AssertionError();
            }

            if (inflater == null) {
                throw new InflateException("inflater is null");
            }
            final View rootView = prepareRootView(inflater, container, config);
            HCaptchaLog.d("DialogFragment.onCreateView inflated");

            final HCaptchaWebView webView = prepareWebView(rootView, config);

            loadingContainer = rootView.findViewById(R.id.loadingContainer);
            loadingContainer.setVisibility(Boolean.TRUE.equals(config.getLoading()) ? View.VISIBLE : View.GONE);

            webViewHelper = new HCaptchaWebViewHelper(new Handler(Looper.getMainLooper()),
                    requireContext(), config, internalConfig, this, webView);
            readyForInteraction = false;
            return rootView;
        } catch (AssertionError | BadParcelableException | InflateException | ClassCastException e) {
            HCaptchaLog.w("Cannot create view. Dismissing dialog...");
            // Happens when fragment tries to reconstruct because the activity was killed
            // And thus there is no way of communicating back
            dismiss();
            if (listener != null) {
                listener.onFailure(new HCaptchaException(HCaptchaError.ERROR));
            }
        }
        return null;
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
            if (Boolean.FALSE.equals(webViewHelper.getConfig().getLoading())) {
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
        if (webViewHelper != null && Boolean.TRUE.equals(webViewHelper.getConfig().getLoading())) {
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
            // checkbox will be shown
            readyForInteraction = true;
            hideLoadingContainer();
        }
    }

    @Override
    public void onOpen() {
        assert webViewHelper != null;

        if (webViewHelper.getConfig().getSize() == HCaptchaSize.INVISIBLE) {
            hideLoadingContainer();
        }

        readyForInteraction = true;

        listener.onOpen();
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
                listener.onFailure(exception);
            }
        }
    }

    @Override
    public void onSuccess(final String token) {
        assert webViewHelper != null;
        if (isAdded()) {
            dismissAllowingStateLoss();
        }
        listener.onSuccess(token);
    }

    @Override
    public void startVerification(@NonNull Activity fragmentActivity) {
        final FragmentManager fragmentManager = ((FragmentActivity) fragmentActivity).getSupportFragmentManager();
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
                listener.onFailure(new HCaptchaException(HCaptchaError.ERROR));
            }
        }
    }

    @Override
    public void reset() {
        if (webViewHelper != null) {
            webViewHelper.reset();
        }
        if (isAdded()) {
            dismissAllowingStateLoss();
        }
    }

    private View prepareRootView(@NonNull LayoutInflater inflater,
                                 @Nullable ViewGroup container,
                                 @NonNull HCaptchaConfig config) {
        final View rootView = inflater.inflate(R.layout.hcaptcha_fragment, container, false);
        rootView.setFocusableInTouchMode(true);
        rootView.requestFocus();
        rootView.setOnKeyListener((view, keyCode, event) -> {
            final boolean backDown = keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN;
            if (!backDown) {
                return false;
            }

            final boolean withoutLoadingUI = !readyForInteraction && Boolean.FALSE.equals(config.getLoading());
            if (withoutLoadingUI) {
                return true;
            }

            return webViewHelper != null && webViewHelper.shouldRetry(
                    new HCaptchaException(HCaptchaError.CHALLENGE_CLOSED));
        });

        return rootView;
    }

    private HCaptchaWebView prepareWebView(@NonNull View rootView, @NonNull HCaptchaConfig config) {
        final HCaptchaWebView webView = rootView.findViewById(R.id.webView);
        if (Boolean.FALSE.equals(config.getLoading())) {
            webView.setOnTouchListener((view, event) -> {
                if (!readyForInteraction && isAdded()) {
                    final Activity activity = getActivity();
                    if (activity != null) {
                        activity.dispatchTouchEvent(event);
                        return true;
                    }
                }
                return view.performClick();
            });
        }
        return webView;
    }
}
