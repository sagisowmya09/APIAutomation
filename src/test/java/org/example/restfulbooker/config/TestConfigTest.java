package org.example.restfulbooker.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.example.restfulbooker.reporting.HtmlReportExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("TestConfig value usability (local + Jenkins)")
@ExtendWith(HtmlReportExtension.class)
class TestConfigTest {

    @Test
    void rejectsBlankNullAndUnresolvedMavenPlaceholders() {
        assertFalse(TestConfig.isUsable(null));
        assertFalse(TestConfig.isUsable(""));
        assertFalse(TestConfig.isUsable("   "));
        assertFalse(TestConfig.isUsable("null"));
        assertFalse(TestConfig.isUsable("${baseUri}"));
        assertFalse(TestConfig.isUsable("${auth.username}"));
    }

    @Test
    void acceptsRealValues() {
        assertTrue(TestConfig.isUsable("https://restful-booker.herokuapp.com"));
        assertTrue(TestConfig.isUsable("admin"));
        assertTrue(TestConfig.isUsable("  password123  "));
    }

    @Test
    void baseUri_resolvesWithoutSystemOverrides() {
        // Smoke: plain resolution must not throw and must look like an absolute HTTP(S) URL
        String uri = TestConfig.baseUri();
        assertTrue(uri.startsWith("http://") || uri.startsWith("https://"), uri);
        assertFalse(uri.endsWith("/"), uri);
        assertFalse(uri.contains("${"), uri);
    }
}
