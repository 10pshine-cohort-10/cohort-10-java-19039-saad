package com.cms.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LogMaskerTest {

    @Test
    void maskIdentifier_hidesEmailLocalPart() {
        assertThat(LogMasker.maskIdentifier("john.doe@example.com")).isEqualTo("j***@example.com");
    }

    @Test
    void maskIdentifier_showsOnlyLastDigitsOfPhone() {
        assertThat(LogMasker.maskIdentifier("+15551234567")).isEqualTo("***67");
    }

    @Test
    void maskIdentifier_handlesEmptyValues() {
        assertThat(LogMasker.maskIdentifier("  ")).isEqualTo("<empty>");
        assertThat(LogMasker.maskIdentifier(null)).isEqualTo("<empty>");
    }
}
