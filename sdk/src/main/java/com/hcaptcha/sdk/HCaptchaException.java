package com.hcaptcha.sdk;

import androidx.annotation.Nullable;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;


/**
 * A checked exception which contains an {@link HCaptchaError} id and message.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class HCaptchaException extends Exception {

    /**
     * The hCaptcha error object
     */
    @NonNull
    private final HCaptchaError error;

    @Nullable
    private final HCaptchaRetryer retryer;

    public HCaptchaException(HCaptchaError error) {
        this(error, null);
    }

    HCaptchaException(@NonNull HCaptchaError error, @Nullable HCaptchaRetryer retryer) {
        this.error = error;
        this.retryer = retryer;
    }

    /**
     * @return The {@link HCaptchaError} error
     */
    public HCaptchaError getError() {
        return this.error;
    }

    /**
     * @return The error id
     */
    public int getStatusCode() {
        return this.error.getErrorId();
    }

    /**
     * @return The message string
     */
    @Nullable
    @Override
    public String getMessage() {
        return this.error.getMessage();
    }

    /**
     * The hCaptcha will retry challenge
     */
    public void retry() {
        if (retryer == null) {
            HCaptchaLog.w("Cannot be retried");
            return;
        }

        retryer.retry();
    }
}
