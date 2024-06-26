package com.hcaptcha.sdk

enum class HCaptchaEvent {
    Loaded,
    Opened
}
sealed class HCaptchaResponse {
    data class Success(val token: String) : HCaptchaResponse()
    data class Failure(val error: HCaptchaError) : HCaptchaResponse()
    data class Event(val event: HCaptchaEvent) : HCaptchaResponse()
}
