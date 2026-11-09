/*
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 */
package jdk.m3.tooling.image;

import java.util.SortedSet;
import java.util.TreeSet;

/** Immutable class-file facts. */
public record M3ClassInfo(
        String binaryName,
        String inputId,
        String entryName,
        String sha256,
        SortedSet<String> referencedClasses,
        SortedSet<M3FeatureKind> features,
        SortedSet<String> nativeMethods) {

    /** Defensively normalizes ordered collections. */
    public M3ClassInfo {
        binaryName = requireText(binaryName, "binaryName");
        inputId = requireText(inputId, "inputId");
        entryName = requireText(entryName, "entryName");
        sha256 = M3Hashing.requireSha256(sha256, "sha256");
        referencedClasses = java.util.Collections.unmodifiableSortedSet(new TreeSet<>(referencedClasses));
        features = java.util.Collections.unmodifiableSortedSet(new TreeSet<>(features));
        nativeMethods = java.util.Collections.unmodifiableSortedSet(new TreeSet<>(nativeMethods));
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
