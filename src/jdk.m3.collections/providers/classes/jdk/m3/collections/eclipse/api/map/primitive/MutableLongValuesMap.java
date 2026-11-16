/*
 * Copyright (c) 2026 Goldman Sachs.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.api.map.primitive;

import jdk.m3.collections.eclipse.api.bag.MutableBag;
import jdk.m3.collections.eclipse.api.bag.primitive.MutableLongBag;
import jdk.m3.collections.eclipse.api.block.function.primitive.LongToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.LongPredicate;
import jdk.m3.collections.eclipse.api.iterator.MutableLongIterator;

/**
 * This file was automatically generated from template file mutablePrimitiveValuesMap.stg.
 *
 * @since 6.0.
 */
public interface MutableLongValuesMap extends LongValuesMap
{
    @Override
    MutableLongBag select(LongPredicate predicate);

    @Override
    MutableLongBag reject(LongPredicate predicate);

    @Override
    <V> MutableBag<V> collect(LongToObjectFunction<? extends V> function);

    /**
     * Removes all entries from this map.
     */
    void clear();

    @Override
    MutableLongIterator longIterator();
}
