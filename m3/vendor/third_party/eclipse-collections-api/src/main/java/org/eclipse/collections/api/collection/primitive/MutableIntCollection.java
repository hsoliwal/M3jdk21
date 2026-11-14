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

import org.eclipse.collections.api.IntIterable;
import org.eclipse.collections.api.block.function.primitive.IntToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.IntPredicate;
import org.eclipse.collections.api.block.procedure.primitive.IntProcedure;
import org.eclipse.collections.api.iterator.MutableIntIterator;
import org.eclipse.collections.api.collection.MutableCollection;

/**
 * This file was automatically generated from template file mutablePrimitiveCollection.stg.
 *
 * @since 3.0.
 */
public interface MutableIntCollection extends IntIterable
{
    @Override
    MutableIntIterator intIterator();

    boolean add(int element);

    boolean addAll(int... source);

    boolean addAll(IntIterable source);

    boolean remove(int value);

    boolean removeAll(IntIterable source);

    boolean removeAll(int... source);

    /**
     * @since 9.1
     */
    default boolean removeIf(IntPredicate predicate)
    {
        boolean changed = false;
        MutableIntIterator iterator = this.intIterator();
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
    boolean retainAll(IntIterable elements);

    /**
     * @see Collection#retainAll(Collection)
     * @since 5.0
     */
    boolean retainAll(int... source);

    void clear();

    @Override
    MutableIntCollection select(IntPredicate predicate);

    @Override
    MutableIntCollection reject(IntPredicate predicate);

    <V> MutableCollection<V> collect(IntToObjectFunction<? extends V> function);

    MutableIntCollection with(int element);

    MutableIntCollection without(int element);

    MutableIntCollection withAll(IntIterable elements);

    MutableIntCollection withoutAll(IntIterable elements);

    MutableIntCollection asUnmodifiable();

    MutableIntCollection asSynchronized();

    ImmutableIntCollection toImmutable();

    /**
     * @since 9.0.
     */
    @Override
    default MutableIntCollection tap(IntProcedure procedure)
    {
        this.forEach(procedure);
        return this;
    }

    /**
     * Creates a new empty mutable version of the same collection type.

     * @since 9.2.
     */
    default MutableIntCollection newEmpty()
    {
        throw new UnsupportedOperationException("Implement in concrete classes.");
    }
}
