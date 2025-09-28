package com.hcaptcha.example;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.text.InputType;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.hcaptcha.sdk.*;

import java.util.Arrays;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String SITEKEY = "10000000-ffff-ffff-ffff-000000000001";

    private Spinner sizeSpinner;
    private CheckBox hideDialog;
    private CheckBox loading;
    private CheckBox disableHardwareAccel;
    private TextView tokenTextView;
    private TextView errorTextView;
    private TextView phonePrefixInput;
    private androidx.appcompat.widget.SwitchCompat phoneModeSwitch;
    private HCaptcha hCaptcha;
    private HCaptchaTokenResponse tokenResponse;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sizeSpinner = findViewById(R.id.sizes);
        tokenTextView = findViewById(R.id.tokenTextView);
        errorTextView = findViewById(R.id.errorTextView);
        hideDialog = findViewById(R.id.hide_dialog);
        loading = findViewById(R.id.loading);
        disableHardwareAccel = findViewById(R.id.hwAccel);
        phonePrefixInput = findViewById(R.id.phonePrefix);
        phoneModeSwitch = findViewById(R.id.phoneModeSwitch);
        // Initialize phone mode UI and toggle label/color dynamically
        setPhoneModeUi(phoneModeSwitch != null && phoneModeSwitch.isChecked());
        if (phoneModeSwitch != null) {
            phoneModeSwitch.setOnCheckedChangeListener((button, checked) -> setPhoneModeUi(checked));
        }
        final ArrayAdapter<HCaptchaSize> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                Arrays.asList(HCaptchaSize.NORMAL, HCaptchaSize.INVISIBLE, HCaptchaSize.COMPACT));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sizeSpinner.setAdapter(adapter);

        // Toggle verbose webview logs
        final CheckBox debugCheckBox = findViewById(R.id.webViewDebug);
        debugCheckBox.setOnCheckedChangeListener((checkBox, checked) -> {
            android.webkit.WebView.setWebContentsDebuggingEnabled(checked);
        });
    }

    @Override
    protected void onDestroy() {
        hCaptcha.reset();
        super.onDestroy();
    }

    private HCaptchaSize getSizeFromSpinner() {
        return (HCaptchaSize) sizeSpinner.getSelectedItem();
    }

    private HCaptchaConfig getConfig() {
        final HCaptchaSize size = getSizeFromSpinner();
        return HCaptchaConfig.builder()
                .siteKey(SITEKEY)
                .size(size)
                .loading(loading.isChecked())
                .hideDialog(hideDialog.isChecked())
                .disableHardwareAcceleration(disableHardwareAccel.isChecked())
                .tokenExpiration(10)
                .diagnosticLog(true)
                .retryPredicate((config, exception) -> exception.getHCaptchaError() == HCaptchaError.SESSION_TIMEOUT)
                .build();
    }

    private void setTokenTextView(final String text) {
        tokenTextView.setText(text);
        errorTextView.setText("-");
    }

    private void setErrorTextView(final String error) {
        tokenTextView.setText("-");
        errorTextView.setText(error);
    }

    private void setPhoneModeUi(final boolean fullNumberMode) {
        if (phoneModeSwitch != null) {
            phoneModeSwitch.setText(fullNumberMode ? "Phone" : "Prefix");
            phoneModeSwitch.setTextColor(ContextCompat.getColor(this, android.R.color.black));
        }
        if (phonePrefixInput != null) {
            phonePrefixInput.setHint(fullNumberMode ? "Full phone number" : "Phone prefix");
            phonePrefixInput.setInputType(fullNumberMode ? InputType.TYPE_CLASS_PHONE : InputType.TYPE_CLASS_NUMBER);
        }
    }

    public void onClickReset(final View view) {
        if (hCaptcha != null) {
            hCaptcha.reset();
        }
        setTokenTextView("-");
        hCaptcha = null;
    }

    public void onClickSetup(final View v) {
        hCaptcha = HCaptcha.getClient(this).setup(getConfig());
        setupClient(hCaptcha);
    }

    public void onClickVerify(final View v) {
        setTokenTextView("-");
        
        // Always build verifyParams regardless of hCaptcha state
        final String input = phonePrefixInput.getText() != null ? phonePrefixInput.getText().toString().trim() : null;
        final HCaptchaVerifyParams verifyParams;
        if (input != null && !input.isEmpty()) {
            final HCaptchaVerifyParams.HCaptchaVerifyParamsBuilder builder = HCaptchaVerifyParams.builder();
            if (phoneModeSwitch.isChecked()) {
                builder.phoneNumber(input);
            } else {
                builder.phonePrefix(input);
            }
            verifyParams = builder.build();
        } else {
            verifyParams = null;
        }
        
        if (hCaptcha != null) {
            hCaptcha.verifyWithHCaptcha(verifyParams);
        } else {
            hCaptcha = HCaptcha.getClient(this).verifyWithHCaptcha(getConfig(), verifyParams);
            setupClient(hCaptcha);
        }
    }

    public void onMarkUsed(final View v) {
        if (tokenResponse != null) {
            tokenResponse.markUsed();
        }
    }

    public void onHitTest(final View v) {
        Toast.makeText(this, "Hit Test!", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "onHitTest");
    }

    private void setupClient(final HCaptcha hCaptcha) {
        hCaptcha
            .addOnSuccessListener(response -> {
                tokenResponse = response;
                String userResponseToken = response.getTokenResult();
                setTokenTextView(userResponseToken);
            })
            .addOnFailureListener(e -> {
                Log.d(TAG, "hCaptcha failed: " + e.getMessage() + "(" + e.getStatusCode() + ")");
                setErrorTextView(e.getMessage());
                tokenResponse = null;
            })
            .addOnOpenListener(() -> Toast.makeText(MainActivity.this, "hCaptcha shown", Toast.LENGTH_SHORT).show());
    }
}
