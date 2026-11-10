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
package jdk.internal.m3.paradigm.annotation;

import java.util.Collection;
import jdk.internal.m3.collections.PlaceholderLogger;

import java.util.Objects;
/**
 * ValidationUtils - Common validation and null-check utilities.
 *
 * <p>Centralized validation helpers to reduce duplication of
 * {@code Objects.requireNonNull()} checks across the module.</p>
 *
 * @since m3-anop 1.0
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/utils/ValidationUtils.java}
 * blob {@code 3462d7a91407079f1ce9d2154845dae79320d933} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class ValidationUtils {
    private static final PlaceholderLogger log = PlaceholderLogger.of(ValidationUtils.class);


    private ValidationUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /** Require non-null value with standard message format. */
    public static <T> T requireNonNull(T value, String name) {
        return Objects.requireNonNull(value, name + " cannot be null");
    }

    /** Require non-null value with custom message. */
    public static <T> T requireNonNull(T value, String name, String message) {
        return Objects.requireNonNull(value, name + ": " + message);
    }

    /** Require multiple values to be non-null. */
    public static void requireNonNulls(Object... values) {
        for (int i = 0; i < values.length; i++) {
            Objects.requireNonNull(values[i], "Parameter at index " + i + " cannot be null");
        }
    }

    /** Check if string is not null and not empty. */
    public static boolean isNotEmpty(String value) {
        return value != null && !value.isEmpty();
    }

    /** Check if string is null or empty. */
    public static boolean isEmpty(String value) {
        return value == null || value.isEmpty();
    }

    /** Get value or default if null. */
    public static <T> T getOrDefault(T value, T defaultValue) {
        return value != null ? value : defaultValue;
    }

    // MEMBER_UNION (user decision 2026-11-10, "Union into AnOP owners"): members of the IOP twin
    /**
     * Checks if a string is blank (null, empty, or whitespace-only).
     *
     * @param str
     *            the string to check
     * @return true if the string is blank
     * @m3.pattern synexia:bop
     * @m3.algorithm Short-circuits null as blank, otherwise delegates to String.isBlank.
     * @m3.dataStructures One String reference and a boolean result.
     * @m3.description Null-or-blank String predicate.
     */
    public static boolean isBlank(final String str) {
        return str == null || str.isBlank();
    }

    /**
     * Checks if a collection is empty or null.
     *
     * @param collection
     *            the collection to check
     * @return true if collection is null or empty
     * @m3.pattern synexia:bop
     * @m3.algorithm Short-circuits null as empty, otherwise invokes the supplied Collection.isEmpty implementation.
     * @m3.dataStructures One caller-owned Collection reference and a boolean result.
     * @m3.description Null-or-empty Collection predicate.
     */
    public static boolean isEmpty(final Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    /**
     * Checks if a string is not blank.
     *
     * @param str
     *            the string to check
     * @return true if the string is not blank
     * @m3.pattern synexia:bop
     * @m3.algorithm Negates the existing isBlank result.
     * @m3.dataStructures One String reference and a boolean result.
     * @m3.description Negated null-or-blank String predicate.
     */
    public static boolean isNotBlank(final String str) {
        return !isBlank(str);
    }

    /**
     * Checks if a collection is not empty.
     *
     * @param collection
     *            the collection to check
     * @return true if collection is not null and not empty
     * @m3.pattern synexia:bop
     * @m3.algorithm Negates the existing isEmpty result.
     * @m3.dataStructures One caller-owned Collection reference and a boolean result.
     * @m3.description Negated null-or-empty Collection predicate.
     */
    public static boolean isNotEmpty(final Collection<?> collection) {
        return !isEmpty(collection);
    }

    /**
     * Checks if a string is valid (not null and not empty).
     *
     * <p>
     * Replaces pattern: {@code str != null && !str.isEmpty()}
     * </p>
     *
     * @param str
     *            the string to validate
     * @return true if the string is not null and not empty
     * @m3.pattern synexia:bop
     * @m3.algorithm Short-circuits null before String.isEmpty; whitespace-only strings remain valid.
     * @m3.dataStructures One String reference and a boolean result.
     * @m3.description String-presence predicate.
     */
    public static boolean isValidString(final String str) {
        return str != null && !str.isEmpty();
    }

    /**
     * Validates that multiple objects are not null.
     *
     * @param objects
     *            the objects to validate
     * @throws NullPointerException
     *             if any object is null
     * @m3.pattern synexia:bop
     * @m3.algorithm Traverses indices with IntStream.range and throws from Objects.requireNonNull at the first null element; a null array fails before traversal.
     * @m3.dataStructures Caller-supplied Object array, int stream indices and String messages.
     * @m3.description Ordered varargs object-presence validation operation.
     */
    public static void requireAllNonNull(final Object... objects) {
        java.util.stream.IntStream.range(0, objects.length)
                .forEach(i -> Objects.requireNonNull(objects[i], "Argument at index " + i + " cannot be null"));
    }

    /**
     * Validates that a condition is true.
     *
     * @param condition
     *            the condition to check
     * @param message
     *            the error message if condition is false
     * @throws IllegalArgumentException
     *             if condition is false
     * @m3.pattern synexia:bop
     * @m3.algorithm Throws IllegalArgumentException with the supplied message only when the condition is false.
     * @m3.dataStructures A boolean parameter, a String message and a possible exception object.
     * @m3.description Boolean argument-condition validation operation.
     */
    public static void validateCondition(final boolean condition, final String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Validates that a value is within a range.
     *
     * @param value
     *            the value to validate
     * @param min
     *            the minimum value (inclusive)
     * @param max
     *            the maximum value (inclusive)
     * @param paramName
     *            the parameter name for error message
     * @throws IllegalArgumentException
     *             if value is outside the range
     * @m3.pattern synexia:bop
     * @m3.algorithm Throws when value is below min or above max and composes the parameter and bound values into the message.
     * @m3.dataStructures Three int values, a String parameter name and a possible exception object.
     * @m3.description Inclusive integer-range validation operation.
     */
    public static void validateRange(final int value, final int min, final int max, final String paramName) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(
                    paramName + " must be between " + min + " and " + max + ", but got " + value);
        }
    }

    /**
     * Validates that a value is within a range.
     *
     * @param value
     *            the value to validate
     * @param min
     *            the minimum value (inclusive)
     * @param max
     *            the maximum value (inclusive)
     * @param paramName
     *            the parameter name for error message
     * @throws IllegalArgumentException
     *             if value is outside the range
     * @m3.pattern synexia:bop
     * @m3.algorithm Throws when value is below min or above max; ordinary double comparisons permit NaN and do not normalize reversed bounds.
     * @m3.dataStructures Three double values, a String parameter name and a possible exception object.
     * @m3.description Comparison-based inclusive double-range validation operation.
     */
    public static void validateRange(final double value, final double min, final double max, final String paramName) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(
                    paramName + " must be between " + min + " and " + max + ", but got " + value);
        }
    }

    /**
     * Validates that a string matches a condition.
     *
     * @param str
     *            the string to validate
     * @param condition
     *            the condition to check
     * @param paramName
     *            the parameter name for error message
     * @throws IllegalArgumentException
     *             if condition is false
     * @m3.pattern synexia:bop
     * @m3.algorithm Calls condition.test(str) once and throws IllegalArgumentException for false; null predicates and predicate exceptions propagate.
     * @m3.dataStructures String input/name, a caller-owned Predicate and a possible exception object.
     * @m3.description Caller Predicate-based String validation operation.
     */
    public static void validateString(
            final String str, final java.util.function.Predicate<String> condition, final String paramName) {
        if (!condition.test(str)) {
            throw new IllegalArgumentException(paramName + " validation failed: " + str);
        }
    }
}
