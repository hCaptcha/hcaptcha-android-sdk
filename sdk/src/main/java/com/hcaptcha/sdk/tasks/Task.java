package com.hcaptcha.sdk.tasks;

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hcaptcha.sdk.HCaptchaError;
import com.hcaptcha.sdk.HCaptchaException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;


/**
 * Generic task definition which allows registration of listeners for the result/error of the task.
 *
 * @param <TResult> The result type of the task.
 */
public abstract class Task<TResult> {

    private boolean complete;

    private boolean successful;

    private TResult result;

    private HCaptchaException hCaptchaException;

    private final List<OnSuccessListener<TResult>> onSuccessListeners;

    private final List<OnFailureListener> onFailureListeners;

    private final List<OnOpenListener> onOpenListeners;

    protected final Handler handler = new Handler(Looper.getMainLooper());

    /**
     * Creates a new Task object
     */
    protected Task() {
        this.onSuccessListeners = new ArrayList<>();
        this.onFailureListeners = new ArrayList<>();
        this.onOpenListeners = new ArrayList<>();
        this.reset();
    }

    private void reset() {
        this.complete = false;
        this.successful = false;
        this.result = null;
        this.hCaptchaException = null;
    }

    /**
     * @return if current task is complete or not
     */
    public boolean isComplete() {
        return complete;
    }

    /**
     * @return if current task is successful or not
     */
    public boolean isSuccessful() {
        return successful;
    }

    /**
     * @return the result object
     */
    @Nullable
    protected TResult getResult() {
        return result;
    }

    /**
     * @return the exception if any
     */
    @Nullable
    public HCaptchaException getException() {
        return hCaptchaException;
    }

    /**
     * Sets the task result and marks it as successfully completed
     *
     * @param result the result object
     */
    protected void setResult(TResult result) {
        this.result = result;
        this.successful = true;
        this.complete = true;
        tryCb();
    }

    /**
     * Sets the task exception and marks it as not successfully completed
     *
     * @param exception the hCaptcha exception
     */
    protected void setException(@NonNull HCaptchaException exception) {
        this.hCaptchaException = exception;
        this.successful = false;
        this.complete = true;
        tryCb();
    }

    /**
     * Internal callback which called once 'open-callback' fired in js SDK
     */
    protected void captchaOpened() {
        for (OnOpenListener listener : onOpenListeners) {
            listener.onOpen();
        }
    }

    /**
     * Internal callback which called once 'expired-callback' fired in js SDK
     * @param expirationTimeout - token expiration timeout (seconds)
     */
    protected void scheduleCaptchaExpired(final long expirationTimeout) {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                for (OnFailureListener listener : onFailureListeners) {
                    listener.onFailure(new HCaptchaException(HCaptchaError.TOKEN_TIMEOUT));
                }
            }
        }, TimeUnit.SECONDS.toMillis(expirationTimeout));
    }

    /**
     * Add a success listener triggered when the task finishes successfully
     *
     * @param onSuccessListener the success listener to be triggered
     * @return current object
     */
    public Task<TResult> addOnSuccessListener(@NonNull final OnSuccessListener<TResult> onSuccessListener) {
        this.onSuccessListeners.add(onSuccessListener);
        tryCb();
        return this;
    }

    /**
     * Add a failure listener triggered when the task finishes with an exception
     *
     * @param onFailureListener the failure listener to be triggered
     * @return current object
     */
    public Task<TResult> addOnFailureListener(@NonNull final OnFailureListener onFailureListener) {
        this.onFailureListeners.add(onFailureListener);
        tryCb();
        return this;
    }

    /**
     * Add a hCaptcha open listener triggered when the hCaptcha View is displayed
     *
     * @param onOpenListener the open listener to be triggered
     * @return current object
     */
    public Task<TResult> addOnOpenListener(@NonNull final OnOpenListener onOpenListener) {
        this.onOpenListeners.add(onOpenListener);
        tryCb();
        return this;
    }

    private void tryCb() {
        boolean shouldReset = false;
        if (getResult() != null) {
            for (OnSuccessListener<TResult> onSuccessListener : onSuccessListeners) {
                onSuccessListener.onSuccess(getResult());
                shouldReset = true;
            }
        }
        if (getException() != null) {
            for (OnFailureListener onFailureListener : onFailureListeners) {
                onFailureListener.onFailure(getException());
                shouldReset = true;
            }
        }
        if (shouldReset) {
            reset();
        }
    }

}
