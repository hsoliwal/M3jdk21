// Copyright 2026 Hitesh Soliwal and contributors
// Full donor dependency receiver; source custody in JSON_CLOSURE_PROVENANCE.json.
// SPDX-License-Identifier: Apache-2.0

package com.m3.file;

import java.util.HashSet;
import java.util.Set;

/** Synchronized, idempotent JNI library loader shared by all Synexia modules. */
public final class M3JniLibraryLoader {
    private static final Object LOCK = new Object();
    private static final Set<String> LOADED = new HashSet<>();

    private M3JniLibraryLoader() {}

    public static void load(M3NativeLibrarySpec library) {
        String key = library.cacheKey();
        synchronized (LOCK) {
            if (LOADED.contains(key)) {
                return;
            }
            if (library.explicit()) {
                System.load(library.explicitPath().toString());
            } else {
                System.loadLibrary(library.logicalName());
            }
            LOADED.add(key);
        }
    }

    public static boolean isLoaded(M3NativeLibrarySpec library) {
        synchronized (LOCK) {
            return LOADED.contains(library.cacheKey());
        }
    }
}
