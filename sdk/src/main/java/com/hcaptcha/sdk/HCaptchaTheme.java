package com.hcaptcha.sdk;

import androidx.annotation.NonNull;
import com.fasterxml.jackson.annotation.JsonValue;

import java.io.Serializable;


/**
 * The hCaptcha checkbox theme
 */
public enum HCaptchaTheme implements Serializable {

    /**
     * Dark theme
     */
    DARK("dark"),

    /**
     * Light theme
     */
    LIGHT("light"),

    /**
     * Contrast theme
     */
    CONTRAST("contrast");

    private final String theme;

    HCaptchaTheme(final String theme) {
        this.theme = theme;
    }

    /**
     * @return the hCaptcha api.js string encoding
     */
    public String getTheme() {
        return this.theme;
    }

    @JsonValue
    @NonNull
    @Override
    public String toString() {
        return theme;
    }

}
