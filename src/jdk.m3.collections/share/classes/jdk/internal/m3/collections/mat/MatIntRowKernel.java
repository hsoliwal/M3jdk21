/* *****************************************************************************
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
package jdk.internal.m3.collections.mat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Pure integer-row hashing and exact range comparison, with opt-in JNI parity.
 *
 * <p>The Java implementation is the semantic oracle and always remains usable.
 * Native execution is permitted only after an explicit SHA-256-pinned library
 * load. JNI accesses Java arrays only during each synchronous call and retains
 * no pointer, global reference, or persistent backing-store ownership.</p>
 *
 * <p>Lineage: absorbed from Synexia {@code m3-mat-value/src/main/java/jdk/internal/m3/matvalue/MatIntRowKernel.java}
 * blob {@code 25a432eb3b045c57547ad4eb80e53df8fe624bfc} at com.synexia develop {@code 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source modulo names).
 */
public final class MatIntRowKernel {
    private static final long FNV_OFFSET = 0xcbf29ce484222325L;
    private static final long FNV_PRIME = 0x100000001b3L;
    private static volatile boolean nativeLoaded;
    private static Path libraryPath;
    private static String librarySha;

    private MatIntRowKernel() { }

    /** Original MatInternedRows hash: length then four little-endian bytes per signed int. */
    public static long hash(int[] values) {
        Objects.requireNonNull(values, "values");
        // Native FNV was slower in the bounded CPU benchmark; preserve Java by default.
        return hashJava(values);
    }

    /** Checked exact slice equality, not hash equality. */
    public static boolean equal(int[] left, int leftOffset,
            int[] right, int rightOffset, int length) {
        check(left, leftOffset, right, rightOffset, length);
        if (length == 0 || left == right && leftOffset == rightOffset) return true;
        // Exact Java behavior is the default even when JNI has been loaded.
        return equalJava(left, leftOffset, right, rightOffset, length);
    }

    /** Always invokes native for parity testing; fails if the library is not loaded. */
    public static long hashNativeRequired(int[] values) {
        Objects.requireNonNull(values, "values");
        if (!nativeLoaded) throw new IllegalStateException("MAT row native library not loaded");
        return hashNative0(values);
    }

    /** Always invokes native for parity testing; fails if the library is not loaded. */
    public static boolean equalNativeRequired(int[] left, int leftOffset,
            int[] right, int rightOffset, int length) {
        check(left, leftOffset, right, rightOffset, length);
        if (!nativeLoaded) throw new IllegalStateException("MAT row native library not loaded");
        return equalNative0(left, leftOffset, right, rightOffset, length);
    }

    /**
     * Load exactly one explicitly selected native implementation from a
     * non-symlink regular file whose bytes match the supplied SHA-256.
     * No automatic library discovery or search-path fallback is allowed.
     */
    public static synchronized void loadNative(Path library, String expectedSha256)
            throws IOException {
        Objects.requireNonNull(library, "library");
        Objects.requireNonNull(expectedSha256, "expectedSha256");
        if (!expectedSha256.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("expected SHA-256 must contain 64 lowercase hex digits");
        }
        if (!library.isAbsolute() || Files.isSymbolicLink(library)
                || !Files.isRegularFile(library, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("native library must be an absolute regular non-symlink file");
        }
        Path checked = library.toRealPath(LinkOption.NOFOLLOW_LINKS);
        String actual = digest(checked);
        if (!actual.equals(expectedSha256)) {
            throw new SecurityException("MAT row native SHA-256 mismatch");
        }
        if (nativeLoaded) {
            if (!libraryPath.equals(checked) || !librarySha.equals(actual)) {
                throw new IllegalStateException("MAT native library already pinned to another binary");
            }
            return;
        }
        System.load(checked.toString());
        libraryPath = checked;
        librarySha = actual;
        nativeLoaded = true;
    }

    public static boolean nativeLoaded() { return nativeLoaded; }

    static long hashJava(int[] values) {
        long result = (FNV_OFFSET ^ values.length) * FNV_PRIME;
        for (int value : values) {
            for (int shift = 0; shift < Integer.SIZE; shift += Byte.SIZE) {
                result = (result ^ ((value >>> shift) & 0xffL)) * FNV_PRIME;
            }
        }
        return result;
    }

    private static boolean equalJava(int[] left, int leftOffset,
            int[] right, int rightOffset, int length) {
        for (int i = 0; i < length; i++) {
            if (left[leftOffset + i] != right[rightOffset + i]) return false;
        }
        return true;
    }

    private static void check(int[] left, int leftOffset,
            int[] right, int rightOffset, int length) {
        Objects.requireNonNull(left, "left");
        Objects.requireNonNull(right, "right");
        Objects.checkFromIndexSize(leftOffset, length, left.length);
        Objects.checkFromIndexSize(rightOffset, length, right.length);
    }

    private static String digest(Path file) throws IOException {
        MessageDigest digest;
        try { digest = MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException failure) { throw new IllegalStateException(failure); }
        try (InputStream input = Files.newInputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) digest.update(buffer, 0, read);
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static native long hashNative0(int[] values);
    private static native boolean equalNative0(int[] left, int leftOffset,
            int[] right, int rightOffset, int length);
}
