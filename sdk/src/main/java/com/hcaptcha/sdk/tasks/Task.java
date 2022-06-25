package com.hcaptcha.sdk.tasks;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hcaptcha.sdk.HCaptchaEvent;
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

    private final List<OnEventListener> onEventListeners;

    /**
     * Creates a new Task object
     */
    protected Task() {
        this.onSuccessListeners = new ArrayList<>();
        this.onFailureListeners = new ArrayList<>();
        this.onOpenListeners = new ArrayList<>();
        this.onEventListeners = new ArrayList<>();
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
     * Internal callback which called once '*-callback' fired in js SDK
     */
    protected void captchaEvent(HCaptchaEvent event) {
        for (OnEventListener listener : onEventListeners) {
            listener.onEvent(event);
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
     * @deprecated use {@link #addOnEventListener(OnOpenListener)}
     */
    @Deprecated
    public Task<TResult> addOnOpenListener(@NonNull final OnOpenListener onOpenListener) {
        this.onOpenListeners.add(onOpenListener);
        tryCb();
        return this;
    }

    /**
     * Add a hCaptcha event listener triggered when the hCaptcha state changes
     *
     * @param listener the event listener to be triggered
     * @return current object
     */
    public Task<TResult> addOnEventListener(@NonNull final OnEventListener listener) {
        this.onEventListeners.add(listener);
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
