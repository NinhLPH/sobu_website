package com.vn.sodu.config;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

class WebConfigCorsTest {

    private static final String ORIGIN = "http://localhost:3000";
    private static final String HEADERS = "authorization,content-type,ngrok-skip-browser-warning";
    private static final String[] RESOURCES = {"products", "categories", "brands", "badges", "vouchers"};
    private static final String[] ACTIONS = {"active", "status", "status", "status", "toggle"};

    @ParameterizedTest
    @CsvSource({"MVC,PATCH", "MVC,PUT", "SECURITY,PATCH", "SECURITY,PUT"})
    void allowsFrontendPreflightWithoutInvokingHandlers(String layer, String method) throws Exception {
        CorsFilter filter = new CorsFilter(source(layer));
        for (int i = 0; i < RESOURCES.length; i++) {
            String path = "/api/admin/" + RESOURCES[i] + "/1"
                    + ("PATCH".equals(method) ? "/" + ACTIONS[i] : "");
            MockHttpServletRequest request = preflight(path, ORIGIN, method);
            MockHttpServletResponse response = new MockHttpServletResponse();
            AtomicBoolean handlerInvoked = new AtomicBoolean();

            filter.doFilter(request, response, (req, res) -> handlerInvoked.set(true));

            assertThat(response.getStatus()).as("%s %s %s", layer, method, path).isEqualTo(200);
            assertThat(response.getHeader("Access-Control-Allow-Origin")).isEqualTo(ORIGIN);
            assertThat(response.getHeader("Access-Control-Allow-Methods").split(",\\s*"))
                    .contains(method);
            assertThat(response.getHeader("Access-Control-Allow-Headers").split(",\\s*"))
                    .contains("authorization", "content-type", "ngrok-skip-browser-warning");
            assertThat(response.getHeader("Access-Control-Allow-Credentials")).isEqualTo("true");
            assertThat(response.getHeader("Access-Control-Max-Age")).isEqualTo("3600");
            assertThat(handlerInvoked).isFalse();
        }
    }

    @ParameterizedTest
    @CsvSource({"MVC,PATCH", "MVC,PUT", "SECURITY,PATCH", "SECURITY,PUT"})
    void rejectsUntrustedOrigin(String layer, String method) throws Exception {
        MockHttpServletRequest request = preflight("/api/admin/products/1/active",
                "https://untrusted.example", method);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean handlerInvoked = new AtomicBoolean();

        new CorsFilter(source(layer)).doFilter(request, response, (req, res) -> handlerInvoked.set(true));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getHeader("Access-Control-Allow-Origin")).isNull();
        assertThat(handlerInvoked).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"MVC", "SECURITY"})
    void preservesCorsSettingsAndExistingMethods(String layer) {
        CorsConfiguration configuration = source(layer).getCorsConfiguration(
                preflight("/api/admin/products/1/active", ORIGIN, "PATCH"));

        assertThat(configuration).isNotNull();
        assertThat(configuration.getAllowedMethods())
                .containsExactly("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
        assertThat(configuration.getAllowedOrigins()).containsExactly(ORIGIN);
        assertThat(configuration.getAllowedHeaders()).containsExactly("*");
        assertThat(configuration.getExposedHeaders()).containsExactly("Authorization");
        assertThat(configuration.getAllowCredentials()).isTrue();
        assertThat(configuration.getMaxAge()).isEqualTo(3600L);
        assertThat(source(layer).getCorsConfiguration(preflight("/outside-api", ORIGIN, "PATCH")))
                .isNull();
    }

    private CorsConfigurationSource source(String layer) {
        CorsProperties properties = new CorsProperties();
        properties.setAllowedOrigins(new String[]{ORIGIN});
        WebConfig config = new WebConfig(properties);
        if ("SECURITY".equals(layer)) {
            return config.corsConfigurationSource();
        }
        InspectableCorsRegistry registry = new InspectableCorsRegistry();
        config.addCorsMappings(registry);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        registry.configurations().forEach(source::registerCorsConfiguration);
        return source;
    }

    private MockHttpServletRequest preflight(String path, String origin, String method) {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", path);
        request.addHeader("Origin", origin);
        request.addHeader("Access-Control-Request-Method", method);
        request.addHeader("Access-Control-Request-Headers", HEADERS);
        return request;
    }

    private static class InspectableCorsRegistry extends CorsRegistry {
        Map<String, CorsConfiguration> configurations() {
            return getCorsConfigurations();
        }
    }
}
