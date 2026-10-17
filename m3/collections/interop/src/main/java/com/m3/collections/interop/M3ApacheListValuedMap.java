/* SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 Hitesh Soliwal and contributors.
 * Generated from the SDK family recipe; Apache implementations are dependencies, not copied source.
 */
package com.m3.collections.interop;

import com.m3.collections.M3LinkedHashMap;
import com.m3.collections.M3LinkedList;
import java.util.List;
import org.apache.commons.collections4.multimap.AbstractListValuedMap;

/**
 * Apache Commons Collections ListValuedMap semantics over existing M3 storage.
 * The donor owns live views, key removal and pair-count size. M3 owns the key
 * directory and per-key values. This is not Apache's concrete HashMap/ArrayList
 * implementation, is not thread-safe, and promises no serialized donor identity.
 * Allocating-return operations keep the donor's documented behavior.
 */
public final class M3ApacheListValuedMap<K, V> extends AbstractListValuedMap<K, V> {
    public M3ApacheListValuedMap() {
        super(new M3LinkedHashMap<K, List<V>>());
    }

    @Override
    protected List<V> createCollection() {
        return new M3LinkedList<>();
    }
}
