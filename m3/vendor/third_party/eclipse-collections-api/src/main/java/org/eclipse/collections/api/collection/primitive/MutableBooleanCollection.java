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

import org.eclipse.collections.api.BooleanIterable;
import org.eclipse.collections.api.block.function.primitive.BooleanToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.BooleanPredicate;
import org.eclipse.collections.api.block.procedure.primitive.BooleanProcedure;
import org.eclipse.collections.api.iterator.MutableBooleanIterator;
import org.eclipse.collections.api.collection.MutableCollection;

/**
 * This file was automatically generated from template file mutablePrimitiveCollection.stg.
 *
 * @since 3.0.
 */
public interface MutableBooleanCollection extends BooleanIterable
{
    @Override
    MutableBooleanIterator booleanIterator();

    boolean add(boolean element);

    boolean addAll(boolean... source);

    boolean addAll(BooleanIterable source);

    boolean remove(boolean value);

    boolean removeAll(BooleanIterable source);

    boolean removeAll(boolean... source);

    /**
     * @since 9.1
     */
    default boolean removeIf(BooleanPredicate predicate)
    {
        boolean changed = false;
        MutableBooleanIterator iterator = this.booleanIterator();
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
    boolean retainAll(BooleanIterable elements);

    /**
     * @see Collection#retainAll(Collection)
     * @since 5.0
     */
    boolean retainAll(boolean... source);

    void clear();

    @Override
    MutableBooleanCollection select(BooleanPredicate predicate);

    @Override
    MutableBooleanCollection reject(BooleanPredicate predicate);

    <V> MutableCollection<V> collect(BooleanToObjectFunction<? extends V> function);

    MutableBooleanCollection with(boolean element);

    MutableBooleanCollection without(boolean element);

    MutableBooleanCollection withAll(BooleanIterable elements);

    MutableBooleanCollection withoutAll(BooleanIterable elements);

    MutableBooleanCollection asUnmodifiable();

    MutableBooleanCollection asSynchronized();

    ImmutableBooleanCollection toImmutable();

    /**
     * @since 9.0.
     */
    @Override
    default MutableBooleanCollection tap(BooleanProcedure procedure)
    {
        this.forEach(procedure);
        return this;
    }

    /**
     * Creates a new empty mutable version of the same collection type.

     * @since 9.2.
     */
    default MutableBooleanCollection newEmpty()
    {
        throw new UnsupportedOperationException("Implement in concrete classes.");
    }
}
