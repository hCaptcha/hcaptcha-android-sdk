package com.hcaptcha.sdk;

import androidx.annotation.NonNull;
import com.fasterxml.jackson.annotation.JsonValue;

import java.io.Serializable;


/**
 * The hCaptcha checkbox size
 */
public enum HCaptchaSize implements Serializable {

    /**
     * Checkbox is hidden and challenge is automatically displayed.
     */
    INVISIBLE("invisible"),

    /**
     * Checkbox has a 'normal' size and user must press it to show the challenge.
     */
    NORMAL("normal"),

    /**
     * Checkbox has a 'compact' size and user must press it to show the challenge.
     */
    COMPACT("compact");

    private final String size;

    HCaptchaSize(final String size) {
        this.size = size;
    }

    /**
     * @return the hCaptcha api.js string encoding
     */
    public String getSize() {
        return this.size;
    }

    @JsonValue
    @NonNull
    @Override
    public String toString() {
        return size;
    }

}
