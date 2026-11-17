/* *****************************************************************************
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
package jdk.internal.m3.collections.mat;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable pool-ID -> pool-ID map over two exact MAT pools.
 *
 * <p>Lineage: absorbed from Synexia {@code
 * m3-mat-collections/src/main/java/jdk/internal/m3/matcollections/IdToIdMap.java}
 * blob {@code cf79e16c3a0b98a3c8c21eac85c14f34bdebe418} at com.synexia develop {@code
 * 35474ccf8dafbe80feb55f1d82cb1136eeec119d} by
 * RELOCATE_M3_OWNER v4 (mat batch: identifiers and package only; bytecode equal to the source
 * modulo names).
 */
public final class IdToIdMap<K,V> {
    private final Pool<K> keyPool;
    private final Pool<V> valuePool;
    private final AdaptiveIntToIntMap map;

    private IdToIdMap(Pool<K> keyPool, Pool<V> valuePool, AdaptiveIntToIntMap map) {
        this.keyPool = keyPool; this.valuePool = valuePool; this.map = map;
    }

    public static <K,V> Builder<K,V> builder(Pool<K> keyPool, Pool<V> valuePool) {
        return new Builder<>(keyPool, valuePool);
    }

    public int size() { return map.size(); }
    public boolean containsKey(Ref<K> key) {
        return key != null && key.pool() == keyPool && map.containsKey(key.id());
    }
    public Ref<V> get(Ref<K> key) {
        Objects.requireNonNull(key, "key");
        if (key.pool() != keyPool) throw new IllegalArgumentException("foreign key pool");
        return valuePool.ref(map.get(key.id()));
    }
    public long primitivePayloadBytes() { return map.primitivePayloadBytes(); }

    public static final class Builder<K,V> {
        private final Pool<K> keyPool;
        private final Pool<V> valuePool;
        private int[] keys = new int[16];
        private int[] values = new int[16];
        private int size;

        private Builder(Pool<K> keyPool, Pool<V> valuePool) {
            this.keyPool = Objects.requireNonNull(keyPool, "keyPool");
            this.valuePool = Objects.requireNonNull(valuePool, "valuePool");
        }

        public Builder<K,V> put(Ref<K> key, Ref<V> value) {
            Objects.requireNonNull(key, "key");
            Objects.requireNonNull(value, "value");
            if (key.pool() != keyPool || value.pool() != valuePool) throw new
                    IllegalArgumentException("foreign MAT pool");
            if (size == keys.length) {
                keys = Arrays.copyOf(keys, size * 2);
                values = Arrays.copyOf(values, size * 2);
            }
            keys[size] = key.id(); values[size] = value.id(); size++;
            return this;
        }

        public IdToIdMap<K,V> build() {
            AdaptiveIntToIntMap.Builder builder = AdaptiveIntToIntMap.builder(
                    CollectionPolicy.BALANCED);
            for (int i=0;i<size;i++) builder.put(keys[i], values[i]);
            return new IdToIdMap<>(keyPool, valuePool, builder.build());
        }
    }
}
