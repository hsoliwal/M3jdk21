/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.api.map.primitive;

import org.eclipse.collections.api.ByteIterable;
import org.eclipse.collections.api.bag.Bag;
import org.eclipse.collections.api.bag.primitive.ByteBag;
import org.eclipse.collections.api.block.function.primitive.ByteToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.BytePredicate;
import org.eclipse.collections.api.block.procedure.primitive.ByteProcedure;
import org.eclipse.collections.api.collection.primitive.MutableByteCollection;

/**
 * This file was automatically generated from template file primitiveValuesMap.stg.
 *
 * @since 6.0.
 */
public interface ByteValuesMap extends ByteIterable
{
    /**
     * Returns whether or not this map contains the value.
     * @param value the value to test
     * @return if this collection contains the value
     */
    boolean containsValue(byte value);

    /**
     * Iterates through each value in this map.
     * @param procedure the procedure to invoke for each value in this map.
     */
    void forEachValue(ByteProcedure procedure);

    /**
     * Returns the values in this map as a separate collection. The returned collection is backed by the map, so any
     * changes made to the returned collection will affect the state of this map.
     * @return the values as a collection backed by this map
     */
    MutableByteCollection values();

    @Override
    ByteBag select(BytePredicate predicate);

    @Override
    ByteBag reject(BytePredicate predicate);

    /**
     * @since 9.0.
     */
    @Override
    default ByteValuesMap tap(ByteProcedure procedure)
    {
        this.forEach(procedure);
        return this;
    }

    @Override
    <V> Bag<V> collect(ByteToObjectFunction<? extends V> function);
}
