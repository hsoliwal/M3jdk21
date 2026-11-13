/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.tooling.image;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Minimal deterministic command-line parser. */
public final class M3Arguments {
    private final Map<String, List<String>> options;
    private final List<String> trailing;

    private M3Arguments(Map<String, List<String>> options, List<String> trailing) {
        this.options = options;
        this.trailing = trailing;
    }

    /** Parses {@code --key=value}, {@code --key value}, flags, and arguments after {@code --}. */
    public static M3Arguments parse(String[] args, int offset) {
        Map<String, List<String>> options = new LinkedHashMap<>();
        List<String> trailing = new ArrayList<>();
        boolean afterSeparator = false;
        for (int index = offset; index < args.length; index++) {
            String token = args[index];
            if (afterSeparator) {
                trailing.add(token);
                continue;
            }
            if ("--".equals(token)) {
                afterSeparator = true;
                continue;
            }
            if (!token.startsWith("--")) {
                throw new IllegalArgumentException("Expected --option or -- before application arguments: " + token);
            }
            String raw = token.substring(2);
            int equals = raw.indexOf('=');
            String key;
            String value;
            if (equals >= 0) {
                key = raw.substring(0, equals);
                value = raw.substring(equals + 1);
            } else if (index + 1 < args.length && !args[index + 1].startsWith("--")) {
                key = raw;
                value = args[++index];
            } else {
                key = raw;
                value = "true";
            }
            key = normalizeKey(key);
            options.computeIfAbsent(key, ignored -> new ArrayList<>()).add(value);
        }
        Map<String, List<String>> immutable = new LinkedHashMap<>();
        options.forEach((key, values) -> immutable.put(key, List.copyOf(values)));
        return new M3Arguments(Collections.unmodifiableMap(immutable), List.copyOf(trailing));
    }

    /** Returns the required single option. */
    public String require(String key) {
        String value = single(key, null);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required option --" + key);
        }
        return value;
    }

    /** Returns one option or a default. */
    public String single(String key, String defaultValue) {
        List<String> values = options.get(normalizeKey(key));
        if (values == null || values.isEmpty()) {
            return defaultValue;
        }
        if (values.size() != 1) {
            throw new IllegalArgumentException("Option --" + key + " must occur once");
        }
        return values.get(0);
    }

    /** Returns all values of a repeated option. */
    public List<String> all(String key) {
        return options.getOrDefault(normalizeKey(key), List.of());
    }

    /** Returns a path option or a default. */
    public Path path(String key, Path defaultValue) {
        String value = single(key, null);
        return value == null ? defaultValue : Path.of(value).toAbsolutePath().normalize();
    }

    /** Returns a required normalized path option. */
    public Path requirePath(String key) {
        return Path.of(require(key)).toAbsolutePath().normalize();
    }

    /** Returns a boolean flag. */
    public boolean flag(String key) {
        return Boolean.parseBoolean(single(key, "false"));
    }

    /** Returns arguments after {@code --}. */
    public List<String> trailing() {
        return trailing;
    }

    /** Rejects unknown options. */
    public void requireOnly(String... allowed) {
        java.util.Set<String> allowedSet = new java.util.HashSet<>();
        for (String value : allowed) {
            allowedSet.add(normalizeKey(value));
        }
        for (String key : options.keySet()) {
            if (!allowedSet.contains(key)) {
                throw new IllegalArgumentException("Unknown option --" + key);
            }
        }
    }

    private static String normalizeKey(String key) {
        return key.trim().toLowerCase(Locale.ROOT).replace('_', '-');
    }
}
