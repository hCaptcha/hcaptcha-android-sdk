package com.hcaptcha.sdk.tasks;


/**
 * A success listener class
 *
 * @param <TResult> expected result type
 */
public interface OnSuccessListener<TResult> {

    /**
     * Called when the challenge is successfully completed
     *
     * @param tResult the hCaptcha token result
     */
    void onSuccess(TResult tResult);

}
