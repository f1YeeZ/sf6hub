package com.example.hubdemo.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordUtilTest {
    @Test
    void hashUsesBcryptAndMatchesPassword() {
        String hash = PasswordUtil.hash("secret");

        assertThat(hash).startsWith("$2");
        assertThat(PasswordUtil.matches("secret", hash)).isTrue();
        assertThat(PasswordUtil.matches("wrong", hash)).isFalse();
    }

    @Test
    void matchesLegacySha256Hash() {
        String legacyHash = "00000000000000000000000000000000:bcfa9d35208eaff87d3d179ed05636b0b659157926158e3c645c1605a0c7d43f";

        assertThat(PasswordUtil.matches("123456", legacyHash)).isTrue();
    }

    @Test
    void rejectsMalformedLegacyHash() {
        assertThat(PasswordUtil.matches("secret", "not-a-valid-hash")).isFalse();
    }
}
