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

/**
 * Provides an implementation of an empty map iterator.
 *
 * @param <K> the type of keys
 * @param <V> the type of mapped values
 * @since 4.0
 */
public abstract class M3ApacheAbstractEmptyMapIterator<K, V> extends M3ApacheAbstractEmptyIterator<K> {

    /**
     * Create a new M3ApacheAbstractEmptyMapIterator.
     */
    public M3ApacheAbstractEmptyMapIterator() {
    }

    /**
     * Always throws IllegalStateException.
     *
     * @return Always throws IllegalStateException.
     * @throws IllegalStateException Always thrown.
     */
    public K getKey() {
        throw new IllegalStateException("Iterator contains no elements");
    }

    /**
     * Always throws IllegalStateException.
     *
     * @return Always throws IllegalStateException.
     * @throws IllegalStateException Always thrown.
     */
    public V getValue() {
        throw new IllegalStateException("Iterator contains no elements");
    }

    /**
     * Always throws IllegalStateException.
     *
     * @param ignored ignored.
     * @return Always throws IllegalStateException.
     * @throws IllegalStateException Always thrown.
     */
    public V setValue(final V ignored) {
        throw new IllegalStateException("Iterator contains no elements");
    }

}
