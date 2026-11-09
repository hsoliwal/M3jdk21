/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.tooling.image;

/** Executable image backend. */
public enum M3BackendKind {
    /** Standard Java 21 backend using jlink and AppCDS. */
    JDK21_SHIFTED,
    /** Optional external GraalVM Native Image process adapter. */
    NATIVE_IMAGE;

    /** Parses a command-line spelling. */
    public static M3BackendKind parse(String value) {
        return switch (value.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "jdk21-shifted", "jdk21_shifted", "jdk21", "shifted" -> JDK21_SHIFTED;
            case "native-image", "native_image", "native" -> NATIVE_IMAGE;
            default -> throw new IllegalArgumentException("Unknown backend: " + value);
        };
    }
}
