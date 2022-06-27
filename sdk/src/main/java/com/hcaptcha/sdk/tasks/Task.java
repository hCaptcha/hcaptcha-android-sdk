package com.hcaptcha.sdk.tasks;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hcaptcha.sdk.HCaptchaException;

import java.util.ArrayList;
import java.util.List;


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

    private final List<OnCloseListener> onCloseListeners;

    private final List<OnChallengeExpiredListener> onChallengeExpiredListeners;

    /**
     * Creates a new Task object
     */
    protected Task() {
        this.onSuccessListeners = new ArrayList<>();
        this.onFailureListeners = new ArrayList<>();
        this.onOpenListeners = new ArrayList<>();
        this.onCloseListeners = new ArrayList<>();
        this.onChallengeExpiredListeners = new ArrayList<>();
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
     * Internal callback which called once 'close-callback' fired in js SDK
     */
    protected void captchaClosed() {
        for (OnCloseListener listener : onCloseListeners) {
            listener.onClose();
        }
    }

    /**
     * Internal callback which called once 'chalexpired-callback' fired in js SDK
     */
    protected void captchaChallengeExpired() {
        for (OnChallengeExpiredListener listener : onChallengeExpiredListeners) {
            listener.onChallengeExpired();
        }
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

    /**
     * Add a hCaptcha close listener triggered when the hCaptcha View is closed
     *
     * @param listener the open listener to be triggered
     * @return current object
     */
    public Task<TResult> addOnCloseListener(@NonNull final OnCloseListener listener) {
        this.onCloseListeners.add(listener);
        tryCb();
        return this;
    }

    /**
     * Add a hCaptcha challenge expired listener triggered when the challenge expired
     *
     * @param listener the challenge expired listener to be triggered
     * @return current object
     */
    public Task<TResult> addOnChallengeExpiredListener(@NonNull final OnChallengeExpiredListener listener) {
        this.onChallengeExpiredListeners.add(listener);
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
