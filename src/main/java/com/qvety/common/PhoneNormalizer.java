package com.qvety.common;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import java.util.Optional;

/**
 * Phones are stored as E.164 with default region Egypt: "01012345678", "+201012345678" and
 * "0100 123 4567" are the same number. Unparseable input is rejected; +1 is never assumed.
 */
public final class PhoneNormalizer {

    public static final String DEFAULT_REGION = "EG";

    private static final PhoneNumberUtil UTIL = PhoneNumberUtil.getInstance();

    private PhoneNormalizer() {}

    /** @return the E.164 form, or empty when the input is not a valid number for the default region. */
    public static Optional<String> toE164(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        var trimmed = raw.trim();
        // Egyptians type the trunk zero (010..., 02...) or the international form (+20..., 0020...).
        // A bare "1012345678" is a typo we reject rather than guess at; libphonenumber alone would accept it.
        if (!trimmed.startsWith("+") && !trimmed.startsWith("0")) {
            return Optional.empty();
        }
        try {
            var number = UTIL.parse(trimmed, DEFAULT_REGION);
            if (!UTIL.isValidNumber(number)) {
                return Optional.empty();
            }
            return Optional.of(UTIL.format(number, PhoneNumberUtil.PhoneNumberFormat.E164));
        } catch (NumberParseException e) {
            return Optional.empty();
        }
    }

    /** True when the string reads as an email address rather than a phone: contains '@'. */
    public static boolean looksLikeEmail(String identifier) {
        return identifier != null && identifier.contains("@");
    }
}
