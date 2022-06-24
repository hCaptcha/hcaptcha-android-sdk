package com.hcaptcha.sdk;

import androidx.annotation.Nullable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;


/**
 * A checked exception which contains an {@link HCaptchaError} id and message.
 */
@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class HCaptchaException extends Exception {

    /**
     * The hCaptcha error object
     */
    private final HCaptchaError hCaptchaError;

    /**
     * @return The {@link HCaptchaError} error
     */
    public HCaptchaError getHCaptchaError() {
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

}
