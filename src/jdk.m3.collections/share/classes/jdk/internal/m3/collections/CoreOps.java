/* *****************************************************************************
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
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Dependency-light core operations shared across Synexia modules.
 *
 * <p>This owner contains only JDK-backed string checks, parsing, null/collection helpers,
 * predicates, safe execution and precondition guards. It intentionally has no logging,
 * framework, HTTP, JSON, cache, file-discovery or model-runtime dependency so Java 21 products
 * can reuse the exact same mechanics without importing the broader {@code m3-common}
 * dependency graph.</p>
 *
 * @since 1.0.0
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/utils/CoreOps.java}
 * blob {@code f62b4c55f11578b8163bd9389313b48e98d059cc} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (common non-language batch b28 (canonical, utils, semantic, convergence, io, extracted, validation, relationbits) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class CoreOps {

    private CoreOps() {
        throw new AssertionError("No instances");
    }

    // --- String checks ---
    public static boolean isEmpty(String s) { return s == null || s.isEmpty(); }
    public static boolean isNotEmpty(String s) { return s != null && !s.isEmpty(); }
    public static boolean isBlank(String s) { return s == null || s.isBlank(); }
    public static boolean isNotBlank(String s) { return s != null && !s.isBlank(); }
    public static String trim(String s) { return s == null ? "" : s.trim(); }
    public static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
    public static String trimToEmpty(String s) { return s == null ? "" : s.trim(); }

    // --- Safe parsing ---
    public static int parseInt(String s, int def) {
        if (isBlank(s)) return def;
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return def; }
    }
    public static Optional<Integer> parseIntOpt(String s) {
        if (isBlank(s)) return Optional.empty();
        try { return Optional.of(Integer.parseInt(s.trim())); } catch (NumberFormatException e) { return Optional.empty(); }
    }
    public static long parseLong(String s, long def) {
        if (isBlank(s)) return def;
        try { return Long.parseLong(s.trim()); } catch (NumberFormatException e) { return def; }
    }
    public static Optional<Long> parseLongOpt(String s) {
        if (isBlank(s)) return Optional.empty();
        try { return Optional.of(Long.parseLong(s.trim())); } catch (NumberFormatException e) { return Optional.empty(); }
    }
    public static double parseDouble(String s, double def) {
        if (isBlank(s)) return def;
        try { return Double.parseDouble(s.trim()); } catch (NumberFormatException e) { return def; }
    }
    public static Optional<Double> parseDoubleOpt(String s) {
        if (isBlank(s)) return Optional.empty();
        try { return Optional.of(Double.parseDouble(s.trim())); } catch (NumberFormatException e) { return Optional.empty(); }
    }
    public static boolean parseBoolean(String s, boolean def) {
        if (isBlank(s)) return def;
        String l = s.trim().toLowerCase(Locale.ROOT);
        if (isTrueValue(l)) return true;
        if (isFalseValue(l)) return false;
        return def;
    }
    public static Optional<Boolean> parseBooleanOpt(String s) {
        if (isBlank(s)) return Optional.empty();
        String l = s.trim().toLowerCase(Locale.ROOT);
        if (isTrueValue(l)) return Optional.of(true);
        if (isFalseValue(l)) return Optional.of(false);
        return Optional.empty();
    }
    public static boolean isTrueValue(String s) {
        return "true".equals(s) || "yes".equals(s) || "1".equals(s) || "on".equals(s);
    }
    public static boolean isFalseValue(String s) {
        return "false".equals(s) || "no".equals(s) || "0".equals(s) || "off".equals(s); // DONOR_BUG_FIX (declared, b28): donor tested "of"
    }

    // --- Generic parsing ---
    public static <T> T parse(String s, Function<String, T> p, T def) {
        if (isBlank(s)) return def;
        try {
            T r = p.apply(s.trim());
            return r != null ? r : def;
        } catch (Exception e) {
            return def;
        }
    }
    public static <T> Optional<T> parseOpt(String s, Function<String, T> p) {
        if (isBlank(s)) return Optional.empty();
        try { return Optional.ofNullable(p.apply(s.trim())); } catch (Exception e) { return Optional.empty(); }
    }

    // --- Null handling ---
    public static boolean isNull(Object o) { return o == null; }
    public static boolean nonNull(Object o) { return o != null; }
    @SafeVarargs
    @SuppressWarnings("varargs") // LINT_SUPPRESS (declared, b28): source-retention; the -Werror build rejects [varargs] here
    public static <T> T coalesce(T... vs) {
        if (vs == null) return null;
        return Arrays.stream(vs).filter(Objects::nonNull).findFirst().orElse(null);
    }
    public static <T> T defaultIfNull(T v, T def) { return v != null ? v : def; }
    public static <T> T defaultIfNull(T v, Supplier<T> def) { return v != null ? v : def.get(); }
    public static String defaultIfBlank(String v, String def) { return isNotBlank(v) ? v : def; }
    public static <T> Optional<T> opt(T v) { return Optional.ofNullable(v); }
    public static Optional<String> optString(String v) { return isNotBlank(v) ? Optional.of(v.trim()) : Optional.empty(); }

    // --- Collection checks ---
    public static boolean isEmpty(Collection<?> c) { return c == null || c.isEmpty(); }
    public static boolean isNotEmpty(Collection<?> c) { return c != null && !c.isEmpty(); }
    public static boolean isEmpty(Map<?, ?> m) { return m == null || m.isEmpty(); }
    public static boolean isNotEmpty(Map<?, ?> m) { return m != null && !m.isEmpty(); }
    public static int size(Collection<?> c) { return c == null ? 0 : c.size(); }
    public static int size(Map<?, ?> m) { return m == null ? 0 : m.size(); }
    public static <T> boolean isEmpty(T[] a) { return a == null || a.length == 0; }
    public static <T> boolean isNotEmpty(T[] a) { return a != null && a.length > 0; }
    public static <T> int length(T[] a) { return a == null ? 0 : a.length; }

    // --- Safe execution ---
    public static <T> T safe(Supplier<T> s, T def) {
        try {
            T r = s.get();
            return r != null ? r : def;
        } catch (Exception e) {
            return def;
        }
    }
    public static <T> Optional<T> safeOpt(Supplier<T> s) {
        try { return Optional.ofNullable(s.get()); } catch (Exception e) { return Optional.empty(); }
    }
    public static boolean safeRun(Runnable r) {
        try { r.run(); return true; } catch (Exception e) { return false; }
    }

    // --- Predicates ---
    public static <T> Predicate<T> nullSafe(Predicate<T> p) { return t -> t != null && p.test(t); }
    public static <T> Predicate<T> notNull() { return t -> t != null; }
    public static Predicate<String> notBlank() { return CoreOps::isNotBlank; }

    // --- Assertions ---
    public static <T> T requireNonNull(T obj, String msg) {
        if (obj == null) throw new NullPointerException(msg);
        return obj;
    }
    public static String requireNonBlank(String s, String msg) {
        if (isBlank(s)) throw new IllegalArgumentException(msg);
        return s.trim();
    }
    public static void require(boolean cond, String msg) {
        if (!cond) throw new IllegalArgumentException(msg);
    }
    public static void checkState(boolean cond, String msg) {
        if (!cond) throw new IllegalStateException(msg);
    }

    /** Lightweight CLI retained for compatibility; library callers should use the static API. */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("CoreOps CLI: is-blank|parse-int|parse-long|parse-boolean|coalesce|trim-to-null|list");
            return;
        }
        try {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "list" -> System.out.println("Ops: string checks, safe parsing, null handling, collection checks, safe exec, predicates, assertions");
                case "is-blank" -> System.out.println("Is blank: " + isBlank(args[1]));
                case "is-not-blank" -> System.out.println("Is not blank: " + isNotBlank(args[1]));
                case "parse-int" -> System.out.println("Parsed: " + parseInt(args[1], Integer.parseInt(args[2])));
                case "parse-long" -> System.out.println("Parsed: " + parseLong(args[1], Long.parseLong(args[2])));
                case "parse-boolean" -> System.out.println("Parsed: " + parseBoolean(args[1], Boolean.parseBoolean(args[2])));
                case "coalesce" -> {
                    String[] values = java.util.stream.IntStream.range(1, args.length)
                            .mapToObj(i -> "null".equals(args[i]) ? null : args[i])
                            .toArray(String[]::new);
                    System.out.println("Coalesced: " + coalesce(values));
                }
                case "trim-to-null" -> System.out.println("Trimmed: " + trimToNull(args[1]));
                default -> throw new IllegalArgumentException("Unknown: " + args[0]);
            }
        } catch (RuntimeException e) {
            System.err.println("Failed: " + e.getMessage());
            throw e;
        }
    }
}
