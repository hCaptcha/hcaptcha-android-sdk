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
    private final HCaptchaError hCaptchaError;

    @Nullable
    private final transient HCaptchaRetryer retryer;

    public HCaptchaException(HCaptchaError error) {
        this(error, null);
    }

    HCaptchaException(@NonNull HCaptchaError error, @Nullable HCaptchaRetryer retryer) {
        this.hCaptchaError = error;
        this.retryer = retryer;
    }

    /**
     * @return The {@link HCaptchaError} error
     */
    public HCaptchaError gethCaptchaError() {
        return this.hCaptchaError;
    }

    /**
     * @return The error id
     */
    public int getStatusCode() {
        return this.hCaptchaError.getErrorId();
    }

    /**
     * @return The message string
     */
    @Nullable
    @Override
    public String getMessage() {
        return this.hCaptchaError.getMessage();
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

    boolean shouldRetry() {
        return retryer != null && retryer.shouldRetry();
    }
}
