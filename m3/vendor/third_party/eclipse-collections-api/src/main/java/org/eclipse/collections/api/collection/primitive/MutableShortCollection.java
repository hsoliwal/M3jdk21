/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.api.collection.primitive;

import java.util.Collection;

import org.eclipse.collections.api.ShortIterable;
import org.eclipse.collections.api.block.function.primitive.ShortToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.ShortPredicate;
import org.eclipse.collections.api.block.procedure.primitive.ShortProcedure;
import org.eclipse.collections.api.iterator.MutableShortIterator;
import org.eclipse.collections.api.collection.MutableCollection;

/**
 * This file was automatically generated from template file mutablePrimitiveCollection.stg.
 *
 * @since 3.0.
 */
public interface MutableShortCollection extends ShortIterable
{
    @Override
    MutableShortIterator shortIterator();

    boolean add(short element);

    boolean addAll(short... source);

    boolean addAll(ShortIterable source);

    boolean remove(short value);

    boolean removeAll(ShortIterable source);

    boolean removeAll(short... source);

    /**
     * @since 9.1
     */
    default boolean removeIf(ShortPredicate predicate)
    {
        boolean changed = false;
        MutableShortIterator iterator = this.shortIterator();
        while (iterator.hasNext())
        {
            if (predicate.accept(iterator.next()))
            {
                iterator.remove();
                changed = true;
            }
        }
        return changed;
    }

    /**
     * @see Collection#retainAll(Collection)
     * @since 5.0
     */
    boolean retainAll(ShortIterable elements);

    /**
     * @see Collection#retainAll(Collection)
     * @since 5.0
     */
    boolean retainAll(short... source);

    void clear();

    @Override
    MutableShortCollection select(ShortPredicate predicate);

    @Override
    MutableShortCollection reject(ShortPredicate predicate);

    <V> MutableCollection<V> collect(ShortToObjectFunction<? extends V> function);

    MutableShortCollection with(short element);

    MutableShortCollection without(short element);

    MutableShortCollection withAll(ShortIterable elements);

    MutableShortCollection withoutAll(ShortIterable elements);

    MutableShortCollection asUnmodifiable();

    MutableShortCollection asSynchronized();

    ImmutableShortCollection toImmutable();

    /**
     * @since 9.0.
     */
    @Override
    default MutableShortCollection tap(ShortProcedure procedure)
    {
        this.forEach(procedure);
        return this;
    }

    /**
     * Creates a new empty mutable version of the same collection type.

     * @since 9.2.
     */
    default MutableShortCollection newEmpty()
    {
        throw new UnsupportedOperationException("Implement in concrete classes.");
    }
}
