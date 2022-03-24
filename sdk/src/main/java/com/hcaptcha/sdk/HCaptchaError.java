package com.hcaptcha.sdk;

import androidx.annotation.NonNull;

import java.io.Serializable;


/**
 * Enum with all possible hCaptcha errors.
 * It contains both errors related to the android sdk instance and js client errors.
 * More info about js client errors here: https://docs.hcaptcha.com/configuration#error-codes
 */
public enum HCaptchaError implements Serializable {

    /**
     * Internet connection is missing.
     *
     * Make sure AndroidManifest requires internet permission: {@code {@literal <}uses-permission android:name="android.permission.INTERNET" /{@literal >}}
     */
    NETWORK_ERROR(7, "No internet connection"),

    /**
     * Invalid data is not accepted by endpoints.
     */
    INVALID_DATA(8, "Invalid data is not accepted by endpoints"),

    /**
     * User may need to select the checkbox or if invisible programmatically call execute to initiate the challenge again.
     */
    CHALLENGE_ERROR(9, "Challenge encountered error on setup"),

    /**
     * User may need to select the checkbox or if invisible programmatically call execute to initiate the challenge again.
     */
    INTERNAL_ERROR(10, "hCaptcha client encountered an internal error"),

    /**
     * hCaptcha session timed out either the token or challenge expired
     */
    SESSION_TIMEOUT(15, "Session Timeout"),

    /**
     * User closed the challenge by pressing `back` button or touching the outside of the dialog.
     */
    CHALLENGE_CLOSED(30, "Challenge Closed"),

    /**
     * Rate limited due to too many tries.
     */
    RATE_LIMITED(31, "Rate Limited"),

    /**
     * Invalid custom theme
     */
    INVALID_CUSTOM_THEME(32, "Invalid custom theme"),

    /**
     * Generic error for unknown situations - should never happen.
     */
    ERROR(29, "Unknown error");

    private final int errorId;

    private final String message;

    HCaptchaError(final int errorId, final String message) {
        this.errorId = errorId;
        this.message = message;
    }

    /**
     * @return the integer encoding of the enum
     */
    public int getErrorId() {
        return this.errorId;
    }

    /**
     * @return the error message
     */
    public String getMessage() {
        return this.message;
    }

    @NonNull
    @Override
    public String toString() {
        return message;
    }

    /**
     * Finds the enum based on the integer encoding
     *
     * @param errorId the integer encoding
     * @return the {@link HCaptchaError} object
     * @throws RuntimeException when no match
     */
    @NonNull
    public static HCaptchaError fromId(final int errorId) {
        final HCaptchaError[] errors = HCaptchaError.values();
        for (final HCaptchaError error : errors) {
            if (error.errorId == errorId) {
                return error;
            }
        }
        throw new RuntimeException("Unsupported error id: " + errorId);
    }

}
