// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.file;
//FLLDCIM: RESTORE - source m3/qualification/master-landing-20261028/retained-variants/pr1619/m3/file/src/main/java/jdk/m3/file/M3FileKind.java blob 6fb86175cefbd4c18952f95660ccf54b55034ee4 (retained PR #1619 head; parent 0cd0944136c1 blob faf29b02173a25a758c2b53e1f6204fd639e2324 https://github.com/hsoliwal/M3jdk21/blob/0cd0944136c1/m3/file/src/main/java/com/m3/file/M3FileKind.java); recipe m3/file/qualification/tree-lanes-port-20261107 RESTORE_EXACT

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
