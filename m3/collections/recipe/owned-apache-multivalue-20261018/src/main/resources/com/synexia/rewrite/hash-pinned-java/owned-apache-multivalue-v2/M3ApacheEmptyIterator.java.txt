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

import java.util.Iterator;

import org.apache.commons.collections4.ResettableIterator;

/**
 * Provides an implementation of an empty iterator.
 * <p>
 * This class provides an implementation of an empty iterator.
 * This class provides for binary compatibility between Commons Collections
 * 2.1.1 and 3.1 due to issues with {@code M3ApacheIteratorUtils}.
 * </p>
 *
 * @param <E> the type of elements returned by this iterator.
 * @since 2.1.1 and 3.1
 */
public class M3ApacheEmptyIterator<E> extends M3ApacheAbstractEmptyIterator<E> {

    /**
     * Singleton instance of the iterator.
     *
     * @since 3.1
     */
    @SuppressWarnings("rawtypes")
    public static final ResettableIterator RESETTABLE_INSTANCE = new M3ApacheEmptyIterator<>();

    /**
     * Singleton instance of the iterator.
     *
     * @since 2.1.1 and 3.1
     */
    @SuppressWarnings("rawtypes")
    public static final Iterator INSTANCE = RESETTABLE_INSTANCE;

    /**
     * Gets a typed empty iterator instance.
     *
     * @param <E> the element type
     * @return Iterator&lt;E&gt;
     */
    @SuppressWarnings("unchecked") // An empty singleton cannot produce an element of the wrong type.
    public static <E> Iterator<E> emptyIterator() {
        return INSTANCE;
    }

    /**
     * Gets a typed resettable empty iterator instance.
     *
     * @param <E> the element type
     * @return ResettableIterator&lt;E&gt;
     */
    @SuppressWarnings("unchecked") // Same stateless empty singleton.
    public static <E> ResettableIterator<E> resettableEmptyIterator() {
        return RESETTABLE_INSTANCE;
    }

    /**
     * Constructs a new instance.
     */
    protected M3ApacheEmptyIterator() {
    }

}
