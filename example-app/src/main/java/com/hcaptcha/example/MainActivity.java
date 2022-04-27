package com.hcaptcha.example;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import com.hcaptcha.sdk.*;
import com.hcaptcha.sdk.tasks.OnFailureListener;
import com.hcaptcha.sdk.tasks.OnOpenListener;
import com.hcaptcha.sdk.tasks.OnSuccessListener;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private RadioGroup sizeRadioGroup;
    private CheckBox setupCheckBox;
    private TextView tokenTextView;
    private TextView errorTextView;

    private HCaptcha hCaptcha;
    private HCaptchaConfig hCaptchaConfig;

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupCheckBox = findViewById(R.id.setupCheckBox);
        setupCheckBox.setOnCheckedChangeListener((checkBox, checked) -> {
            initHCaptcha(getSizeFromRadio(sizeRadioGroup));
        });

        sizeRadioGroup = findViewById(R.id.sizeRadioGroup);
        sizeRadioGroup.setOnCheckedChangeListener((radioGroup, resId) -> {
            initHCaptcha(getSizeFromRadio(radioGroup));
        });
        sizeRadioGroup.check(R.id.size_invisible);
        tokenTextView = findViewById(R.id.tokenTextView);
        errorTextView = findViewById(R.id.errorTextView);

        CheckBox debugCheckBox = findViewById(R.id.webViewDebug);
        debugCheckBox.setOnCheckedChangeListener((checkBox, checked) -> {
            android.webkit.WebView.setWebContentsDebuggingEnabled(checked);
        });
    }

    private HCaptchaSize getSizeFromRadio(@NonNull RadioGroup radioGroup) {
        switch (radioGroup.getCheckedRadioButtonId()) {
            case R.id.size_invisible: return HCaptchaSize.INVISIBLE;
            case R.id.size_compact: return HCaptchaSize.COMPACT;
            case R.id.size_normal: return HCaptchaSize.NORMAL;
            default: return null;
        }
    }

    private void initHCaptcha(@Nullable HCaptchaSize size) {
        HCaptchaConfig.HCaptchaConfigBuilder builder = HCaptchaConfig.builder()
                .siteKey("10000000-ffff-ffff-ffff-000000000001")
                .loading(true);
        if (size != null) {
            builder.size(size);
        }

        hCaptchaConfig = builder.build();

        hCaptcha = HCaptcha.getClient(this);
        if (setupCheckBox.isChecked()) {
            hCaptcha.setup(hCaptchaConfig);
        }
    }

    private void setTokenTextView(final String text) {
        tokenTextView.setText(text);
        errorTextView.setText("-");
    }

    private void setErrorTextView(final String error) {
        tokenTextView.setText("-");
        errorTextView.setText(error);
    }

    public HCaptcha verifyWithHCaptcha() {
        if (setupCheckBox.isChecked() || getSizeFromRadio(sizeRadioGroup) == null) {
            return hCaptcha.verifyWithHCaptcha();
        }

        return hCaptcha.verifyWithHCaptcha(hCaptchaConfig);
    }

    public void onClickShowHCaptcha(final View v) {
        setTokenTextView("-");
        verifyWithHCaptcha()
                .addOnSuccessListener(new OnSuccessListener<HCaptchaTokenResponse>() {
                    @Override
                    public void onSuccess(HCaptchaTokenResponse response) {
                        String userResponseToken = response.getTokenResult();
                        setTokenTextView(userResponseToken);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(HCaptchaException e) {
                        Log.d(TAG, "hCaptcha failed: " + e.getMessage() + "(" + e.getStatusCode() + ")");
                        setErrorTextView(e.getMessage());
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
