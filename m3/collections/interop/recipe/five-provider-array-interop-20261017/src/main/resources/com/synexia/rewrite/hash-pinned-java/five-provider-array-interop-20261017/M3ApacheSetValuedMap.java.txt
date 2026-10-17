/* SPDX-License-Identifier: Apache-2.0
 * Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
 * Existing M3 constructor and collection hooks originate in the SDK family recipe.
 * Apache implementations remain dependencies; the added inversion hook is adapted below.
 */
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
// retain the existing M3 SDK family constructor and collection hooks; adapt
// Apache 4.6.0 LinkedHashSetValuedLinkedHashMap inversion to those M3 array owners.
// No donor serialization contract is introduced.
package com.m3.collections.interop;

import com.m3.collections.M3LinkedHashMap;
import com.m3.collections.M3LinkedHashSet;
import java.util.Set;
import org.apache.commons.collections4.MultiMapUtils;
import org.apache.commons.collections4.multimap.AbstractSetValuedMap;

/**
 * Apache set-valued map semantics over an M3 key directory and key-only value
 * owners. Key and value encounter order are retained, duplicate mappings are
 * suppressed, and null keys and values are supported. Size counts mappings.
 *
 * <p>Contract views and iterators come from the pinned Apache implementation;
 * retained collection payload and primitive slot links belong to M3. Instances
 * are mutable and are not thread-safe.
 *
 * @param <K> key type
 * @param <V> value type
 */
public final class M3ApacheSetValuedMap<K, V> extends AbstractSetValuedMap<K, V> {
    /** Creates an empty map with independently owned M3 storage. */
    public M3ApacheSetValuedMap() {
        super(new M3LinkedHashMap<K, Set<V>>());
    }

    @Override
    protected Set<V> createCollection() {
        return new M3LinkedHashSet<>();
    }

    /** Creates an independent inverted map with M3-owned keys and value sets. */
    @Override
    public M3ApacheSetValuedMap<V, K> inverted() {
        return MultiMapUtils.invert(this, new M3ApacheSetValuedMap<>());
    }
}
