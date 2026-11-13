/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.tooling.image;

/** Compatibility policy applied while constructing an image plan. */
public enum M3Mode {
    /** Preserve every application class as executable bytecode. */
    COMPATIBLE,
    /** Prefer observed and reachable classes while retaining bytecode fallback. */
    HYBRID,
    /** Refuse unresolved dynamism and classes outside the admitted world. */
    CLOSED_WORLD;

    /** Parses a command-line spelling. */
    public static M3Mode parse(String value) {
        return switch (value.trim().toLowerCase(java.util.Locale.ROOT)) {
            case "compatible" -> COMPATIBLE;
            case "hybrid" -> HYBRID;
            case "closed-world", "closed_world", "closedworld" -> CLOSED_WORLD;
            default -> throw new IllegalArgumentException("Unknown mode: " + value);
        };
    }
}
