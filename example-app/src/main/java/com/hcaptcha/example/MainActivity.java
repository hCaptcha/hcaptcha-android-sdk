package com.hcaptcha.example;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.hcaptcha.sdk.*;
import com.hcaptcha.sdk.tasks.OnFailureListener;
import com.hcaptcha.sdk.tasks.OnSuccessListener;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private TextView tokenTextView;

    private TextView errorTextView;

    private HCaptcha hCaptcha;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.tokenTextView = findViewById(R.id.tokenTextView);
        this.errorTextView = findViewById(R.id.errorTextView);
        // For debugging purposes only
        // android.webkit.WebView.setWebContentsDebuggingEnabled(true);

        initHCaptcha(HCaptchaSize.INVISIBLE);
        RadioGroup sizes = findViewById(R.id.sizes);
        sizes.check(R.id.size_invisible);
    }

    private void initHCaptcha(@Nullable HCaptchaSize size) {
        if (size == null) {
            hCaptcha = HCaptcha.getClient(this);
            return;
        }

        final String YOUR_API_SITE_KEY = "10000000-ffff-ffff-ffff-000000000001";
        final HCaptchaConfig config = HCaptchaConfig.builder()
                .siteKey(YOUR_API_SITE_KEY)
                .size(size)
                .loading(true)
                .build();
        hCaptcha = HCaptcha.getClient(this, config);
    }

    private void setTokenTextView(final String text) {
        tokenTextView.setText(text.substring(0, Math.min(100, text.length())) + "...");
        errorTextView.setText("-");
    }

    private void setErrorTextView(final String error) {
        tokenTextView.setText("-");
        errorTextView.setText(error);
    }

    public void onClickHCaptchaInvisible(View v) {
        initHCaptcha(HCaptchaSize.INVISIBLE);
    }

    public void onClickHCaptchaCompact(View v) {
        initHCaptcha(HCaptchaSize.COMPACT);
    }

    public void onClickHCaptchaNormal(View v) {
        initHCaptcha(HCaptchaSize.NORMAL);
    }

    public void onClickHCaptchaNull(View v) {
        initHCaptcha(null);
    }

    public void onClickShowHCaptcha(final View v) {
        setTokenTextView("-");
        hCaptcha.verifyWithHCaptcha()
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
                });
    }

}
