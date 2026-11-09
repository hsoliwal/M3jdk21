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

import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
/**
 * Common method references for maximum code reuse.
 *
 * <p>
 * This class provides pre-defined method references and lambdas for common
 * operations. Use static imports for clean code.
 * </p>
 *
 * <h2>Usage</h2>
 *
 * <pre>{@code
 * import static jdk.internal.m3.collections.MethodRefs.*;
 *
 * // Instead of:
 * stream.filter(Objects::nonNull)
 *
 * // Use:
 * stream.filter(notNull())
 *
 * // Compose predicates:
 * stream.filter(notNull().and(notBlank()))
 * }</pre>
 *
 * @since 1.0.0
 *
 * <p>Lineage: absorbed from Synexia {@code m3-common/src/main/java/jdk/internal/m3/common/utils/MethodRefs.java}
 * blob {@code 20246865d0a7586f095a5ff51719d339c5641263} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (common batch b29 (slf4j owners on the PlaceholderLogger facade) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class MethodRefs {
    private static final PlaceholderLogger log = PlaceholderLogger.of(MethodRefs.class);


    private MethodRefs() {
        // Utility class - no instantiation
    }

    // --- PREDICATES - Object Checks ---

    /** Predicate: not null */
    public static <T> Predicate<T> notNull() {
        return Objects::nonNull;
    }

    /** Predicate: is null */
    public static <T> Predicate<T> isNull() {
        return Objects::isNull;
    }

    /** Predicate: equals value */
    public static <T> Predicate<T> eq(T value) {
        return t -> Objects.equals(t, value);
    }

    /** Predicate: not equals value */
    public static <T> Predicate<T> neq(T value) {
        return t -> !Objects.equals(t, value);
    }

    /** Predicate: instance of class */
    public static <T> Predicate<T> instanceOf(Class<?> clazz) {
        return t -> clazz.isInstance(t);
    }

    // --- PREDICATES - String Checks ---

    /** Predicate: string not blank */
    public static Predicate<String> notBlank() {
        return s -> s != null && !s.isBlank();
    }

    /** Predicate: string is blank */
    public static Predicate<String> isBlank() {
        return s -> s == null || s.isBlank();
    }

    /** Predicate: string not empty */
    public static Predicate<String> notEmpty() {
        return s -> s != null && !s.isEmpty();
    }

    /** Predicate: string is empty */
    public static Predicate<String> isEmpty() {
        return s -> s == null || s.isEmpty();
    }

    /** Predicate: string starts with prefix */
    public static Predicate<String> startsWith(String prefix) {
        return s -> s != null && s.startsWith(prefix);
    }

    /** Predicate: string ends with suffix */
    public static Predicate<String> endsWith(String suffix) {
        return s -> s != null && s.endsWith(suffix);
    }

    /** Predicate: string contains substring */
    public static Predicate<String> contains(String sub) {
        return s -> s != null && s.contains(sub);
    }

    /** Predicate: string matches regex */
    public static Predicate<String> matches(String regex) {
        return s -> s != null && s.matches(regex);
    }

    /** Predicate: string length equals */
    public static Predicate<String> lengthEq(int len) {
        return s -> s != null && s.length() == len;
    }

    /** Predicate: string length greater than */
    public static Predicate<String> lengthGt(int len) {
        return s -> s != null && s.length() > len;
    }

    /** Predicate: string length less than */
    public static Predicate<String> lengthLt(int len) {
        return s -> s != null && s.length() < len;
    }

    // --- PREDICATES - Number Checks ---

    /** Predicate: number > value */
    public static <T extends Comparable<T>> Predicate<T> gt(T value) {
        return t -> t != null && t.compareTo(value) > 0;
    }

    /** Predicate: number >= value */
    public static <T extends Comparable<T>> Predicate<T> gte(T value) {
        return t -> t != null && t.compareTo(value) >= 0;
    }

    /** Predicate: number &lt; value */
    public static <T extends Comparable<T>> Predicate<T> lt(T value) {
        return t -> t != null && t.compareTo(value) < 0;
    }

    /** Predicate: number &lt;= value */
    public static <T extends Comparable<T>> Predicate<T> lte(T value) {
        return t -> t != null && t.compareTo(value) <= 0;
    }

    /** Predicate: value in range [min, max] */
    public static <T extends Comparable<T>> Predicate<T> between(T min, T max) {
        return t -> t != null && t.compareTo(min) >= 0 && t.compareTo(max) <= 0;
    }

    /** Predicate: int is positive */
    public static Predicate<Integer> positive() {
        return n -> n != null && n > 0;
    }

    /** Predicate: int is negative */
    public static Predicate<Integer> negative() {
        return n -> n != null && n < 0;
    }

    /** Predicate: int is zero */
    public static Predicate<Integer> zero() {
        return n -> n != null && n == 0;
    }

    // --- FUNCTIONS - Transformers ---

    /** Function: identity */
    public static <T> Function<T, T> identity() {
        return Function.identity();
    }

    /** Function: to string */
    public static <T> Function<T, String> toStr() {
        return Object::toString;
    }

    /** Function: safe to string (null -> empty) */
    public static <T> Function<T, String> toStrSafe() {
        return t -> t == null ? "" : t.toString();
    }

    /** Function: trim string */
    public static Function<String, String> trim() {
        return s -> s == null ? "" : s.trim();
    }

    /** Function: uppercase string */
    public static Function<String, String> upper() {
        return s -> s == null ? null : s.toUpperCase(java.util.Locale.ROOT);
    }

    /** Function: lowercase string */
    public static Function<String, String> lower() {
        return s -> s == null ? null : s.toLowerCase(java.util.Locale.ROOT);
    }

    /** Function: get string length */
    public static Function<String, Integer> length() {
        return s -> s == null ? 0 : s.length();
    }

    /** Function: wrap in Optional */
    public static <T> Function<T, Optional<T>> toOpt() {
        return Optional::ofNullable;
    }

    // --- CONSUMERS ---

    /** Consumer: no-op */
    public static <T> Consumer<T> noop() {
        return t -> {};
    }

    /**
     * Consumer that logs items at debug level.
     *
     * @param <T>
     *            item type
     * @return consumer that logs
     */
    public static <T> Consumer<T> print() {
        return t -> log.debug("{}", t);
    }

    /**
     * Consumer that logs items with prefix at debug level.
     *
     * @param prefix
     *            prefix to prepend
     * @param <T>
     *            item type
     * @return consumer that logs with prefix
     */
    public static <T> Consumer<T> printWith(String prefix) {
        return t -> log.debug("{}{}", prefix, t);
    }

    // --- SUPPLIERS ---

    /** Supplier: null */
    public static <T> Supplier<T> nullSupplier() {
        return () -> null;
    }

    /** Supplier: empty string */
    public static Supplier<String> emptyString() {
        return () -> "";
    }

    /** Supplier: constant value */
    public static <T> Supplier<T> constant(T value) {
        return () -> value;
    }

    // --- COMPARATORS ---

    /** Comparator: natural order (null-safe, nulls first) */
    public static <T extends Comparable<T>> Comparator<T> nullsFirst() {
        return Comparator.nullsFirst(Comparator.naturalOrder());
    }

    /** Comparator: natural order (null-safe, nulls last) */
    public static <T extends Comparable<T>> Comparator<T> nullsLast() {
        return Comparator.nullsLast(Comparator.naturalOrder());
    }

    /** Comparator: by string length */
    public static Comparator<String> byLength() {
        return Comparator.comparingInt(s -> s == null ? 0 : s.length());
    }

    /** Comparator: by string length descending */
    public static Comparator<String> byLengthDesc() {
        return byLength().reversed();
    }

    /** Comparator: case-insensitive */
    public static Comparator<String> caseInsensitive() {
        return String.CASE_INSENSITIVE_ORDER;
    }

    // --- BINARY OPERATORS ---

    /** BinaryOperator: first value */
    public static <T> BinaryOperator<T> first() {
        return (a, b) -> a;
    }

    /** BinaryOperator: last value */
    public static <T> BinaryOperator<T> last() {
        return (a, b) -> b;
    }

    /** BinaryOperator: non-null first */
    public static <T> BinaryOperator<T> nonNullFirst() {
        return (a, b) -> a != null ? a : b;
    }

    /** BinaryOperator: concat strings */
    public static BinaryOperator<String> concat() {
        return String::concat;
    }

    /** BinaryOperator: concat with separator */
    public static BinaryOperator<String> concatWith(String sep) {
        return (a, b) -> a + sep + b;
    }

    // --- BI-PREDICATES ---

    /** BiPredicate: objects equal */
    public static <T> BiPredicate<T, T> equals() {
        return Objects::equals;
    }

    /** BiPredicate: objects not equal */
    public static <T> BiPredicate<T, T> notEquals() {
        return (a, b) -> !Objects.equals(a, b);
    }

    /** BiPredicate: string contains ignore case */
    public static BiPredicate<String, String> containsIgnoreCase() {
        return (s, sub) -> s != null
                && sub != null
                && s.toLowerCase(java.util.Locale.ROOT).contains(sub.toLowerCase(java.util.Locale.ROOT));
    }
}
