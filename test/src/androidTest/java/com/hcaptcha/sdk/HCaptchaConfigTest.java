package com.hcaptcha.sdk;

import static com.hcaptcha.sdk.compose.HCaptchaComposeTest.SITE_KEY;
import static org.junit.Assert.assertTrue;

import android.os.Parcel;

import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.ext.junit.runners.AndroidJUnit4;

@RunWith(AndroidJUnit4.class)
public class HCaptchaConfigTest {

    @Test
    public void testParcelable() {
        HCaptchaConfig config = HCaptchaConfig.builder()
                .siteKey(SITE_KEY)
                .build();
        Parcel parcel = Parcel.obtain();
        parcel.writeSerializable(config);
        assertTrue(parcel.dataSize() > 0);
    }
}
