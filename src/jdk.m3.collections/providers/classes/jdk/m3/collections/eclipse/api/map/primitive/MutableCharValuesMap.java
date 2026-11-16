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
import jdk.m3.collections.eclipse.api.bag.primitive.MutableCharBag;
import jdk.m3.collections.eclipse.api.block.function.primitive.CharToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.CharPredicate;
import jdk.m3.collections.eclipse.api.iterator.MutableCharIterator;

/**
 * This file was automatically generated from template file mutablePrimitiveValuesMap.stg.
 *
 * @since 6.0.
 */
public interface MutableCharValuesMap extends CharValuesMap
{
    @Override
    MutableCharBag select(CharPredicate predicate);

    @Override
    MutableCharBag reject(CharPredicate predicate);

    @Override
    <V> MutableBag<V> collect(CharToObjectFunction<? extends V> function);

    /**
     * Removes all entries from this map.
     */
    void clear();

    @Override
    MutableCharIterator charIterator();
}
