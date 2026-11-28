package com.cms.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class PhoneNormalizerTest {

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "+1 (555) 123-4567|+15551234567",
            "  +44 20 7946 0958 |+442079460958",
            "555.123.4567|5551234567",
            "+15551234567|+15551234567"
    })
    void normalize_stripsFormattingAndKeepsLeadingPlus(String raw, String expected) {
        assertThat(PhoneNormalizer.normalize(raw)).isEqualTo(expected);
    }

    @Test
    void normalize_returnsNullForNull() {
        assertThat(PhoneNormalizer.normalize(null)).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"+1 555 123 4567", "5551234567", "+442079460958"})
    void isValid_acceptsRealisticNumbers(String raw) {
        assertThat(PhoneNormalizer.isValid(raw)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345", "abc1234567", "+1-555-CALL-NOW", "++15551234567", "1234567890123456", "555-1234 ext 5"})
    void isValid_rejectsMalformedNumbers(String raw) {
        assertThat(PhoneNormalizer.isValid(raw)).isFalse();
    }
}
