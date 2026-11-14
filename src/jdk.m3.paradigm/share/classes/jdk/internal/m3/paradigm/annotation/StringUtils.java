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

import java.util.Locale;

/**
 * StringUtils - Common string manipulation utilities.
 *
 * <p>
 * Centralized string helpers for FQCN parsing, normalization, and common string
 * operations across the module.
 *
 * <h2>Usage Examples</h2>
 *
 * <pre>{@code
 *
 * // Extract simple name from FQCN
 * String simple = StringUtils.getSimpleName("com.example.IUserService");
 * // Result: "IUserService"
 *
 * // Extract package from FQCN
 * String pkg = StringUtils.getPackageName("com.example.IUserService");
 * // Result: "com.example"
 *
 * // Strip "I" prefix from interface name
 * String impl = StringUtils.stripInterfacePrefix("IUserService");
 * // Result: "UserService"
 *
 * // Normalize whitespace
 * String norm = StringUtils.normalizeWhitespace("text  with  spaces");
 * // Result: "text with spaces"
 * }</pre>
 *
 * @since m3-anop 1.0
 *
 * <p>Lineage: absorbed from Synexia {@code m3-anop/src/main/java/jdk/internal/m3/anop/utils/StringUtils.java}
 * blob {@code 3a158126f2c0725bca3e4c7f46566e3b520241d1} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm AnOP batch (jdk.m3.paradigm, annotation) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class StringUtils {

    private static final char DOT = '.';
    private static final String INTERFACE_PREFIX = "I";
    private static final String WHITESPACE_PATTERN = "\\s+";
    private static final String SINGLE_SPACE = " ";

    private StringUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Extract simple class name from fully qualified class name.
     *
     * <p>
     * Example: "com.example.IUserService" -> "IUserService"
     *
     * @param fqcn
     *            the fully qualified class name
     * @return simple name
     */
    public static String getSimpleName(String fqcn) {
        if (fqcn == null || fqcn.isEmpty()) {
            return fqcn;
        }
        int lastDot = fqcn.lastIndexOf(DOT);
        if (lastDot < 0) {
            return fqcn;
        }
        return fqcn.substring(lastDot + 1);
    }

    /**
     * Extract package name from fully qualified class name.
     *
     * <p>
     * Example: "com.example.IUserService" -> "com.example"
     *
     * @param fqcn
     *            the fully qualified class name
     * @return package name, or empty string if no package
     */
    public static String getPackageName(String fqcn) {
        if (fqcn == null || fqcn.isEmpty()) {
            return "";
        }
        int lastDot = fqcn.lastIndexOf(DOT);
        if (lastDot < 0) {
            return "";
        }
        return fqcn.substring(0, lastDot);
    }

    /**
     * Strip "I" prefix from interface name if present.
     *
     * <p>
     * Example: "IUserService" -> "UserService"<br/>
     * Example: "UserService" -> "UserService"
     *
     * @param interfaceName
     *            the interface name
     * @return name without "I" prefix, or original if no prefix
     */
    public static String stripInterfacePrefix(String interfaceName) {
        if (interfaceName != null
                && interfaceName.length() > 1
                && interfaceName.charAt(0) == 'I'
                && Character.isUpperCase(interfaceName.charAt(1))) {
            return interfaceName.substring(1);
        }
        return interfaceName;
    }

    /**
     * Compute implementation class name from interface FQCN.
     *
     * <p>
     * Example: "com.example.IUserService" -> "com.example.impl.UserServiceImpl"
     *
     * @param interfaceFqcn
     *            the interface fully qualified class name
     * @return impl FQCN
     */
    public static String computeImplName(String interfaceFqcn) {
        String simpleName = getSimpleName(interfaceFqcn);
        simpleName = stripInterfacePrefix(simpleName);
        String packageName = getPackageName(interfaceFqcn);
        return packageName + ".impl." + simpleName + "Impl";
    }

    /**
     * Normalize whitespace in string (collapse multiple spaces to single space).
     *
     * <p>
     * Example: "text with spaces" -> "text with spaces"
     *
     * @param text
     *            the text to normalize
     * @return normalized text
     */
    public static String normalizeWhitespace(String text) {
        if (text == null) {
            return null;
        }
        return text.replaceAll(WHITESPACE_PATTERN, SINGLE_SPACE).trim();
    }

    /**
     * Normalize whitespace and convert to lowercase.
     *
     * @param text
     *            the text to normalize
     * @return normalized lowercase text
     */
    public static String normalizeAndLowercase(String text) {
        if (text == null) {
            return null;
        }
        return normalizeWhitespace(text).toLowerCase(Locale.ROOT);
    }

    /**
     * Join strings with a separator.
     *
     * @param separator
     *            the separator
     * @param parts
     *            the parts
     * @return joined string
     */
    public static String join(String separator, String... parts) {
        return String.join(separator, parts);
    }

    /**
     * Check if string starts with another string (case-insensitive).
     *
     * @param text
     *            the text to check
     * @param prefix
     *            the prefix to match
     * @return true if text starts with prefix (ignoring case)
     */
    public static boolean startsWithIgnoreCase(String text, String prefix) {
        if (text == null || prefix == null) {
            return false;
        }
        return text.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT));
    }

    /**
     * Extract substring between start and end markers.
     *
     * @param text
     *            the text
     * @param start
     *            the start marker (exclusive)
     * @param end
     *            the end marker (exclusive)
     * @return extracted text, or empty string if markers not found
     */
    public static String extractBetween(String text, String start, String end) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        int startIdx = text.indexOf(start);
        if (startIdx < 0) {
            return "";
        }
        int endIdx = text.indexOf(end, startIdx + start.length());
        if (endIdx < 0) {
            return "";
        }
        return text.substring(startIdx + start.length(), endIdx);
    }
}
