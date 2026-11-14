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

import org.eclipse.collections.api.FloatIterable;
import org.eclipse.collections.api.block.function.primitive.FloatToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.FloatPredicate;
import org.eclipse.collections.api.block.procedure.primitive.FloatProcedure;
import org.eclipse.collections.api.iterator.MutableFloatIterator;
import org.eclipse.collections.api.collection.MutableCollection;

/**
 * This file was automatically generated from template file mutablePrimitiveCollection.stg.
 *
 * @since 3.0.
 */
public interface MutableFloatCollection extends FloatIterable
{
    @Override
    MutableFloatIterator floatIterator();

    boolean add(float element);

    boolean addAll(float... source);

    boolean addAll(FloatIterable source);

    boolean remove(float value);

    boolean removeAll(FloatIterable source);

    boolean removeAll(float... source);

    /**
     * @since 9.1
     */
    default boolean removeIf(FloatPredicate predicate)
    {
        boolean changed = false;
        MutableFloatIterator iterator = this.floatIterator();
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
    boolean retainAll(FloatIterable elements);

    /**
     * @see Collection#retainAll(Collection)
     * @since 5.0
     */
    boolean retainAll(float... source);

    void clear();

    @Override
    MutableFloatCollection select(FloatPredicate predicate);

    @Override
    MutableFloatCollection reject(FloatPredicate predicate);

    <V> MutableCollection<V> collect(FloatToObjectFunction<? extends V> function);

    MutableFloatCollection with(float element);

    MutableFloatCollection without(float element);

    MutableFloatCollection withAll(FloatIterable elements);

    MutableFloatCollection withoutAll(FloatIterable elements);

    MutableFloatCollection asUnmodifiable();

    MutableFloatCollection asSynchronized();

    ImmutableFloatCollection toImmutable();

    /**
     * @since 9.0.
     */
    @Override
    default MutableFloatCollection tap(FloatProcedure procedure)
    {
        this.forEach(procedure);
        return this;
    }

    /**
     * Creates a new empty mutable version of the same collection type.

     * @since 9.2.
     */
    default MutableFloatCollection newEmpty()
    {
        throw new UnsupportedOperationException("Implement in concrete classes.");
    }
}
