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

import org.eclipse.collections.api.CharIterable;
import org.eclipse.collections.api.block.function.primitive.CharToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.CharPredicate;
import org.eclipse.collections.api.block.procedure.primitive.CharProcedure;
import org.eclipse.collections.api.iterator.MutableCharIterator;
import org.eclipse.collections.api.collection.MutableCollection;

/**
 * This file was automatically generated from template file mutablePrimitiveCollection.stg.
 *
 * @since 3.0.
 */
public interface MutableCharCollection extends CharIterable
{
    @Override
    MutableCharIterator charIterator();

    boolean add(char element);

    boolean addAll(char... source);

    boolean addAll(CharIterable source);

    boolean remove(char value);

    boolean removeAll(CharIterable source);

    boolean removeAll(char... source);

    /**
     * @since 9.1
     */
    default boolean removeIf(CharPredicate predicate)
    {
        boolean changed = false;
        MutableCharIterator iterator = this.charIterator();
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
    boolean retainAll(CharIterable elements);

    /**
     * @see Collection#retainAll(Collection)
     * @since 5.0
     */
    boolean retainAll(char... source);

    void clear();

    @Override
    MutableCharCollection select(CharPredicate predicate);

    @Override
    MutableCharCollection reject(CharPredicate predicate);

    <V> MutableCollection<V> collect(CharToObjectFunction<? extends V> function);

    MutableCharCollection with(char element);

    MutableCharCollection without(char element);

    MutableCharCollection withAll(CharIterable elements);

    MutableCharCollection withoutAll(CharIterable elements);

    MutableCharCollection asUnmodifiable();

    MutableCharCollection asSynchronized();

    ImmutableCharCollection toImmutable();

    /**
     * @since 9.0.
     */
    @Override
    default MutableCharCollection tap(CharProcedure procedure)
    {
        this.forEach(procedure);
        return this;
    }

    /**
     * Creates a new empty mutable version of the same collection type.

     * @since 9.2.
     */
    default MutableCharCollection newEmpty()
    {
        throw new UnsupportedOperationException("Implement in concrete classes.");
    }
}
