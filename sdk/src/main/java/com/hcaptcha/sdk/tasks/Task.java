package com.hcaptcha.sdk.tasks;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.hcaptcha.sdk.HCaptchaException;

import java.util.ArrayList;
import java.util.Iterator;
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

    /**
     * Creates a new Task object
     */
    protected Task() {
        this.complete = false;
        this.successful = false;
        this.onSuccessListeners = new ArrayList<>();
        this.onFailureListeners = new ArrayList<>();
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
     * @param hCaptchaException the hCaptcha exception
     */
    protected void setException(@NonNull HCaptchaException hCaptchaException) {
        this.hCaptchaException = hCaptchaException;
        this.successful = false;
        this.complete = true;
        tryCb();
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

    private void tryCb() {
        if (getResult() != null) {
            final Iterator<OnSuccessListener<TResult>> iterator = onSuccessListeners.iterator();
            while (iterator.hasNext()) {
                final OnSuccessListener<TResult> onSuccessListener = iterator.next();
                onSuccessListener.onSuccess(getResult());
                // Remove listener as result was successfully delivered
                iterator.remove();
            }
        }
        if (getException() != null) {
            final Iterator<OnFailureListener> iterator = onFailureListeners.iterator();
            while (iterator.hasNext()) {
                final OnFailureListener onFailureListener = iterator.next();
                onFailureListener.onFailure(getException());
                // Remove listener as exception was successfully delivered
                iterator.remove();
            }
        }
    }

}
