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
package jdk.internal.m3.paradigm.contract;

import jdk.internal.m3.collections.PlaceholderLogger;

import java.util.Optional;
/**
 * ParsingUtils - Common parsing and string extraction utilities.
 *
 * <h2>Purpose</h2>
 * <p>
 * Centralizes string parsing and extraction logic used throughout the
 * m3-iop module, particularly for course codes and specification data.
 * </p>
 *
 * <h2>Patterns Consolidated</h2>
 * <ul>
 * <li>Course code parsing: {@code split(".")[0]}</li>
 * <li>Safe string splitting with fallback</li>
 * <li>PatternKind matching and extraction</li>
 * </ul>
 *
 * @since IOP 1.0 - Parsing Utilities
 * @m3.pattern synexia:bop
 * @m3.algorithm Combines regex splitting, delimiter searches and JDK numeric parsers with documented fallback or Optional results.
 * @m3.dataStructures String inputs, temporary String arrays, primitive scalars and Optional wrappers.
 * @m3.description Reusable String extraction and scalar parsing operations.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-iop/src/main/java/jdk/internal/m3/iop/utils/ParsingUtils.java}
 * blob {@code 48220cefdb8e152afb794e665a73eac0a7fe3029} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm IOP batch (jdk.m3.paradigm, contract) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class ParsingUtils {
    private static final PlaceholderLogger log = PlaceholderLogger.of(ParsingUtils.class);


    /**
     * @m3.pattern synexia:bop
     * @m3.algorithm Executes an empty private constructor body without initializing utility state.
     * @m3.dataStructures One transient utility instance if reflectively invoked; no declared instance fields.
     * @m3.description Private constructor restricts the static utility owner to its own implementation.
     */
    private ParsingUtils() {
        // Utility class - no instantiation
    }

    // --- COURSE CODE PARSING ---

    /**
     * Parses a course number from a course code string.
     *
     * <p>
     * Replaces pattern: {@code courseCode.split("\\.")[0]}
     * </p>
     *
     * <p>
     * Examples:
     * <ul>
     * <li>"18.01" → 18</li>
     * <li>"8" → 8</li>
     * <li>"MIT-18" → throws NumberFormatException</li>
     * </ul>
     *
     * @param courseCode
     *            the course code string (e.g., "18.01")
     * @return the parsed course number
     * @throws NumberFormatException
     *             if the course code format is invalid
     * @throws NullPointerException
     *             if courseCode is null
     * @m3.pattern synexia:bop
     * @m3.algorithm Rejects null, splits on a literal dot regex, rejects zero parts and parses the first part with Integer.parseInt.
     * @m3.dataStructures One String input, a temporary String array and an int result.
     * @m3.description Course-prefix integer parser.
     */
    public static int parseCourseNumber(final String courseCode) {
        if (courseCode == null) {
            throw new NullPointerException("courseCode cannot be null");
        }
        final String[] parts = courseCode.split("\\.");
        if (parts.length == 0) {
            throw new NumberFormatException("Invalid course code format: " + courseCode);
        }
        return Integer.parseInt(parts[0]);
    }

    /**
     * Safely parses a course number with optional fallback.
     *
     * @param courseCode
     *            the course code string
     * @return an Optional containing the parsed number, or empty if parsing fails
     * @m3.pattern synexia:bop
     * @m3.algorithm Wraps parseCourseNumber in Optional and maps NumberFormatException or NullPointerException to empty.
     * @m3.dataStructures String input, boxed Integer and Optional result.
     * @m3.description Failure-to-empty adapter for the course-prefix parser.
     */
    public static Optional<Integer> parseCourseNumberSafe(final String courseCode) {
        try {
            return Optional.of(parseCourseNumber(courseCode));
        } catch (final NumberFormatException | NullPointerException e) {
            return Optional.empty();
        }
    }

    // --- GENERIC PARSING ---

    /**
     * Splits a string by a delimiter and returns the first part.
     *
     * @param str
     *            the string to split
     * @param delimiter
     *            the delimiter regex
     * @param defaultValue
     *            the default value if split fails
     * @return the first part or default value
     * @m3.pattern synexia:bop
     * @m3.algorithm Returns the fallback for null/empty input or zero split parts; otherwise returns parts[0]; invalid delimiters still propagate.
     * @m3.dataStructures String inputs, a temporary String array and one selected String reference.
     * @m3.description First regex-delimited segment operation with a caller fallback.
     */
    public static String getFirstPart(final String str, final String delimiter, final String defaultValue) {
        if (str == null || str.isEmpty()) {
            return defaultValue;
        }
        final String[] parts = str.split(delimiter);
        return (parts.length > 0) ? parts[0] : defaultValue;
    }

    /**
     * Splits a string by a delimiter and returns a specific part.
     *
     * @param str
     *            the string to split
     * @param delimiter
     *            the delimiter regex
     * @param index
     *            the part index (0-based)
     * @return an Optional containing the part, or empty if index is out of bounds
     * @m3.pattern synexia:bop
     * @m3.algorithm Returns empty for null/empty input, splits with String.split, and wraps an in-range segment; invalid delimiters still propagate.
     * @m3.dataStructures String inputs, an int index, a temporary String array and Optional result.
     * @m3.description Bounds-checked regex-delimited segment operation.
     */
    public static Optional<String> getPart(final String str, final String delimiter, final int index) {
        if (str == null || str.isEmpty()) {
            return Optional.empty();
        }
        final String[] parts = str.split(delimiter);
        if (index >= 0 && index < parts.length) {
            return Optional.of(parts[index]);
        }
        return Optional.empty();
    }

    /**
     * Extracts text between two delimiters.
     *
     * @param str
     *            the string to search
     * @param startDelimiter
     *            the start delimiter
     * @param endDelimiter
     *            the end delimiter
     * @return an Optional containing the extracted text, or empty if delimiters not
     *         found
     * @m3.pattern synexia:bop
     * @m3.algorithm Finds the first start delimiter, searches the end delimiter after its full length, then returns the intervening substring.
     * @m3.dataStructures String inputs, int offsets, an extracted String and Optional result.
     * @m3.description First matching delimiter-pair extraction operation.
     */
    public static Optional<String> extractBetween(
            final String str, final String startDelimiter, final String endDelimiter) {
        if (str == null || str.isEmpty()) {
            return Optional.empty();
        }

        final int startIdx = str.indexOf(startDelimiter);
        if (startIdx == -1) {
            return Optional.empty();
        }

        final int endIdx = str.indexOf(endDelimiter, startIdx + startDelimiter.length());
        if (endIdx == -1) {
            return Optional.empty();
        }

        final String extracted = str.substring(startIdx + startDelimiter.length(), endIdx);
        return Optional.of(extracted);
    }

    // --- INTEGER PARSING ---

    /**
     * Safely parses an integer with a default value.
     *
     * @param str
     *            the string to parse
     * @param defaultValue
     *            the default value if parsing fails
     * @return the parsed integer or default value
     * @m3.pattern synexia:bop
     * @m3.algorithm Delegates to Integer.parseInt and returns the default on NumberFormatException or NullPointerException.
     * @m3.dataStructures String input, int fallback and int result.
     * @m3.description Integer parser with a caller fallback.
     */
    public static int parseInt(final String str, final int defaultValue) {
        try {
            return Integer.parseInt(str);
        } catch (final NumberFormatException | NullPointerException e) {
            return defaultValue;
        }
    }

    /**
     * Safely parses an integer with Optional.
     *
     * @param str
     *            the string to parse
     * @return an Optional containing the parsed integer, or empty if parsing fails
     * @m3.pattern synexia:bop
     * @m3.algorithm Wraps Integer.parseInt in Optional and maps NumberFormatException or NullPointerException to empty.
     * @m3.dataStructures String input, boxed Integer and Optional result.
     * @m3.description Failure-to-empty integer parser.
     */
    public static Optional<Integer> parseIntSafe(final String str) {
        try {
            return Optional.of(Integer.parseInt(str));
        } catch (final NumberFormatException | NullPointerException e) {
            return Optional.empty();
        }
    }

    // --- DOUBLE PARSING ---

    /**
     * Safely parses a double with a default value.
     *
     * @param str
     *            the string to parse
     * @param defaultValue
     *            the default value if parsing fails
     * @return the parsed double or default value
     * @m3.pattern synexia:bop
     * @m3.algorithm Delegates to Double.parseDouble and returns the default on NumberFormatException or NullPointerException; NaN and infinity remain valid results.
     * @m3.dataStructures String input, double fallback and double result.
     * @m3.description Double parser with a caller fallback.
     */
    public static double parseDouble(final String str, final double defaultValue) {
        try {
            return Double.parseDouble(str);
        } catch (final NumberFormatException | NullPointerException e) {
            return defaultValue;
        }
    }

    /**
     * Safely parses a double with Optional.
     *
     * @param str
     *            the string to parse
     * @return an Optional containing the parsed double, or empty if parsing fails
     * @m3.pattern synexia:bop
     * @m3.algorithm Wraps Double.parseDouble in Optional and maps NumberFormatException or NullPointerException to empty; NaN and infinity remain present.
     * @m3.dataStructures String input, boxed Double and Optional result.
     * @m3.description Failure-to-empty double parser.
     */
    public static Optional<Double> parseDoubleSafe(final String str) {
        try {
            return Optional.of(Double.parseDouble(str));
        } catch (final NumberFormatException | NullPointerException e) {
            return Optional.empty();
        }
    }

    // --- BOOLEAN PARSING ---

    /**
     * Parses a string to boolean with common patterns.
     *
     * <p>
     * Recognizes: "true", "yes", "1", "on" (case-insensitive) as true. All other
     * values are false.
     * </p>
     *
     * @param str
     *            the string to parse
     * @return true if string matches a truthy pattern
     * @m3.pattern synexia:bop
     * @m3.algorithm Rejects null/empty input, then matches true, yes and on ignoring case or exactly 1; surrounding whitespace is not stripped.
     * @m3.dataStructures One String reference, four literal String tokens and a boolean result.
     * @m3.description Fixed truthy-token recognition operation.
     */
    public static boolean parseBoolean(final String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        return str.equalsIgnoreCase("true")
                || str.equalsIgnoreCase("yes")
                || str.equals("1")
                || str.equalsIgnoreCase("on");
    }
}
