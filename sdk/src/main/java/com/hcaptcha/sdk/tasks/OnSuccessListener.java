package com.hcaptcha.sdk.tasks;


/**
 * A success listener class
 *
 * @param <R> expected result type
 */
public interface OnSuccessListener<R> {

    /**
     * Called when the challenge is successfully completed
     *
     * @param result the hCaptcha token result
     */
    void onSuccess(R result);

}
