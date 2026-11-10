/* ******************************************************************************
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
package jdk.internal.m3.paradigm.context;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Process-level immutable canonicalization plus explicit identity-scope factories.
 *
 * <p>Lineage: absorbed from Synexia {@code m3-cop/src/main/java/jdk/internal/m3/context/core/ContextPools.java}
 * blob {@code fea1827be78172a6f515573ac6535f975c1ae21b} at com.synexia develop {@code 8ab80ea57d9f} by
 * RELOCATE_M3_OWNER v4 (paradigm COP batch (jdk.m3.paradigm, context) v6.3: identifiers and package only; bytecode equal to the source modulo names).
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public final class Pools {
    private static final ContextImmutablePool GLOBAL = new ContextImmutablePool(262_144);

    private Pools() { }

    /** Process-lifetime strong identity domain. Entries are never evicted or cleared. */
    public static ContextImmutablePool global() { return GLOBAL; }

    /** Explicit non-evicting domain with a caller-owned lifetime. */
    public static ContextImmutablePool identityScope(int capacity) {
        return new ContextImmutablePool(capacity);
    }

    public static <T> T intern(T value) { return (T) GLOBAL.intern(value); }
    public static <E> List<E> internList(Iterable<? extends E> values) { return GLOBAL.internList(values); }
    public static <E> Set<E> internSet(Iterable<? extends E> values) { return GLOBAL.internSet(values); }
    public static <K,V> Map<K,V> internMap(Map<? extends K,? extends V> values) { return GLOBAL.internMap(values); }

    /**
     * Framework-internal best effort: immutable values are canonicalized, mutable/unknown values
     * remain untouched. Capacity exhaustion never breaks an unrelated context construction.
     */
    @SuppressWarnings("unchecked")
    public static <T> T canonicalizeIfImmutable(T value) {
        if (value == null || !ContextImmutablePool.isKnownImmutable(value)) return value;
        try { return (T) GLOBAL.intern(value); }
        catch (IllegalStateException capacity) { return value; }
    }
}
