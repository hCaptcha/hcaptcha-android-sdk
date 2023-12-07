package com.hcaptcha.sdk;

import androidx.annotation.Nullable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;


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
    @NonNull
    private final HCaptchaError hCaptchaError;

    @Nullable
    private final String message;

    public HCaptchaException(HCaptchaError error) {
        this(error, null);
    }

    /**
     * @return The {@link HCaptchaError} error
     */
    public HCaptchaError getHCaptchaError() {
        return hCaptchaError;
    }

    /**
     * @return The error id
     */
    public int getStatusCode() {
        return hCaptchaError.getErrorId();
    }

    /**
     * @return The message string
     */
    @Nullable
    @Override
    public String getMessage() {
        return message == null ? hCaptchaError.getMessage() : message;
    }
}
