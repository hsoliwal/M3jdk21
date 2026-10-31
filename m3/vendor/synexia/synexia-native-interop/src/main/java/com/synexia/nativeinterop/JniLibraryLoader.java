// SPDX-License-Identifier: Apache-2.0

package com.synexia.nativeinterop;

import java.util.HashSet;
import java.util.Set;

/** Synchronized, idempotent JNI library loader shared by all Synexia modules. */
public final class JniLibraryLoader {
    private static final Object LOCK = new Object();
    private static final Set<String> LOADED = new HashSet<>();

    private JniLibraryLoader() {}

    public static void load(NativeLibrarySpec library) {
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

    public static boolean isLoaded(NativeLibrarySpec library) {
        synchronized (LOCK) {
            return LOADED.contains(library.cacheKey());
        }
    }
}
