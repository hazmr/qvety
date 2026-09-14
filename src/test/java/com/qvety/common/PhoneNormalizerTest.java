package com.qvety.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PhoneNormalizerTest {

    @ParameterizedTest
    @CsvSource({
        "01012345678, +201012345678",       // local mobile
        "+201012345678, +201012345678",     // already international
        "0100 123 4567, +201001234567",     // spaces
        "0223456789, +20223456789",         // Cairo landline
        "00201012345678, +201012345678",    // international prefix typed
    })
    void normalizesEgyptianNumbers(String input, String expected) {
        assertThat(PhoneNormalizer.toE164(input)).contains(expected);
    }

    @ParameterizedTest
    @CsvSource({"1012345678", "12345", "abc", "''", "+1 555 0100"})
    void rejectsUnparseableOrInvalid(String input) {
        assertThat(PhoneNormalizer.toE164(input)).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({"a@b.example.com, true", "01012345678, false", "+201012345678, false"})
    void detectsEmails(String input, boolean expected) {
        assertThat(PhoneNormalizer.looksLikeEmail(input)).isEqualTo(expected);
    }
}
