// Modified 2026 by Hitesh Soliwal and Synexia contributors: owned M3 array storage, pinned bounded helper closure and documented query-contract repairs.
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.m3.collections.apache;
import java.util.*;
import org.apache.commons.collections4.Transformer;
final class M3ApacheCollectionUtils {
static final Collection<Object> EMPTY_COLLECTION = Collections.emptyList();
public static <T> Collection<T> emptyIfNull(final Collection<T> collection) {
        return collection == null ? emptyCollection() : collection;
    }

public static <C> boolean addAll(final Collection<C> collection, final Iterator<? extends C> iterator) {
        Objects.requireNonNull(collection, "collection");
        Objects.requireNonNull(iterator, "iterator");
        boolean changed = false;
        while (iterator.hasNext()) {
            changed |= collection.add(iterator.next());
        }
        return changed;
    }
static <T> Collection<T> emptyCollection() { return Collections.emptyList(); }

}
