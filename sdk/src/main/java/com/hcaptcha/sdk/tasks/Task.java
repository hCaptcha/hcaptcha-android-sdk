package com.hcaptcha.sdk.tasks;

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hcaptcha.sdk.HCaptchaError;
import com.hcaptcha.sdk.HCaptchaException;
import com.hcaptcha.sdk.HCaptchaLog;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;


/**
 * Generic task definition which allows registration of listeners for the result/error of the task.
 *
 * @param <R> The result type of the task.
 */
public abstract class Task<R> {

    private boolean complete;

    private boolean successful;

    private R result;

    private HCaptchaException hCaptchaException;

    private final List<OnSuccessListener<R>> onSuccessListeners;

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
    protected R getResult() {
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
    protected void setResult(R result) {
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
     * Schedule timer to expire the token.
     * @param tokenExpiration - token expiration timeout (seconds)
     */
    protected void scheduleCaptchaExpired(final long tokenExpiration) {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                for (OnFailureListener listener : onFailureListeners) {
                    listener.onFailure(new HCaptchaException(HCaptchaError.TOKEN_TIMEOUT));
                }
            }
        }, TimeUnit.SECONDS.toMillis(tokenExpiration));
    }

    /**
     * Add a success listener triggered when the task finishes successfully
     *
     * @param onSuccessListener the success listener to be triggered
     * @return current object
     */
    public Task<R> addOnSuccessListener(@NonNull final OnSuccessListener<R> onSuccessListener) {
        this.onSuccessListeners.add(onSuccessListener);
        tryCb();
        return this;
    }

    /**
     * Remove a success listener
     * @param onSuccessListener the success listener to be removed
     * @return current object
     */
    public Task<R> removeOnSuccessListener(@NonNull final OnSuccessListener<R> onSuccessListener) {
        if (!onSuccessListeners.remove(onSuccessListener)) {
            HCaptchaLog.d("removeOnSuccessListener: %1 not found and cannot be removed", onSuccessListener);
        }
        return this;
    }

    /**
     * Add a failure listener triggered when the task finishes with an exception
     *
     * @param onFailureListener the failure listener to be triggered
     * @return current object
     */
    public Task<R> addOnFailureListener(@NonNull final OnFailureListener onFailureListener) {
        this.onFailureListeners.add(onFailureListener);
        tryCb();
        return this;
    }

    /**
     * Remove a failure listener
     * @param onFailureListener to be removed
     * @return current object
     */
    public Task<R> removeOnFailureListener(@NonNull final OnFailureListener onFailureListener) {
        if (!onFailureListeners.remove(onFailureListener)) {
            HCaptchaLog.d("removeOnFailureListener: %1 not found and cannot be removed", onFailureListener);
        }
        return this;
    }

    /**
     * Add a hCaptcha open listener triggered when the hCaptcha View is displayed
     *
     * @param onOpenListener the open listener to be triggered
     * @return current object
     */
    public Task<R> addOnOpenListener(@NonNull final OnOpenListener onOpenListener) {
        this.onOpenListeners.add(onOpenListener);
        tryCb();
        return this;
    }

    /**
     * Remove a open listener
     * @param onOpenListener to be removed
     * @return current object
     */
    public Task<R> removeOnOpenListener(@NonNull final OnOpenListener onOpenListener) {
        if (!onOpenListeners.remove(onOpenListener)) {
            HCaptchaLog.d("removeOnOpenListener: %1 not found and cannot be removed", onOpenListener);
        }
        return this;
    }

    /**
     * Remove all listeners: success, failure and open listeners
     * @return current object
     */
    public Task<R> removeAllListeners() {
        onSuccessListeners.clear();
        onFailureListeners.clear();
        onOpenListeners.clear();
        return this;
    }

    private void tryCb() {
        boolean shouldReset = false;
        if (getResult() != null) {
            for (OnSuccessListener<R> onSuccessListener : onSuccessListeners) {
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
