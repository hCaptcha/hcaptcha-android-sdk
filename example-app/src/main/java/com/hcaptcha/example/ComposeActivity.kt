package com.hcaptcha.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hcaptcha.sdk.HCaptchaCompose
import com.hcaptcha.sdk.HCaptchaConfig
import com.hcaptcha.sdk.HCaptchaResponse

class ComposeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var hCaptchaVisible by remember { mutableStateOf(false) }
            var text by remember { mutableStateOf("") }

            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                // Multiline Text
                TextField(
                    value = text,
                    onValueChange = { newText -> text = newText },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(Color.Gray)
                )

                // Button to toggle WebView visibility
                Button(
                    onClick = {
                        hCaptchaVisible = !hCaptchaVisible
                        // Additional logic when the button is clicked
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Text(text = "Toggle WebView")
                }

                // WebView Dialog
                if (hCaptchaVisible) {
                    HCaptchaCompose(HCaptchaConfig
                        .builder()
                        .build()) { result ->
                        when (result) {
                            is HCaptchaResponse.Success -> {
                                text = "Success: ${result.token}"
                                hCaptchaVisible = false
                                println(text)
                            }
                            is HCaptchaResponse.Failure -> {
                                hCaptchaVisible = false
                                text = "Failure: ${result.error.message}"
                                println(text)
                            }
                            is HCaptchaResponse.Event -> {
                                println("Event: ${result.event}")
                            }

                            else -> {}
                        }
                    }
                }
            }
        }
    }
}

