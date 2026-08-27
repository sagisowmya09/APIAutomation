package org.example.restfulbooker.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Resolves runtime configuration so the suite works the same locally and on Jenkins.
 *
 * Precedence (highest first):
 * 1) JVM system property ({@code -DbaseUri=...}, including values Surefire forwards)
 * 2) Environment variable ({@code RESTFUL_BOOKER_BASE_URI}, etc.)
 * 3) External file {@code config/config.properties} (project root; see {@link #loadProperties()})
 * 4) Built-in defaults
 *
 * Blank values, the literal {@code "null"}, and unresolved Maven placeholders like
 * {@code ${baseUri}} are ignored so a plain {@code mvn clean test} (no -D flags) still works.
 */
public final class TestConfig {

    private static final Properties PROPS = loadProperties();

    private TestConfig() {
    }

    public static String baseUri() {
        String uri = requireUsable(
                System.getProperty("baseUri"),
                System.getenv("RESTFUL_BOOKER_BASE_URI"),
                System.getenv("BASE_URI"),
                PROPS.getProperty("baseUri"),
                "https://restful-booker.herokuapp.com"
        );
        // Avoid baseUri/ + /booking becoming //booking when Jenkins injects a trailing slash
        while (uri.endsWith("/")) {
            uri = uri.substring(0, uri.length() - 1);
        }
        return uri;
    }

    public static String authUsername() {
        return requireUsable(
                System.getProperty("auth.username"),
                System.getenv("RESTFUL_BOOKER_USERNAME"),
                System.getenv("AUTH_USERNAME"),
                PROPS.getProperty("auth.username"),
                "admin"
        );
    }

    public static String authPassword() {
        return requireUsable(
                System.getProperty("auth.password"),
                System.getenv("RESTFUL_BOOKER_PASSWORD"),
                System.getenv("AUTH_PASSWORD"),
                PROPS.getProperty("auth.password"),
                "password123"
        );
    }

    /** TCP connect timeout (ms). Override with -DconnectTimeoutMs or CONNECT_TIMEOUT_MS. */
    public static int connectTimeoutMs() {
        return parsePositiveInt(
                requireUsable(
                        System.getProperty("connectTimeoutMs"),
                        System.getenv("CONNECT_TIMEOUT_MS"),
                        PROPS.getProperty("connectTimeoutMs"),
                        "30000"
                ),
                30_000
        );
    }

    /** Socket/read timeout (ms). Override with -DreadTimeoutMs or READ_TIMEOUT_MS. */
    public static int readTimeoutMs() {
        return parsePositiveInt(
                requireUsable(
                        System.getProperty("readTimeoutMs"),
                        System.getenv("READ_TIMEOUT_MS"),
                        PROPS.getProperty("readTimeoutMs"),
                        "60000"
                ),
                60_000
        );
    }

    /**
     * Loads external config from disk (not classpath), so the same file is edited for local
     * and Jenkins without rebuilding. Search order:
     * <ol>
     *   <li>{@code -Dconfig.file=...} or {@code CONFIG_FILE} env</li>
     *   <li>{@code ./config/config.properties} under {@code user.dir} (Maven/Jenkins workspace)</li>
     *   <li>{@code ./config.properties} under {@code user.dir}</li>
     *   <li>Walk parents of {@code user.dir} for {@code config/config.properties}</li>
     * </ol>
     */
    private static Properties loadProperties() {
        Properties properties = new Properties();
        Path configPath = resolveConfigFile();
        if (configPath == null) {
            return properties;
        }
        try (InputStream in = Files.newInputStream(configPath)) {
            properties.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load config file: " + configPath.toAbsolutePath(), e);
        }
        return properties;
    }

    private static Path resolveConfigFile() {
        String explicit = firstUsable(
                System.getProperty("config.file"),
                System.getenv("CONFIG_FILE"),
                System.getenv("RESTFUL_BOOKER_CONFIG")
        );
        if (explicit != null) {
            Path path = Path.of(explicit).toAbsolutePath().normalize();
            if (!Files.isRegularFile(path)) {
                throw new IllegalStateException("config.file does not exist or is not a file: " + path);
            }
            return path;
        }

        Path userDir = Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
        Path[] candidates = {
                userDir.resolve("config/config.properties"),
                userDir.resolve("config.properties")
        };
        for (Path candidate : candidates) {
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }

        // Surefire/IDE sometimes use a subdirectory as user.dir — walk up looking for the file
        Path dir = userDir;
        for (int i = 0; i < 5 && dir != null; i++) {
            Path nested = dir.resolve("config/config.properties");
            if (Files.isRegularFile(nested)) {
                return nested;
            }
            dir = dir.getParent();
        }
        return null;
    }

    private static String firstUsable(String... values) {
        for (String value : values) {
            if (isUsable(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private static String requireUsable(String... values) {
        String found = firstUsable(values);
        if (found == null) {
            throw new IllegalStateException("No configuration value found");
        }
        return found;
    }

    /**
     * Rejects blanks, the string "null", and unresolved Maven placeholders (${...})
     * that Surefire may forward when a -D property was not supplied.
     */
    static boolean isUsable(String value) {
        if (value == null) {
            return false;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty() || "null".equalsIgnoreCase(trimmed)) {
            return false;
        }
        // Unresolved Maven property, e.g. "${baseUri}"
        return !(trimmed.startsWith("${") && trimmed.endsWith("}"));
    }

    private static int parsePositiveInt(String raw, int fallback) {
        try {
            int parsed = Integer.parseInt(raw.trim());
            return parsed > 0 ? parsed : fallback;
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }
}
