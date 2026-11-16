/*
 * Copyright (c) 2026 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.api.map.primitive;

import jdk.m3.collections.eclipse.api.LongIterable;
import jdk.m3.collections.eclipse.api.bag.Bag;
import jdk.m3.collections.eclipse.api.bag.primitive.LongBag;
import jdk.m3.collections.eclipse.api.block.function.primitive.LongToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.LongPredicate;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.LongProcedure;
import jdk.m3.collections.eclipse.api.collection.primitive.MutableLongCollection;

/**
 * This file was automatically generated from template file primitiveValuesMap.stg.
 *
 * @since 6.0.
 */
public interface LongValuesMap extends LongIterable
{
    /**
     * Returns whether or not this map contains the value.
     * @param value the value to test
     * @return if this collection contains the value
     */
    boolean containsValue(long value);

    /**
     * Iterates through each value in this map.
     * @param procedure the procedure to invoke for each value in this map.
     */
    void forEachValue(LongProcedure procedure);

    /**
     * Returns the values in this map as a separate collection. The returned collection is backed by the map, so any
     * changes made to the returned collection will affect the state of this map.
     * @return the values as a collection backed by this map
     */
    MutableLongCollection values();

    @Override
    LongBag select(LongPredicate predicate);

    @Override
    LongBag reject(LongPredicate predicate);

    /**
     * @since 9.0.
     */
    @Override
    default LongValuesMap tap(LongProcedure procedure)
    {
        this.forEach(procedure);
        return this;
    }

    @Override
    <V> Bag<V> collect(LongToObjectFunction<? extends V> function);
}
