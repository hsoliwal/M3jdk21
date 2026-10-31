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

import org.eclipse.collections.api.LongIterable;
import org.eclipse.collections.api.block.function.primitive.LongToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.LongPredicate;
import org.eclipse.collections.api.block.procedure.primitive.LongProcedure;
import org.eclipse.collections.api.iterator.MutableLongIterator;
import org.eclipse.collections.api.collection.MutableCollection;

/**
 * This file was automatically generated from template file mutablePrimitiveCollection.stg.
 *
 * @since 3.0.
 */
public interface MutableLongCollection extends LongIterable
{
    @Override
    MutableLongIterator longIterator();

    boolean add(long element);

    boolean addAll(long... source);

    boolean addAll(LongIterable source);

    boolean remove(long value);

    boolean removeAll(LongIterable source);

    boolean removeAll(long... source);

    /**
     * @since 9.1
     */
    default boolean removeIf(LongPredicate predicate)
    {
        boolean changed = false;
        MutableLongIterator iterator = this.longIterator();
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
    boolean retainAll(LongIterable elements);

    /**
     * @see Collection#retainAll(Collection)
     * @since 5.0
     */
    boolean retainAll(long... source);

    void clear();

    @Override
    MutableLongCollection select(LongPredicate predicate);

    @Override
    MutableLongCollection reject(LongPredicate predicate);

    <V> MutableCollection<V> collect(LongToObjectFunction<? extends V> function);

    MutableLongCollection with(long element);

    MutableLongCollection without(long element);

    MutableLongCollection withAll(LongIterable elements);

    MutableLongCollection withoutAll(LongIterable elements);

    MutableLongCollection asUnmodifiable();

    MutableLongCollection asSynchronized();

    ImmutableLongCollection toImmutable();

    /**
     * @since 9.0.
     */
    @Override
    default MutableLongCollection tap(LongProcedure procedure)
    {
        this.forEach(procedure);
        return this;
    }

    /**
     * Creates a new empty mutable version of the same collection type.

     * @since 9.2.
     */
    default MutableLongCollection newEmpty()
    {
        throw new UnsupportedOperationException("Implement in concrete classes.");
    }
}
