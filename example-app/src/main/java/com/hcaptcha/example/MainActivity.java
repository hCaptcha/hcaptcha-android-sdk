package com.hcaptcha.example;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import com.hcaptcha.sdk.*;
import com.hcaptcha.sdk.tasks.OnFailureListener;
import com.hcaptcha.sdk.tasks.OnOpenListener;
import com.hcaptcha.sdk.tasks.OnSuccessListener;

import java.util.Arrays;


public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String SITEKEY = "10000000-ffff-ffff-ffff-000000000001";

    private Spinner sizeSpinner;
    private CheckBox hideDialog;
    private CheckBox loading;
    private TextView tokenTextView;
    private TextView errorTextView;
    private HCaptcha hCaptcha;
    private HCaptchaTokenResponse tokenResponse;

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sizeSpinner = findViewById(R.id.sizes);
        tokenTextView = findViewById(R.id.tokenTextView);
        errorTextView = findViewById(R.id.errorTextView);
        hideDialog = findViewById(R.id.hide_dialog);
        loading = findViewById(R.id.loading);
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
                .tokenExpiration(10)
                .diagnosticLog(true)
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

    public void onClickClear(final View v) {
        setTokenTextView("-");
        hCaptcha = null;
    }

    public void onClickSetup(final View v) {
        hCaptcha = HCaptcha.getClient(this).setup(getConfig());
        setupClient(hCaptcha);
    }

    public void onClickVerify(final View v) {
        setTokenTextView("-");
        if (hCaptcha != null) {
            hCaptcha.verifyWithHCaptcha();
        } else {
            hCaptcha = HCaptcha.getClient(this).verifyWithHCaptcha(getConfig());
            setupClient(hCaptcha);
        }
    }

    public void onMarkUsed(final View v) {
        if (tokenResponse != null) {
            tokenResponse.markUsed();
        }
    }

    private void setupClient(final HCaptcha hCaptcha) {
        hCaptcha
            .addOnSuccessListener(new OnSuccessListener<HCaptchaTokenResponse>() {
                @Override
                public void onSuccess(HCaptchaTokenResponse response) {
                    tokenResponse = response;
                    String userResponseToken = response.getTokenResult();
                    setTokenTextView(userResponseToken);
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(HCaptchaException e) {
                    Log.d(TAG, "hCaptcha failed: " + e.getMessage() + "(" + e.getStatusCode() + ")");
                    setErrorTextView(e.getMessage());
                    tokenResponse = null;
                }
            })
            .addOnOpenListener(new OnOpenListener() {
                @Override
                public void onOpen() {
                    Toast.makeText(MainActivity.this, "hCaptcha shown", Toast.LENGTH_SHORT).show();
                }
            });
    }
}
