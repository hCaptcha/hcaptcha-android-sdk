package com.hcaptcha.sdk;

import lombok.NonNull;

import java.io.Serializable;

public interface IHCaptchaHtmlProvider extends Serializable {

    @NonNull
    String getHtml();
}
