package com.qvety.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class TextNormalizerTest {

    static Stream<Arguments> cases() {
        return Stream.of(
            Arguments.of("hamza above", "أحمد", "احمد"),
            Arguments.of("hamza below", "إبراهيم", "ابراهيم"),
            Arguments.of("madda", "آمنة", "امنه"),
            Arguments.of("wasla", "ٱلله", "الله"),
            Arguments.of("ta marbuta", "فاطمة", "فاطمه"),
            Arguments.of("alef maqsura", "مصطفى", "مصطفي"),
            Arguments.of("tashkeel", "مُحَمَّد", "محمد"),
            Arguments.of("sukun and superscript alef", "عَبْدُ ٱلرَّحْمَٰن", "عبد الرحمن"),
            Arguments.of("kashida", "عبـــد الله", "عبد الله"),
            Arguments.of("arabic-indic digits", "٠١٠١٢٣٤٥٦٧٨", "01012345678"),
            Arguments.of("extended arabic-indic digits", "۰۱۲", "012"),
            Arguments.of("latin lowercase", "MOHAMED Ali", "mohamed ali"),
            Arguments.of("trim and collapse", "  Ahmed \t\n  Ali  ", "ahmed ali"),
            Arguments.of("nfd to nfc", "Café", "café"),
            Arguments.of("mixed scripts and digits", "أحمد Ali ٢", "احمد ali 2"),
            Arguments.of("empty", "", ""),
            Arguments.of("whitespace only", "   ", "")
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cases")
    void folds(String label, String input, String expected) {
        assertThat(TextNormalizer.fold(input)).isEqualTo(expected);
    }

    @Test
    void nullIsEmpty() {
        assertThat(TextNormalizer.fold(null)).isEmpty();
    }

    static Stream<Arguments> variants() {
        return Stream.of(
            Arguments.of("أحمد", "احمد"),
            Arguments.of("فاطمة", "فاطمه"),
            Arguments.of("مُحَمَّد", "محمد"),
            Arguments.of("أحمد محمد", "احمد  محمد"),
            Arguments.of("Ahmed", "AHMED")
        );
    }

    @ParameterizedTest(name = "{0} = {1}")
    @MethodSource("variants")
    void variantsFoldEqual(String a, String b) {
        assertThat(TextNormalizer.fold(a)).isEqualTo(TextNormalizer.fold(b));
    }
}
