// SPDX-License-Identifier: Apache-2.0
package com.synexia.rewrite.sealed;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;

/** Actual loaded class bytes for an explicitly declared implementation closure, never a version label alone. */
public final class CodeIdentity {
    private CodeIdentity() { }
    public static String of(Class<?>... closure) {
        StringBuilder out = new StringBuilder("CLASS-CLOSURE/1\n");
        java.util.TreeSet<Class<?>> types = new java.util.TreeSet<>(Comparator.comparing(Class::getName));
        for (Class<?> type : closure) collect(type, types);
        types.forEach(type -> {
            String resource = "/" + type.getName().replace('.', '/') + ".class";
            try (var input = type.getResourceAsStream(resource)) {
                if (input == null) throw new IllegalStateException("MISSING_CLASS_BYTES:" + type.getName());
                out.append(SealHash.frame(type.getName(), SealHash.bytes(input.readAllBytes()))).append('\n');
            } catch (IOException failure) { throw new IllegalStateException("CLASS_IDENTITY_READ", failure); }
        });
        return SealHash.text(out.toString());
    }
    private static void collect(Class<?> type, java.util.Set<Class<?>> types) {
        if (types.add(type)) for (Class<?> nested : type.getDeclaredClasses()) collect(nested, types);
    }
}
