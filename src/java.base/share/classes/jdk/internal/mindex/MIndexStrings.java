/*
 * Copyright (c) 2026, Contributors. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package jdk.internal.mindex;

import java.nio.file.Path;
import java.util.Objects;

import jdk.internal.access.SharedSecrets;

/**
 * Internal process-wide installation/factory facade for experimental MIndex-backed Strings.
 *
 * <p>One backing namespace is installed for the process. String objects retain logical MIndex IDs,
 * never native/mapped addresses. This first integration keeps each String's ordinary byte[] value
 * as a correctness fallback while selected String operations read the canonical backing.</p>
 */
public final class MIndexStrings {
    private static volatile MIndexStringBacking backing;

    private MIndexStrings() {}

    /** Installs one backing namespace. Reinstalling a different instance is rejected. */
    public static synchronized void install(MIndexStringBacking candidate) {
        Objects.requireNonNull(candidate, "candidate");
        MIndexStringBacking current = backing;
        if (current != null && current != candidate) {
            throw new IllegalStateException("MIndex String backing is already installed");
        }
        backing = candidate;
    }

    /** Opens and installs the mapped MIndex backing used by the Synexia OS store. */
    public static MIndexMappedStringBacking installMapped(Path path) {
        MIndexMappedStringBacking mapped = MIndexMappedStringBacking.open(path);
        boolean installed = false;
        try {
            install(mapped);
            installed = true;
            return mapped;
        } finally {
            if (!installed) {
                mapped.close();
            }
        }
    }

    public static boolean installed() {
        return backing != null;
    }

    /** Returns the process backing or fails closed for an invalid backed String. */
    public static MIndexStringBacking backing() {
        MIndexStringBacking current = backing;
        if (current == null) {
            throw new IllegalStateException("MIndex String backing is not installed");
        }
        return current;
    }

    /** Constructs an ordinary java.lang.String carrying the supplied canonical MIndex ID. */
    public static String fromId(long id) {
        if (id == 0L) {
            throw new IllegalArgumentException("MIndex String ID must be non-zero");
        }
        backing(); // validate before entering java.lang.String
        return SharedSecrets.getJavaLangAccess().newMIndexString(id);
    }

    public static boolean isBacked(String value) {
        return SharedSecrets.getJavaLangAccess()
                .isMIndexString(Objects.requireNonNull(value, "value"));
    }

    public static long id(String value) {
        return SharedSecrets.getJavaLangAccess()
                .mindexStringId(Objects.requireNonNull(value, "value"));
    }
}
