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

import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
/**
 * Ops — Unified utility facade: string/collection/null checks, safe exec, parsing, sleep.
 * Single static import for common operations.
 *
 * @since 1.0.0
 */
/**
 * Ops.
 * @author Synexia
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/utils/Ops.java}
 * blob {@code b7719122f63386b73b09ff8f739513ccaabebc1a} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (common batch b29 (slf4j owners on the PlaceholderLogger facade) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class Ops {
    private static final PlaceholderLogger log = PlaceholderLogger.of(Ops.class);

    private Ops() {}

    // --- String ---
    public static boolean isEmpty(String s) { return s == null || s.isEmpty(); }
    public static boolean isNotEmpty(String s) { return s != null && !s.isEmpty(); }
    public static boolean isBlank(String s) { return s == null || s.isBlank(); }
    public static boolean isNotBlank(String s) { return s != null && !s.isBlank(); }
    public static String trim(String s) { return s == null ? "" : s.trim(); }
    public static String truncate(String s, int max) { if (s == null || s.length() <= max) return s; return max > 3 ? s.substring(0, max - 3) + "..." : s.substring(0, max); }

    // --- Collection ---
    public static boolean isEmpty(Collection<?> c) { return c == null || c.isEmpty(); }
    public static boolean isNotEmpty(Collection<?> c) { return c != null && !c.isEmpty(); }
    public static boolean isEmpty(Map<?, ?> m) { return m == null || m.isEmpty(); }
    public static boolean isNotEmpty(Map<?, ?> m) { return m != null && !m.isEmpty(); }
    public static <T> boolean isEmpty(T[] a) { return a == null || a.length == 0; }
    public static <T> boolean isNotEmpty(T[] a) { return a != null && a.length > 0; }

    // --- Null ---
    public static boolean isNull(Object o) { return o == null; }
    public static boolean nonNull(Object o) { return o != null; }
    public static <T> T requireNonNull(T o, String msg) { if (o == null) throw new NullPointerException(msg); return o; }
    public static String requireNonBlank(String s, String msg) { if (isBlank(s)) throw new IllegalArgumentException(msg); return s.trim(); }
    /* LINT_SUPPRESS (declared, b29) */ @SuppressWarnings("varargs") @SafeVarargs public static <T> T coalesce(T... vs) { if (vs == null) return null; return Arrays.stream(vs).filter(Objects::nonNull).findFirst().orElse(null); }
    public static <T> T defaultIfNull(T v, T def) { return v != null ? v : def; }
    public static String defaultIfBlank(String v, String def) { return isNotBlank(v) ? v : def; }
    public static <T> Optional<T> opt(T v) { return Optional.ofNullable(v); }

    // --- Sleep ---
    public static boolean sleep(Duration d) { try { Thread.sleep(d); return true; } catch (InterruptedException e) { Thread.currentThread().interrupt(); return false; } }
    public static boolean sleepMs(long ms) { return sleep(Duration.ofMillis(ms)); }
    public static boolean sleepSec(long s) { return sleep(Duration.ofSeconds(s)); }

    // --- Safe exec ---
    public static <T> T safe(Supplier<T> s, T def) { try { T r = s.get(); return r != null ? r : def; } catch (Exception e) { return def; } }
    public static <T> Optional<T> safeOpt(Supplier<T> s) { try { return Optional.ofNullable(s.get()); } catch (Exception e) { return Optional.empty(); } }
    public static boolean safeRun(Runnable r) { try { r.run(); return true; } catch (Exception e) { return false; } }

    // --- Parsing ---
    public static int parseInt(String s, int def) { if (isBlank(s)) return def; try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return def; } }
    public static long parseLong(String s, long def) { if (isBlank(s)) return def; try { return Long.parseLong(s.trim()); } catch (NumberFormatException e) { return def; } }
    public static double parseDouble(String s, double def) { if (isBlank(s)) return def; try { return Double.parseDouble(s.trim()); } catch (NumberFormatException e) { return def; } }
    public static boolean parseBoolean(String s, boolean def) { if (isBlank(s)) return def; String l = s.trim().toLowerCase(Locale.ROOT); if ("true".equals(l)||"yes".equals(l)||"1".equals(l)||"on".equals(l)) return true; if ("false".equals(l)||"no".equals(l)||"0".equals(l)||"of".equals(l)) return false; return def; }

    // --- CLI ---
    public static void main(String[] args) {
        if (args.length == 0) { log.info("Ops CLI: list|isblank|coalesce|parse"); System.exit(1); }
        try { switch (args[0].toLowerCase()) {
            case "list" -> log.info("Ops: string, collection, null checks, sleep, safe exec, parsing");
            case "isblank" -> { log.info("isBlank({})={}", args[1], isBlank(args[1])); log.info("isNotBlank({})={}", args[1], isNotBlank(args[1])); }
            case "coalesce" -> { var vs = java.util.stream.IntStream.range(1, args.length).mapToObj(i -> "null".equals(args[i]) ? null : args[i]).toArray(String[]::new); log.info("coalesce={}", coalesce(vs)); }
            case "parse" -> { log.info("int={} long={} double={} bool={}", parseInt(args[1],0), parseLong(args[1],0L), parseDouble(args[1],0.0), parseBoolean(args[1],false)); }
            default -> { log.error("Unknown: {}", args[0]); System.exit(1); }
        }; } catch (Exception e) { log.error("Failed", e); System.exit(1); }
    }
}
