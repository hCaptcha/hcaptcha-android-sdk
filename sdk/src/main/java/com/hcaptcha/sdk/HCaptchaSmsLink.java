package com.hcaptcha.sdk;

import androidx.annotation.Nullable;

import lombok.Getter;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Locale;

/**
 * The recipient and message body carried by an {@code sms:} URL.
 *
 * <p>Covers the RFC 5724 grammar - {@code sms:recipients[;subscriber-params][?query]} - as well as
 * the legacy {@code ;body=} form.
 *
 * <p>The body of an MFA challenge link carries a one-time code that the backend validates against
 * the message it actually receives, so it is passed through byte-exact: percent-decoded and
 * nothing else. It must never be trimmed, reformatted or logged.
 */
@Getter
final class HCaptchaSmsLink {
    private static final String SMS_SCHEME = "sms:";
    private static final String SMSTO_SCHEME = "smsto:";
    private static final String BODY_KEY = "body";
    private static final String CHARSET_NAME = "UTF-8";
    private static final String QUERY_SEPARATORS = "&";
    private static final String HEAD_SEPARATORS = ";&";
    private static final int NOT_FOUND = -1;
    private static final int HEX_RADIX = 16;
    private static final int HEX_SHIFT = 4;
    private static final int ESCAPE_LENGTH = 3;

    /**
     * The first recipient of the link, reduced to the characters a messaging app accepts.
     */
    @Nullable
    private final String recipient;

    /**
     * The pre-filled message body, percent-decoded and otherwise untouched.
     */
    @Nullable
    private final String body;

    private HCaptchaSmsLink(@Nullable final String recipient, @Nullable final String body) {
        this.recipient = recipient;
        this.body = body;
    }

    /**
     * Parses an {@code sms:} or {@code smsto:} URL.
     *
     * @param url the URL the WebView tried to navigate to
     * @return the parsed link, or {@code null} for any URL that is not an SMS link
     */
    @Nullable
    static HCaptchaSmsLink parse(@Nullable final String url) {
        if (url == null) {
            return null;
        }

        final String lowercased = url.toLowerCase(Locale.ROOT);
        final int schemeLength;
        if (lowercased.startsWith(SMS_SCHEME)) {
            schemeLength = SMS_SCHEME.length();
        } else if (lowercased.startsWith(SMSTO_SCHEME)) {
            schemeLength = SMSTO_SCHEME.length();
        } else {
            return null;
        }

        // Tolerate the authority-style `sms://` spelling seen in the wild.
        int start = schemeLength;
        while (start < url.length() && url.charAt(start) == '/') {
            start++;
        }
        final String payload = url.substring(start);

        // The query and the `;` subscriber parameters are separate grammars, so `?` has to be
        // split off first. Folding them together loses the body of a link such as
        // `sms:+15551234567;phone-context=+1?body=code`.
        final int mark = payload.indexOf('?');
        final String head = mark < 0 ? payload : payload.substring(0, mark);
        final String query = mark < 0 ? "" : payload.substring(mark + 1);

        final int headParamsStart = indexOfAny(head, HEAD_SEPARATORS, 0);
        final String recipients = headParamsStart < 0 ? head : head.substring(0, headParamsStart);
        final String headParams = headParamsStart < 0 ? "" : head.substring(headParamsStart + 1);

        // A `;` inside a query is body text, not a separator (RFC 3986 lists it as a sub-delim),
        // so the query is split on `&` alone. The legacy `;body=` form is read from the head.
        String messageBody = value(BODY_KEY, query, QUERY_SEPARATORS);
        if (messageBody == null) {
            messageBody = value(BODY_KEY, headParams, HEAD_SEPARATORS);
        }

        return new HCaptchaSmsLink(firstRecipient(recipients), messageBody);
    }

