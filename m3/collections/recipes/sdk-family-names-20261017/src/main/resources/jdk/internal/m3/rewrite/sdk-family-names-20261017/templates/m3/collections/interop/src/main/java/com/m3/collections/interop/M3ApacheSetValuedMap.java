/* SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 Hitesh Soliwal and contributors.
 * Generated from the SDK family recipe; Apache implementations are dependencies, not copied source.
 */
package com.m3.collections.interop;

import com.m3.collections.M3LinkedHashMap;
import com.m3.collections.M3LinkedHashSet;
import java.util.Set;
import org.apache.commons.collections4.multimap.AbstractSetValuedMap;

/**
 * Apache Commons Collections SetValuedMap semantics over existing M3 storage.
 * The donor owns live views, key removal and pair-count size. M3 owns the key
 * directory and per-key values. This is not Apache's concrete HashMap/ArrayList
 * implementation, is not thread-safe, and promises no serialized donor identity.
 * Allocating-return operations keep the donor's documented behavior.
 */
public final class M3ApacheSetValuedMap<K, V> extends AbstractSetValuedMap<K, V> {
    public M3ApacheSetValuedMap() {
        super(new M3LinkedHashMap<K, Set<V>>());
    }

    @Override
    protected Set<V> createCollection() {
        return new M3LinkedHashSet<>();
    }
}
