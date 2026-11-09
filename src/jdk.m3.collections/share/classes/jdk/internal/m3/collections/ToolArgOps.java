/* ******************************************************************************
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0, or (at your option) the Eclipse
 * Public License 2.0, which are available at
 * https://www.apache.org/licenses/LICENSE-2.0 and
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 *
 * Contributors:
 *     Hitesh Soliwal (Synexia) - initial API and implementation
 *******************************************************************************/
package jdk.internal.m3.collections;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
/**
 * AI Tool argument extraction utilities.
 *
 * @author Hitesh Soliwal
 * @since 1.0.0
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/utils/ToolArgOps.java}
 * blob {@code 5c6363afb88fe1cfeb5cae7701690d5ec494ff35} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (common batch b29 (slf4j owners on the PlaceholderLogger facade) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class ToolArgOps {
    private static final PlaceholderLogger log = PlaceholderLogger.of(ToolArgOps.class);


    private ToolArgOps() {}

    // String extraction
    public static String getString(Map<String, Object> args, String key) {
        Object v = args != null ? args.get(key) : null;
        return v != null ? v.toString() : null;
    }

    public static String getString(Map<String, Object> args, String key, String defaultValue) {
        String v = getString(args, key);
        return v != null && !v.isEmpty() ? v : defaultValue;
    }

    public static String requireString(Map<String, Object> args, String key) {
        String v = getString(args, key);
        if (v == null || v.isEmpty()) throw new IllegalArgumentException("Missing required argument: " + key);
        return v;
    }

    // Numeric extraction
    public static int getInt(Map<String, Object> args, String key, int defaultValue) {
        Object v = args != null ? args.get(key) : null;
        if (v == null) return defaultValue;
        if (v instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(v.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static long getLong(Map<String, Object> args, String key, long defaultValue) {
        Object v = args != null ? args.get(key) : null;
        if (v == null) return defaultValue;
        if (v instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(v.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public static double getDouble(Map<String, Object> args, String key, double defaultValue) {
        Object v = args != null ? args.get(key) : null;
        if (v == null) return defaultValue;
        if (v instanceof Number n) return n.doubleValue();
        try {
            return Double.parseDouble(v.toString());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    // Boolean extraction
    public static boolean getBool(Map<String, Object> args, String key, boolean defaultValue) {
        Object v = args != null ? args.get(key) : null;
        if (v == null) return defaultValue;
        if (v instanceof Boolean b) return b;
        String s = v.toString().toLowerCase(java.util.Locale.ROOT);
        if ("true".equals(s) || "yes".equals(s) || "1".equals(s)) return true;
        if ("false".equals(s) || "no".equals(s) || "0".equals(s)) return false;
        return defaultValue;
    }

    // Collection extraction
    @SuppressWarnings("unchecked")
    public static List<String> getStringList(Map<String, Object> args, String key) {
        Object v = args != null ? args.get(key) : null;
        if (v == null) return List.of();
        if (v instanceof List<?> list)
            return list.stream().map(Object::toString).toList();
        if (v instanceof String s) return s.isEmpty() ? List.of() : List.of(s.split(","));
        return List.of(v.toString());
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> getMap(Map<String, Object> args, String key) {
        Object v = args != null ? args.get(key) : null;
        if (v instanceof Map<?, ?> m) {
            Map<String, Object> result = new LinkedHashMap<>();
            m.forEach((k, val) -> result.put(k.toString(), val));
            return result;
        }
        return Map.of();
    }

    // Enum extraction
    public static <E extends Enum<E>> E getEnum(
            Map<String, Object> args, String key, Class<E> enumClass, E defaultValue) {
        String v = getString(args, key);
        if (v == null || v.isEmpty()) return defaultValue;
        try {
            return Enum.valueOf(enumClass, v.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }

    public static <E extends Enum<E>> E requireEnum(Map<String, Object> args, String key, Class<E> enumClass) {
        String v = requireString(args, key);
        try {
            return Enum.valueOf(enumClass, v.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid value for " + key + ": " + v + ". Expected: "
                    + Arrays.toString(enumClass.getEnumConstants()));
        }
    }

    // Typed extraction
    public static <T> T get(Map<String, Object> args, String key, Function<Object, T> converter, T defaultValue) {
        Object v = args != null ? args.get(key) : null;
        if (v == null) return defaultValue;
        try {
            return converter.apply(v);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public static <T> Optional<T> getOpt(Map<String, Object> args, String key, Class<T> type) {
        Object v = args != null ? args.get(key) : null;
        if (v != null && type.isInstance(v)) return Optional.of(type.cast(v));
        return Optional.empty();
    }

    // Validation
    public static boolean has(Map<String, Object> args, String key) {
        return args != null && args.containsKey(key) && args.get(key) != null;
    }

    public static String requireOneOf(Map<String, Object> args, String... keys) {
        return Arrays.stream(keys)
                .filter(key -> has(args, key))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("One of required: " + String.join(", ", keys)));
    }

    public static void requireAll(Map<String, Object> args, String... keys) {
        List<String> missing =
                Arrays.stream(keys).filter(key -> !has(args, key)).toList();
        if (!missing.isEmpty()) throw new IllegalArgumentException("Missing required: " + String.join(", ", missing));
    }

    // Result building
    public static Map<String, Object> success(String message) {
        return Map.of("success", true, "message", message);
    }

    public static Map<String, Object> success(String message, Object data) {
        return Map.of("success", true, "message", message, "data", data);
    }

    public static Map<String, Object> error(String message) {
        return Map.of("success", false, "error", message);
    }

    public static ResultBuilder result() {
        return new ResultBuilder();
    }

    public static class ResultBuilder {
        /** Creates an instance (explicit form of the default constructor; required by -Xlint:all -Werror). */
        public ResultBuilder() {
        }


        private final Map<String, Object> map = new LinkedHashMap<>();

        public ResultBuilder put(String key, Object value) {
            map.put(key, value);
            return this;
        }

        public ResultBuilder success(boolean success) {
            return put("success", success);
        }

        public ResultBuilder message(String msg) {
            return put("message", msg);
        }

        public ResultBuilder error(String err) {
            return put("error", err);
        }

        public ResultBuilder data(Object data) {
            return put("data", data);
        }

        public Map<String, Object> build() {
            return Map.copyOf(map);
        }
    }

    /**
     * Command-line interface for ToolArgOps demonstrations.
     *
     * <p>
     * Usage:
     *
     * <pre>
     * java jdk.internal.m3.collections.ToolArgOps &lt;command&gt; [args]
     *
     * Commands:
     *   list                  - List all available operations
     *   demo                  - Run demonstration
     *
     * Examples:
     *   java jdk.internal.m3.collections.ToolArgOps demo
     * </pre>
     *
     * @param args
     *            command-line arguments
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            System.exit(1); // FM-40: acceptable in CLI main()
        }

        try {
            String command = args[0].toLowerCase();
            switch (command) {
                case "list" -> printOperations();
                case "demo" -> {
                    Map<String, Object> toolArgs = new LinkedHashMap<>();
                    toolArgs.put("name", "John");
                    toolArgs.put("age", 30);
                    toolArgs.put("enabled", true);
                    toolArgs.put("tags", List.of("java", "kotlin"));

                    log.info("Tool Arguments: {}", toolArgs);
                    log.info("getString(\"name\") = {}", getString(toolArgs, "name"));
                    log.info("getInt(\"age\", 0) = {}", getInt(toolArgs, "age", 0));
                    log.info("getBool(\"enabled\", false) = {}", getBool(toolArgs, "enabled", false));
                    log.info("getStringList(\"tags\") = {}", getStringList(toolArgs, "tags"));

                    Map<String, Object> result = success("Operation completed", toolArgs);
                    log.info("Result: {}", result);
                }
                default -> {
                    log.error("Unknown command: {}", command);
                    printUsage();
                    System.exit(1);
                }
            }
        } catch (Exception e) {
            log.error("Error executing command", e);
            System.exit(1);
        }
    }

    private static void printUsage() {
        log.info("""
				Usage: java jdk.internal.m3.collections.ToolArgOps <command> [args]

				Commands:
				  list                  - List all available operations
				  demo                  - Run demonstration

				Examples:
				  java jdk.internal.m3.collections.ToolArgOps demo
				""");
    }

    private static void printOperations() {
        log.info("""
				ToolArgOps - AI Tool Argument Utilities

				String Extraction:
				  getString             - Get string value
				  requireString         - Get required string

				Numeric Extraction:
				  getInt                - Get int with default
				  getLong               - Get long with default
				  getDouble             - Get double with default

				Boolean Extraction:
				  getBool               - Get boolean with default

				Collection Extraction:
				  getStringList         - Get string list
				  getMap                - Get map

				Enum Extraction:
				  getEnum               - Get enum with default
				  requireEnum           - Get required enum

				Typed Extraction:
				  get                   - Get with custom converter
				  getOpt                - Get as Optional

				Validation:
				  has                   - Check if key exists
				  requireOneOf          - Require at least one key
				  requireAll            - Require all keys

				Result Building:
				  success               - Create success result
				  error                 - Create error result
				  result                - Create result builder
				""");
    }
}
