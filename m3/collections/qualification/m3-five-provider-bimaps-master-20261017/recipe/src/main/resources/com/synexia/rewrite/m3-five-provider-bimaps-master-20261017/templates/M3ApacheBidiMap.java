/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// SPDX-License-Identifier: Apache-2.0
// Modified 2026 by Hitesh Soliwal and Contributors to the Synexia Project:
// adapt Apache 4.6.0 DualLinkedHashBidiMap construction and inverse factory
// to existing M3 array owners; no donor serialization contract is introduced.
package com.m3.collections.interop;

import com.m3.collections.M3LinkedHashMap;
import java.util.Map;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.AbstractDualBidiMap;

/**
 * Apache bidirectional-map semantics with M3-owned forward and reverse indexes.
 * Each direction retains its encounter order. Null keys and values are supported.
 * Apache put semantics displace the previous key when a value is reused; mutation
 * through inherited entries, values, keys and iterators maintains both directions.
 *
 * <p>The inverse shares the two original M3 owners and its inverse is this map.
 * Apache owns compatibility views; M3 stores payloads and primitive encounter
 * links. Instances are mutable and are not thread-safe.
 *
 * @param <K> key type
 * @param <V> value type
 */
public final class M3ApacheBidiMap<K, V> extends AbstractDualBidiMap<K, V> {
    /** Creates independent M3 forward and reverse owners. */
    public M3ApacheBidiMap() {
        super(new M3LinkedHashMap<>(), new M3LinkedHashMap<>());
    }

    private M3ApacheBidiMap(Map<K, V> normalMap, Map<V, K> reverseMap,
            BidiMap<V, K> inverseMap) {
        super(normalMap, reverseMap, inverseMap);
    }

    @Override
    protected BidiMap<V, K> createBidiMap(Map<V, K> normalMap,
            Map<K, V> reverseMap, BidiMap<K, V> inverseMap) {
        return new M3ApacheBidiMap<>(normalMap, reverseMap, inverseMap);
    }
}
