package com.gigwave.infrastructure.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads KEY=value pairs from .env into system properties before Spring Boot starts,
 * so application.yml ${MONGODB_URI} etc. resolve from .env when running locally or from IDE.
 * Does not override existing environment variables or system properties.
 */
public final class EnvLoader {

    private static final String[] ENV_CANDIDATES = {
            ".env",           // backend/.env when cwd is backend
            "backend/.env",   // when cwd is project root
            "../.env"         // project root when cwd is backend
    };

    public static void loadIfPresent() {
        String cwd = System.getProperty("user.dir");
        for (String candidate : ENV_CANDIDATES) {
            Path path = Paths.get(cwd, candidate).normalize();
            if (Files.isRegularFile(path)) {
                load(path);
                return;
            }
        }
    }

    private static void load(Path path) {
        try {
            Map<String, String> vars = new HashMap<>();
            for (String line : Files.readAllLines(path)) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int eq = line.indexOf('=');
                if (eq <= 0) continue;
                String key = line.substring(0, eq).trim();
                String value = line.substring(eq + 1).trim();
                if (value.startsWith("\"") && value.endsWith("\""))
                    value = value.substring(1, value.length() - 1).replace("\\\"", "\"");
                else if (value.startsWith("'") && value.endsWith("'"))
                    value = value.substring(1, value.length() - 1).replace("\\'", "'");
                if (!key.isEmpty()) vars.put(key, value);
            }
            for (Map.Entry<String, String> e : vars.entrySet()) {
                String key = e.getKey();
                if (System.getenv(key) == null && System.getProperty(key) == null)
                    System.setProperty(key, e.getValue());
            }
        } catch (Exception ignored) {
            // .env missing or unreadable – use env vars / defaults only
        }
    }

    private EnvLoader() {}
}
