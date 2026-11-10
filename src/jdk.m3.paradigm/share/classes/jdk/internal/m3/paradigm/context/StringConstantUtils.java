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
package jdk.internal.m3.paradigm.context;

import jdk.internal.m3.collections.PlaceholderLogger;

import jdk.internal.m3.collections.TokenInterner;
/**
 * StringConstantUtils - Utility for interned string constants and
 * TokenInterner.
 *
 * <p>
 * Centralizes the creation and management of interned string constants used
 * throughout the synexia-cop module. Provides:
 * <ul>
 * <li>Single TokenInterner instance (singleton pattern)</li>
 * <li>Common string constant definitions (errors, prefixes, suffixes)</li>
 * <li>Optimized string identity comparison (== instead of .equals())</li>
 * </ul>
 *
 * <h2>Design Rationale</h2>
 * <p>
 * Interned strings enable:
 * <ul>
 * <li>Faster comparison: == instead of .equals() via String#intern()</li>
 * <li>Memory savings: identical strings share the same object reference</li>
 * <li>Atomic operations: VarHandle operations benefit from interned keys</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 *
 * <pre>
 *
 * String errorMsg = StringConstantUtils.error("parameter cannot be null");
 * String key = StringConstantUtils.intern("myKey");
 * String prefix = StringConstantUtils.prefix("[", "MyClass");
 * </pre>
 *
 * @since ContextOS 1.0
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/cop/utils/StringConstantUtils.java}
 * blob {@code fcbe8b8702faf8fa3eaca862ce846650673d6adc} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class StringConstantUtils {
    private static final PlaceholderLogger log = PlaceholderLogger.of(StringConstantUtils.class);


    private static final TokenInterner INTERNER = TokenInterner.getInstance();

    // --- Common Error Messages (3+ uses) ---

    private static final String ERR_CANNOT_BE_NULL = INTERNER.intern("cannot be null");
    private static final String ERR_PARAM_NULL = INTERNER.intern("parameter cannot be null");

    // --- Common Brackets and Delimiters (3+ uses) ---

    private static final String BRACKET_OPEN = INTERNER.intern("[");
    private static final String BRACKET_CLOSE = INTERNER.intern("]");
    private static final String COMMA_SPACE = INTERNER.intern(", ");
    private static final String EQUALS = INTERNER.intern("=");
    private static final String SLASH = INTERNER.intern("/");

    private StringConstantUtils() {
        // Utility class - no instantiation
    }

    /**
     * Get the global TokenInterner instance.
     *
     * @return the TokenInterner singleton
     */
    public static TokenInterner interner() {
        return INTERNER;
    }

    /**
     * Intern a string for == comparison.
     *
     * <p>
     * All strings passed through this method are interned and can be safely
     * compared using == operator, which is faster than .equals().
     *
     * @param str
     *            the string to intern
     * @return the interned string (same reference if already interned)
     */
    public static String intern(String str) {
        return INTERNER.intern(str);
    }

    /**
     * Build an error message by appending "cannot be null" suffix.
     *
     * <p>
     * Common pattern for parameter validation messages.
     *
     * @param paramName
     *            the parameter name
     * @return error message like "paramName cannot be null"
     */
    public static String error(String paramName) {
        return INTERNER.intern(paramName + " " + ERR_CANNOT_BE_NULL);
    }

    /**
     * Build a generic error message for parameters.
     *
     * @param message
     *            the error message
     * @return interned error message
     */
    public static String errorMessage(String message) {
        return INTERNER.intern(message);
    }

    /**
     * Build a prefix string for toString() or logging.
     *
     * <p>
     * Pattern: "ClassName[" or "[key="
     *
     * @param prefix
     *            the prefix text
     * @param suffix
     *            optional suffix (may be null)
     * @return "prefix" or "prefix[suffix" depending on context
     */
    public static String prefix(String prefix, String suffix) {
        if (suffix == null || suffix.isEmpty()) {
            return INTERNER.intern(prefix);
        }
        return INTERNER.intern(prefix + suffix + BRACKET_OPEN);
    }

    /**
     * Get opening bracket "[".
     *
     * @return "["
     */
    public static String bracketOpen() {
        return BRACKET_OPEN;
    }

    /**
     * Get closing bracket "]".
     *
     * @return "]"
     */
    public static String bracketClose() {
        return BRACKET_CLOSE;
    }

    /**
     * Get comma with space ", ".
     *
     * @return ", "
     */
    public static String commaSpace() {
        return COMMA_SPACE;
    }

    /**
     * Get equals sign with optional spacing.
     *
     * <p>
     * Pattern: "field=" or "field = "
     *
     * @param spacePadded
     *            if true returns " = ", otherwise "="
     * @return the equals operator
     */
    public static String equals(boolean spacePadded) {
        return spacePadded ? INTERNER.intern(" = ") : EQUALS;
    }

    /**
     * Get field equals pattern "field=".
     *
     * <p>
     * Common for toString() output.
     *
     * @param fieldName
     *            the field name
     * @return "fieldName="
     */
    public static String fieldEquals(String fieldName) {
        return INTERNER.intern(fieldName + EQUALS);
    }

    /**
     * Build a simple toString representation.
     *
     * <p>
     * Pattern: "ClassName[id=value]"
     *
     * @param className
     *            the class name
     * @param fieldName
     *            the field name
     * @param value
     *            the field value
     * @return formatted string
     */
    public static String toStringFormat(String className, String fieldName, Object value) {
        return INTERNER.intern(className + BRACKET_OPEN + fieldName + EQUALS + value + BRACKET_CLOSE);
    }

    /**
     * Get slash "/" for paths or versioning.
     *
     * @return "/"
     */
    public static String slash() {
        return SLASH;
    }
}
