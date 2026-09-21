package com.hcaptcha.sdk;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class HCaptchaSmsLinkTest {
    /**
     * The shape the live MFA challenge emits: a pooled hCaptcha number and a full sentence
     * carrying a hyphenated one-time code.
     */
    private static final String REAL_NUMBER = "+46769439873";
    private static final String REAL_BODY = "Return to the app and press Confirm after sending "
            + "this message. Do not edit or share the code: gsuc-djcd-wd6z";
    private static final String REAL_LINK = "sms:+46769439873?body=Return%20to%20the%20app%20and"
            + "%20press%20Confirm%20after%20sending%20this%20message.%20Do%20not%20edit%20or%20"
            + "share%20the%20code%3A%20gsuc-djcd-wd6z";

    /**
     * Captured from a live MFA sitekey on a device. Note the empty first query parameter: the
     * challenge emits `?&body=`, not `?body=`.
     */
    private static final String LIVE_LINK = "sms:+46769432675?&body=Return%20to%20the%20app%20and"
            + "%20press%20Confirm%20after%20sending%20this%20message.%20Do%20not%20edit%20or%20"
            + "share%20the%20code%3A%20mcsp-u3oz-s4du";

    private static final String NUMBER = "+15551234567";
    private static final String CODE = "code";
    private static final String CODE_QUERY = "?body=code";

    /**
     * @param suffix everything after the hCaptcha number in the link
     */
    private static HCaptchaSmsLink parseSuffix(final String suffix) {
        return parseUrl("sms:" + NUMBER + suffix);
    }

    private static HCaptchaSmsLink parseUrl(final String url) {
        final HCaptchaSmsLink link = HCaptchaSmsLink.parse(url);
        assertNotNull(link);
        return link;
    }

    @Test
    public void parsesLiveChallengeLink() {
        final HCaptchaSmsLink link = parseUrl(REAL_LINK);
        assertEquals(REAL_NUMBER, link.getRecipient());
        assertEquals(REAL_BODY, link.getBody());
    }

    @Test
    public void parsesLiveLinkWithEmptyLeadingQueryParameter() {
        final HCaptchaSmsLink link = parseUrl(LIVE_LINK);
        assertEquals("+46769432675", link.getRecipient());
        assertEquals("Return to the app and press Confirm after sending this message. "
                + "Do not edit or share the code: mcsp-u3oz-s4du", link.getBody());
    }

    @Test
    public void skipsEmptyQueryParameters() {
        assertEquals(CODE, parseSuffix("?&body=code").getBody());
        assertEquals(CODE, parseSuffix("?&&body=code").getBody());
        assertEquals(CODE, parseSuffix("?x=1&body=code&").getBody());
    }

    @Test
    public void keepsBodyByteExact() {
        // The code is hyphenated in the SMS body and unhyphenated in "Show Details"; the backend
        // normalizes, so nothing here may trim, reformat or re-case the body.
        assertEquals("  spaced  ", parseSuffix("?body=%20%20spaced%20%20").getBody());
        assertEquals("gsuc-djcd-wd6z", parseSuffix("?body=gsuc-djcd-wd6z").getBody());
    }

    @Test
    public void splitsQueryBeforeSubscriberParams() {
        // RFC 5724 allows `;` subscriber params before the query. Folding the two grammars
        // together loses the body.
        final HCaptchaSmsLink link = parseSuffix(";phone-context=+1" + CODE_QUERY);
        assertEquals(NUMBER, link.getRecipient());
        assertEquals(CODE, link.getBody());
    }

    @Test
    public void treatsSemicolonInQueryAsBodyText() {
        assertEquals("code;more", parseSuffix("?body=code;more").getBody());
    }

    @Test
    public void readsLegacyBodyParameterFromHead() {
        assertEquals("hello", parseSuffix(";body=hello").getBody());
    }

    @Test
    public void keepsPlusInBody() {
        // `+` is a literal plus in a URI query (RFC 3986); only form encoding reads it as a space.
        assertEquals("a+b", parseSuffix("?body=a+b").getBody());
    }

    @Test
    public void decodesUtf8Escapes() {
        assertEquals("café – ok", parseSuffix("?body=caf%C3%A9%20%E2%80%93%20ok").getBody());
    }

    @Test
    public void keepsMalformedEscapesAsLiteralText() {
        assertEquals("100%", parseSuffix("?body=100%25").getBody());
        assertEquals("50%", parseSuffix("?body=50%").getBody());
        assertEquals("%zz", parseSuffix("?body=%zz").getBody());
    }

    @Test
    public void normalizesRecipientFormatting() {
        assertEquals("+123456789", parseUrl("sms:+123-456-789?body=Hello%20World").getRecipient());
        assertEquals(NUMBER, parseUrl("sms:%2B1%20(555)%20123.4567").getRecipient());
    }

    @Test
    public void keepsOnlyTheFirstRecipient() {
        assertEquals(NUMBER, parseSuffix(",+15559999999" + CODE_QUERY).getRecipient());
    }

    @Test
    public void acceptsSmstoAndAuthorityForms() {
        assertEquals(NUMBER, parseUrl("smsto:" + NUMBER + CODE_QUERY).getRecipient());
        assertEquals(CODE, parseUrl("sms://" + NUMBER + CODE_QUERY).getBody());
        assertEquals(CODE, parseUrl("SMS:" + NUMBER + "?BODY=code").getBody());
    }

    @Test
    public void reportsMissingPartsAsNull() {
        assertNull(parseSuffix("").getBody());
        assertNull(parseSuffix("?body=").getBody());
        assertNull(parseSuffix("?subject=hi").getBody());
        assertNull(parseUrl("sms:" + CODE_QUERY).getRecipient());
        assertNull(parseUrl("sms:not-a-number?body=nope").getRecipient());
    }

    @Test
    public void rejectsNonSmsUrls() {
        assertNull(HCaptchaSmsLink.parse(null));
        assertNull(HCaptchaSmsLink.parse("https://example.com" + CODE_QUERY));
        assertNull(HCaptchaSmsLink.parse("tel:" + NUMBER));
        assertNull(HCaptchaSmsLink.parse("smsx:" + NUMBER));
    }
}
