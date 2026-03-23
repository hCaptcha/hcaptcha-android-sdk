package com.hcaptcha.sdk.journeylitics

import androidx.compose.ui.unit.Density
import org.junit.Test

class PressGestureScopeImplTest {
    private fun newInstance(): Any {
        val classNames = listOf(
            "com.hcaptcha.sdk.journeylitics.PressGestureScopeImpl",
            "com.hcaptcha.sdk.journeylitics.ComposeAnalyticsKt\$PressGestureScopeImpl"
        )
        var clazz: Class<*>? = null
        for (name in classNames) {
            try {
                clazz = Class.forName(name)
                break
            } catch (_: ClassNotFoundException) {
                // Try next name
            }
        }
        requireNotNull(clazz) { "PressGestureScopeImpl class not found" }

        val ctor = clazz.getDeclaredConstructor(Density::class.java)
        ctor.isAccessible = true
        val density = object : Density {
            override val density: Float = 1f
            override val fontScale: Float = 1f
        }
        return ctor.newInstance(density)
    }

    @Test
    fun cancelWithoutReset_doesNotThrow() {
        val instance = newInstance()
        val cancel = instance.javaClass.getDeclaredMethod("cancel")
        cancel.isAccessible = true
        cancel.invoke(instance)
    }

    @Test
    fun releaseWithoutReset_doesNotThrow() {
        val instance = newInstance()
        val release = instance.javaClass.getDeclaredMethod("release")
        release.isAccessible = true
        release.invoke(instance)
    }
}
