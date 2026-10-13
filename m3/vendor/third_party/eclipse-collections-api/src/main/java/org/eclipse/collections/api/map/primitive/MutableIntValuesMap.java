/*
 * Copyright (c) 2022 Goldman Sachs.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.api.map.primitive;

import org.eclipse.collections.api.bag.MutableBag;
import org.eclipse.collections.api.bag.primitive.MutableIntBag;
import org.eclipse.collections.api.block.function.primitive.IntToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.IntPredicate;
import org.eclipse.collections.api.iterator.MutableIntIterator;

/**
 * This file was automatically generated from template file mutablePrimitiveValuesMap.stg.
 *
 * @since 6.0.
 */
public interface MutableIntValuesMap extends IntValuesMap
{
    @Override
    MutableIntBag select(IntPredicate predicate);

    @Override
    MutableIntBag reject(IntPredicate predicate);

    @Override
    <V> MutableBag<V> collect(IntToObjectFunction<? extends V> function);

    /**
     * Removes all entries from this map.
     */
    void clear();

    @Override
    MutableIntIterator intIterator();
}
