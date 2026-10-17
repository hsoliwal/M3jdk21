/*
 * Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package com.m3.collections.interop;

import com.m3.collections.M3LinkedHashMap;
import java.util.Map;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.AbstractDualBidiMap;

/**
 * Apache Commons Collections' actual BidiMap contract backed by two M3 indexed maps.
 *
 * <p>The upstream AbstractDualBidiMap owns the unique-value rule, inverse identity,
 * bidirectional mutation and MapIterator/view contracts. Only its two underlying
 * Map stores are replaced by existing M3LinkedHashMap owners. This does not
 * claim concrete Apache DualHashBidiMap class identity or its serialized form.
 * Per-key/value index links and reference retention follow the existing M3
 * map implementation; no Entry object is held for each mapping.
 *
 * <p>No JNI crossing is required for Java reference equality/hash or user
 * callbacks. Existing primitive mask/sort JNI operators remain independently
 * available to M3 owners and are deliberately not duplicated here.
 */
public final class M3ApacheBidiMap<K, V> extends AbstractDualBidiMap<K, V> {
    public M3ApacheBidiMap() {
        super(new M3LinkedHashMap<>(), new M3LinkedHashMap<>());
    }

    private M3ApacheBidiMap(Map<K, V> forward, Map<V, K> reverse, BidiMap<V, K> inverse) {
        super(forward, reverse, inverse);
    }

    @Override
    protected BidiMap<V, K> createBidiMap(Map<V, K> forward, Map<K, V> reverse,
                                          BidiMap<K, V> inverse) {
        return new M3ApacheBidiMap<>(forward, reverse, inverse);
    }
}
