/*
 * Copyright (c) 2026, Hitesh Soliwal. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * GPLv2 with the Classpath exception.
 */
package jdk.internal.access;

/**
 * M3JDK-private access to fixed-size precomputed facts of canonical String storage.
 *
 * <p>This is not a java.lang.String API. Consumers such as the M3 AST/compiler may reuse the
 * String owner's already-computed lexical geometry without retaining text copies or rescanning an
 * immutable source range. A null result means the String is not currently backed by M3 storage;
 * callers must preserve their ordinary String fallback.</p>
 */
public interface M3StringAccess {
    /**
     * Snapshot of one exact visible String range.
     *
     * <p>All fields are immutable scalars copied from M3StringFacts. Signals are candidate filters;
     * exact String operations remain semantic authority.</p>
     */
    record Facts(
            int utf16Length,
            int utf8Length,
            int modifiedUtf8Length,
            int codePointCount,
            int unpairedSurrogateCount,
            int javaHash,
            int hash31Power,
            long bitSignal64,
            boolean ascii,
            boolean latin1,
            int asciiUpperHash,
            int asciiLowerHash,
            int asciiTitleHash,
            long prefix4,
            long suffix4,
            long bigramSignal64,
            long trigramSignal64,
            int trimStart,
            int trimEnd,
            int stripStart,
            int stripEnd,
            long ownerStructuralHash64,
            long coordinate) { }

    /** True only when the supplied String already owns canonical M3 storage. */
    boolean hasM3Storage(String value);

    /**
     * Returns facts for the full String, or null when no M3 storage is attached.
     * This may prepare the canonical owner's fixed-size facts once.
     */
    Facts facts(String value);

    /**
     * Returns facts for the exact half-open UTF-16 range, or null when no M3 storage is attached.
     * Bounds are checked exactly like String range operations.
     */
    Facts facts(String value, int beginIndex, int endIndex);
}
