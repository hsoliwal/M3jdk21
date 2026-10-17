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
// Apache 4.6.0 ArrayListValuedLinkedHashMap inversion to those M3 array owners.
// No donor serialization contract is introduced.
package com.m3.collections.interop;

import com.m3.collections.M3LinkedHashMap;
import com.m3.collections.M3LinkedList;
import java.util.List;
import org.apache.commons.collections4.MultiMapUtils;
import org.apache.commons.collections4.multimap.AbstractListValuedMap;

/**
 * Apache list-valued map semantics over M3 key and value owners.
 * Key encounter order and per-key value order are retained; duplicate and null
 * values are supported. Size counts mappings. The inherited Apache views retain
 * their exact version-specific behavior, including raw sublist behavior.
 *
 * <p>The inherited implementation owns contract views and iterators; M3 owns
 * their retained collection payload and primitive slot links. Positional value
 * access follows {@link M3LinkedList}, so it is not constant-time array access.
 * Instances are mutable and are not thread-safe.
 *
 * @param <K> key type
 * @param <V> value type
 */
public final class M3ApacheListValuedMap<K, V> extends AbstractListValuedMap<K, V> {
    /** Creates an empty map with independently owned M3 storage. */
    public M3ApacheListValuedMap() {
        super(new M3LinkedHashMap<K, List<V>>());
    }

    @Override
    protected List<V> createCollection() {
        return new M3LinkedList<>();
    }

    /** Creates an independent inverted map with M3-owned keys and value lists. */
    @Override
    public M3ApacheListValuedMap<V, K> inverted() {
        return MultiMapUtils.invert(this, new M3ApacheListValuedMap<>());
    }
}
