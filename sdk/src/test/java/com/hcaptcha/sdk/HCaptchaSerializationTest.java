package com.hcaptcha.sdk;

import android.os.Build;

import com.hcaptcha.sdk.journeylitics.SerializationCases;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.ParameterizedRobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.skyscreamer.jsonassert.JSONAssert;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

@RunWith(ParameterizedRobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.S_V2)
public class HCaptchaSerializationTest {
    @ParameterizedRobolectricTestRunner.Parameter
    public String name;

    @ParameterizedRobolectricTestRunner.Parameter(1)
    public Object value;

    @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
    public static List<Object[]> cases() {
        final List<Object[]> cases = new ArrayList<>();
        for (Map.Entry<String, Object> entry : SerializationCases.values().entrySet()) {
            cases.add(new Object[] {entry.getKey(), entry.getValue()});
        }
        return cases;
    }

    @Test
    public void matches_prior_jackson_payload() throws Exception {
        try (InputStream stream = getClass().getResourceAsStream("/jackson-2.13.5-payloads.json");
                Scanner scanner = new Scanner(stream, "UTF-8")) {
            final JSONObject baseline = new JSONObject(scanner.useDelimiter("\\A").next());
            final String actual;
            if (value instanceof HCaptchaConfig) {
                actual = ((HCaptchaConfig) value).toJson();
            } else {
                actual = ((HCaptchaVerifyParams) value).toJson();
            }
            JSONAssert.assertEquals(baseline.getJSONObject(name).toString(), actual, true);
        }
    }
}
