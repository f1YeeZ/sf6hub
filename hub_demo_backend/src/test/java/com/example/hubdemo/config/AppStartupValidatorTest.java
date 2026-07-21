package com.example.hubdemo.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppStartupValidatorTest {
    @Test
    void rejectsShortJwtSecret() {
        AppStartupValidator validator = new AppStartupValidator(
                "short",
                "https://api.example.com/api",
                "https://example.com",
                "development",
                "local", "", "", "", "", ""
        );

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("JWT_SECRET is required and must be at least 32 characters. Set it in your shell or IDE run configuration.");
    }

    @Test
    void productionRequiresHttpsAndExactCorsOrigins() {
        String secret = "01234567890123456789012345678901";

        assertThatThrownBy(() -> new AppStartupValidator(
                secret, "http://api.example.com/api", "https://example.com", "production",
                "local", "", "", "", "", "").validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("HTTPS");
        assertThatThrownBy(() -> new AppStartupValidator(
                secret, "https://api.example.com/api", "https://*.example.com", "beta",
                "local", "", "", "", "", "").validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("exact HTTPS origins");
    }

    @Test
    void ossModeRequiresCompleteHttpsConfiguration() {
        String secret = "01234567890123456789012345678901";

        assertThatThrownBy(() -> new AppStartupValidator(
                secret, "https://api.example.com/api", "https://example.com", "production",
                "oss", "", "bucket", "id", "secret", "https://cdn.example.com").validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("OSS_ENDPOINT");

        assertThatThrownBy(() -> new AppStartupValidator(
                secret, "https://api.example.com/api", "https://example.com", "production",
                "oss", "http://oss-cn-shanghai.aliyuncs.com", "bucket", "id", "secret", "https://cdn.example.com").validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("HTTPS");
    }
}
