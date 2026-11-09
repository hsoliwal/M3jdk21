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
}
