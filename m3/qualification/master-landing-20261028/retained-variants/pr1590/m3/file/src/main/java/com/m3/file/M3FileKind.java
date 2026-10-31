// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package com.m3.file;

import java.nio.file.attribute.BasicFileAttributes;

/**
 * Entry kind as a mask bit. Links are never followed, so a link is {@link #SYMLINK} whatever it
 * points to; {@link #OTHER} covers devices, pipes and entries whose attributes could not be read.
 */
public enum M3FileKind {
    FILE, DIRECTORY, SYMLINK, OTHER;

    /** Mask of every kind. */
    public static final int ALL = 0b1111;

    static final M3FileKind[] VALUES = values();

    public int bit() {
        return 1 << ordinal();
    }

    /** Mask of {@code kinds}; empty means none. */
    public static int mask(M3FileKind... kinds) {
        int mask = 0;
        for (M3FileKind kind : kinds) mask |= kind.bit();
        return mask;
    }

    /** Classifies attributes read with {@code NOFOLLOW_LINKS}. */
    public static M3FileKind of(BasicFileAttributes attrs) {
        if (attrs.isSymbolicLink()) return SYMLINK;
        if (attrs.isDirectory()) return DIRECTORY;
        if (attrs.isRegularFile()) return FILE;
        return OTHER;
    }
}
