package com.hcaptcha.sdk;

import androidx.annotation.NonNull;
import com.fasterxml.jackson.annotation.JsonValue;

import java.io.Serializable;


/**
 * The hCaptcha challenge orientation
 */
public enum HCaptchaOrientation implements Serializable {

    PORTRAIT("portrait"),

    LANDSCAPE("landscape");

    private final String orientation;

    HCaptchaOrientation(final String orientation) {
        this.orientation = orientation;
    }

    /**
     * @return the hCaptcha api.js string encoding
     */
    public String getOrientation() {
        return this.orientation;
    }

    @JsonValue
    @NonNull
    @Override
    public String toString() {
        return orientation;
    }

}