    /**
     * The challenge only ever targets a single hCaptcha number, so any extra recipients are
     * dropped.
     */
    @Nullable
    private static String firstRecipient(final String list) {
        final int comma = list.indexOf(',');
        final String first = comma < 0 ? list : list.substring(0, comma);
        final String decoded = percentDecode(first);

        // An allowlist rather than a denylist: formatting can arrive as spaces, dashes, parens,
        // dots, or their non-breaking cousins, and anything left in that the messaging app
        // rejects makes it silently drop the recipient.
        final StringBuilder normalized = new StringBuilder(decoded.length());
        for (int index = 0; index < decoded.length(); index++) {
            final char current = decoded.charAt(index);
            if (current == '+' || (current >= '0' && current <= '9')) {
                normalized.append(current);
            }
        }

        return normalized.length() == 0 ? null : normalized.toString();
    }

    /**
     * Reads a single parameter out of a {@code separators}-delimited list.
     *
     * <p>{@code +} is deliberately left alone: it means a literal plus in a URI query (RFC 3986),
     * and only means a space in form encoding, which {@code sms:} links do not use. Decoding
     * happens after splitting so an encoded separator inside a value survives.
     */
    @Nullable
    private static String value(final String key, final String parameters, final String separators) {
        int cursor = 0;
        while (cursor < parameters.length()) {
            final int next = indexOfAny(parameters, separators, cursor);
            final int end = next < 0 ? parameters.length() : next;
            final String item = parameters.substring(cursor, end);
            cursor = end + 1;

            final int equals = item.indexOf('=');
            final String name = equals < 0 ? item : item.substring(0, equals);
            if (key.equalsIgnoreCase(name)) {
                final String decoded = percentDecode(equals < 0 ? "" : item.substring(equals + 1));
                return decoded.isEmpty() ? null : decoded;
            }
        }

        return null;
    }

    private static int indexOfAny(final String value, final String characters, final int from) {
        for (int index = from; index < value.length(); index++) {
            if (characters.indexOf(value.charAt(index)) >= 0) {
                return index;
            }
        }
        return NOT_FOUND;
    }

    /**
     * Percent-decodes as UTF-8.
     *
     * <p>{@link java.net.URLDecoder} is not usable here: it decodes {@code +} as a space, which
     * would corrupt both the recipient and a body that legitimately contains one.
     */
    @SuppressWarnings("java:S4719") // minSdkVersion is 16 and StandardCharsets.UTF_8 is available from 19
    private static String percentDecode(final String value) {
        if (value.indexOf('%') < 0) {
            return value;
        }

        final ByteArrayOutputStream decoded = new ByteArrayOutputStream(value.length());
        final StringBuilder literal = new StringBuilder();
        int index = 0;
        while (index < value.length()) {
            final int escaped = decodeEscape(value, index);
            if (escaped < 0) {
                literal.append(value.charAt(index));
                index++;
            } else {
                flush(literal, decoded);
                decoded.write(escaped);
                index += ESCAPE_LENGTH;
            }
        }
        flush(literal, decoded);

        try {
            return decoded.toString(CHARSET_NAME);
        } catch (UnsupportedEncodingException e) {
            return value;
        }
    }

    /**
     * @return the byte encoded by the {@code %XX} escape at {@code index}, or {@link #NOT_FOUND}
     *         when there is no well-formed escape there.
     */
    private static int decodeEscape(final String value, final int index) {
        if (value.charAt(index) != '%' || index + ESCAPE_LENGTH > value.length()) {
            return NOT_FOUND;
        }

        final int high = Character.digit(value.charAt(index + 1), HEX_RADIX);
        final int low = Character.digit(value.charAt(index + 2), HEX_RADIX);
        if (high < 0 || low < 0) {
            return NOT_FOUND;
        }

        return (high << HEX_SHIFT) + low;
    }

    /**
     * Moves the pending literal run into the byte stream, so a multi-byte character written as
     * literal text and one written as escapes end up in the same encoding.
     */
    private static void flush(final StringBuilder literal, final ByteArrayOutputStream target) {
        if (literal.length() == 0) {
            return;
        }

        try {
            final byte[] bytes = literal.toString().getBytes(CHARSET_NAME);
            target.write(bytes, 0, bytes.length);
        } catch (UnsupportedEncodingException e) {
            HCaptchaLog.w("SmsLink: cannot encode as " + CHARSET_NAME);
        }
        literal.setLength(0);
    }
}
