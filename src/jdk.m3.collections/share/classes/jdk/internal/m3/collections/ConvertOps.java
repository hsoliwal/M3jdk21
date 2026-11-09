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

import java.math.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import java.util.Spliterator;
import java.util.stream.*;
/**
 * ConvertOps — Type conversion: String/Object to primitives, hex, binary, collections, arrays, safe casts.
 *
 * @since 1.0.0
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/utils/ConvertOps.java}
 * blob {@code 7c2181381cb8e5230679061c41ca43c4e65704c4} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (common batch b29 (slf4j owners on the PlaceholderLogger facade) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class ConvertOps {
    private static final PlaceholderLogger log = PlaceholderLogger.of(ConvertOps.class);

    private ConvertOps() {}

    // --- String to primitives ---
    public static int toInt(String s) { return toInt(s, 0); }
    public static int toInt(String s, int def) { if (s == null || s.isBlank()) return def; try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return def; } }
    public static OptionalInt toIntOpt(String s) { if (s == null || s.isBlank()) return OptionalInt.empty(); try { return OptionalInt.of(Integer.parseInt(s.trim())); } catch (NumberFormatException e) { return OptionalInt.empty(); } }
    public static long toLong(String s) { return toLong(s, 0L); }
    public static long toLong(String s, long def) { if (s == null || s.isBlank()) return def; try { return Long.parseLong(s.trim()); } catch (NumberFormatException e) { return def; } }
    public static Long toLongOrNull(String s) { if (s == null || s.isBlank()) return null; try { return Long.parseLong(s.trim()); } catch (NumberFormatException e) { return null; } }
    public static OptionalLong toLongOpt(String s) { if (s == null || s.isBlank()) return OptionalLong.empty(); try { return OptionalLong.of(Long.parseLong(s.trim())); } catch (NumberFormatException e) { return OptionalLong.empty(); } }
    public static double toDouble(String s) { return toDouble(s, 0.0); }
    public static double toDouble(String s, double def) { if (s == null || s.isBlank()) return def; try { return Double.parseDouble(s.trim()); } catch (NumberFormatException e) { return def; } }
    public static Double toDoubleOrNull(String s) { if (s == null || s.isBlank()) return null; try { return Double.parseDouble(s.trim()); } catch (NumberFormatException e) { return null; } }
    public static OptionalDouble toDoubleOpt(String s) { if (s == null || s.isBlank()) return OptionalDouble.empty(); try { return OptionalDouble.of(Double.parseDouble(s.trim())); } catch (NumberFormatException e) { return OptionalDouble.empty(); } }
    public static float toFloat(String s) { return toFloat(s, 0.0f); }
    public static float toFloat(String s, float def) { if (s == null || s.isBlank()) return def; try { return Float.parseFloat(s.trim()); } catch (NumberFormatException e) { return def; } }
    public static boolean toBool(String s) { return toBool(s, false); }
    public static boolean toBool(String s, boolean def) { if (s == null || s.isBlank()) return def; String t = s.trim().toLowerCase(Locale.ROOT); return "true".equals(t) || "1".equals(t) || "yes".equals(t) || "y".equals(t) || "on".equals(t); }
    public static Boolean toBoolOrNull(String s) { if (s == null || s.isBlank()) return null; String t = s.trim().toLowerCase(Locale.ROOT); if ("true".equals(t) || "1".equals(t) || "yes".equals(t)) return Boolean.TRUE; if ("false".equals(t) || "0".equals(t) || "no".equals(t)) return Boolean.FALSE; return null; }
    public static BigDecimal toBigDecimal(String s) { return toBigDecimal(s, BigDecimal.ZERO); }
    public static BigDecimal toBigDecimal(String s, BigDecimal def) { if (s == null || s.isBlank()) return def; try { return new BigDecimal(s.trim()); } catch (NumberFormatException e) { return def; } }
    public static BigInteger toBigInteger(String s) { return toBigInteger(s, BigInteger.ZERO); }
    public static BigInteger toBigInteger(String s, BigInteger def) { if (s == null || s.isBlank()) return def; try { return new BigInteger(s.trim()); } catch (NumberFormatException e) { return def; } }

    // --- Object to primitives ---
    public static int toInt(Object o) { return toInt(o, 0); }
    public static int toInt(Object o, int def) { if (o == null) return def; if (o instanceof Number n) return n.intValue(); return toInt(o.toString(), def); }
    public static long toLong(Object o) { return toLong(o, 0L); }
    public static long toLong(Object o, long def) { if (o == null) return def; if (o instanceof Number n) return n.longValue(); return toLong(o.toString(), def); }
    public static double toDouble(Object o) { return toDouble(o, 0.0); }
    public static double toDouble(Object o, double def) { if (o == null) return def; if (o instanceof Number n) return n.doubleValue(); return toDouble(o.toString(), def); }
    public static boolean toBool(Object o) { return toBool(o, false); }
    public static boolean toBool(Object o, boolean def) { if (o == null) return def; if (o instanceof Boolean b) return b; return toBool(o.toString(), def); }
    public static String toString(Object o) { return toString(o, ""); }
    public static String toString(Object o, String def) { return o == null ? def : o.toString(); }

    // --- Hex ---
    public static String toHex(byte[] b) { return b == null ? "" : IntStream.range(0, b.length).mapToObj(i -> String.format(Locale.ROOT, "%02x", b[i])).collect(Collectors.joining()); }
    public static byte[] fromHex(String h) { if (h == null || h.isEmpty()) return new byte[0]; byte[] d = new byte[h.length() / 2]; IntStream.iterate(0, i -> i < h.length(), i -> i + 2).forEach(i -> d[i / 2] = (byte) ((Character.digit(h.charAt(i), 16) << 4) + Character.digit(h.charAt(i + 1), 16))); return d; }
    public static String toHex(int n) { return Integer.toHexString(n); }
    public static String toHex(long n) { return Long.toHexString(n); }
    public static int fromHexInt(String h) { return fromHexInt(h, 0); }
    public static int fromHexInt(String h, int def) { if (h == null || h.isBlank()) return def; try { return Integer.parseInt(h.startsWith("0x") ? h.substring(2) : h, 16); } catch (NumberFormatException e) { return def; } }

    // --- Binary ---
    public static String toBinary(int n) { return Integer.toBinaryString(n); }
    public static String toBinary(long n) { return Long.toBinaryString(n); }
    public static int fromBinaryInt(String b) { return fromBinaryInt(b, 0); }
    public static int fromBinaryInt(String b, int def) { if (b == null || b.isBlank()) return def; try { return Integer.parseInt(b.startsWith("0b") ? b.substring(2) : b, 2); } catch (NumberFormatException e) { return def; } }

    // --- Collections ---
    /* LINT_SUPPRESS (declared, b29) */ @SuppressWarnings("varargs") @SafeVarargs public static <T> List<T> toList(T... items) { return items == null || items.length == 0 ? List.of() : Arrays.stream(items).collect(Collectors.toUnmodifiableList()); }
    /* LINT_SUPPRESS (declared, b29) */ @SuppressWarnings("varargs") @SafeVarargs public static <T> Set<T> toSet(T... items) { return items == null || items.length == 0 ? Set.of() : Arrays.stream(items).collect(Collectors.toUnmodifiableSet()); }
    public static <T> List<T> toList(Collection<T> c) { return c == null ? List.of() : new ArrayList<>(c); }
    public static <T> Set<T> toSet(Collection<T> c) { return c == null ? Set.of() : new HashSet<>(c); }
    public static <T> List<T> toList(Iterable<T> it) { return it == null ? List.of() : StreamSupport.stream(it.spliterator(), false).toList(); }

    // --- Arrays ---
    public static int[] toIntArray(List<Integer> l) { return l == null ? new int[0] : l.stream().mapToInt(i -> i != null ? i : 0).toArray(); }
    public static long[] toLongArray(List<Long> l) { return l == null ? new long[0] : l.stream().mapToLong(v -> v != null ? v : 0L).toArray(); }
    public static double[] toDoubleArray(List<Double> l) { return l == null ? new double[0] : l.stream().mapToDouble(d -> d != null ? d : 0.0).toArray(); }
    public static String[] toStringArray(List<String> l) { return l == null ? new String[0] : l.toArray(new String[0]); }
    public static List<Integer> toIntList(int[] a) { return a == null ? List.of() : Arrays.stream(a).boxed().toList(); }
    public static List<Long> toLongList(long[] a) { return a == null ? List.of() : Arrays.stream(a).boxed().toList(); }
    public static List<Double> toDoubleList(double[] a) { return a == null ? List.of() : Arrays.stream(a).boxed().toList(); }

    // --- Safe casts ---
    @SuppressWarnings("unchecked") public static <T> T cast(Object o, Class<T> t) { return t.isInstance(o) ? (T) o : null; }
    @SuppressWarnings("unchecked") public static <T> T castOrDefault(Object o, Class<T> t, T def) { return t.isInstance(o) ? (T) o : def; }
    @SuppressWarnings("unchecked") public static <T> Optional<T> castOpt(Object o, Class<T> t) { return t.isInstance(o) ? Optional.of((T) o) : Optional.empty(); }

    // --- CLI ---
    public static void main(String[] args) {
        if (args.length == 0) { log.info("ConvertOps CLI: int|long|double|bool|hex|binary|list"); System.exit(1); }
        try { switch (args[0].toLowerCase()) {
            case "list" -> log.info("Ops: toInt, toLong, toDouble, toBool, toHex, fromHex, toBinary, fromBinary, toList, toSet, cast");
            case "int" -> log.info("toInt({}): {}", args[1], toInt(args[1]));
            case "long" -> log.info("toLong({}): {}", args[1], toLong(args[1]));
            case "double" -> log.info("toDouble({}): {}", args[1], toDouble(args[1]));
            case "bool" -> log.info("toBool({}): {}", args[1], toBool(args[1]));
            case "hex" -> log.info(args[1].startsWith("0x") ? "fromHex: " + fromHexInt(args[1]) : "toHex: 0x" + toHex(toInt(args[1])));
            case "binary" -> log.info(args[1].startsWith("0b") ? "fromBin: " + fromBinaryInt(args[1]) : "toBin: 0b" + toBinary(toInt(args[1])));
            default -> { log.error("Unknown: {}", args[0]); System.exit(1); }
        }; } catch (Exception e) { log.error("Failed", e); System.exit(1); }
    }
}
