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

import java.util.Set;


/**
 * Decorates another {@code Set} to provide additional behavior.
 * <p>
 * Methods are forwarded directly to the decorated set.
 * </p>
 *
 * @param <E> the type of the elements in this set
 * @since 3.0
 */
public abstract class M3ApacheAbstractSetDecorator<E> extends M3ApacheAbstractCollectionDecorator<E> implements
        Set<E> {

    /** Serialization version */
    private static final long serialVersionUID = -4678668309576958546L;

    /**
     * Constructor only used in deserialization, do not use otherwise.
     * @since 3.1
     */
    protected M3ApacheAbstractSetDecorator() {
    }

    /**
     * Constructor that wraps (not copies).
     *
     * @param set  the set to decorate, must not be null
     * @throws NullPointerException if set is null
     */
    protected M3ApacheAbstractSetDecorator(final Set<E> set) {
        super(set);
    }

    /**
     * Gets the set being decorated.
     *
     * @return the decorated set
     */
    @Override
    protected Set<E> decorated() {
        return (Set<E>) super.decorated();
    }

    @Override
    public boolean equals(final Object object) {
        return object == this || decorated().equals(object);
    }

    @Override
    public int hashCode() {
        return decorated().hashCode();
    }

}
