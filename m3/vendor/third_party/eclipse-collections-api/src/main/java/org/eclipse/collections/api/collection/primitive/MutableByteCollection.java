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

import org.eclipse.collections.api.ByteIterable;
import org.eclipse.collections.api.block.function.primitive.ByteToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.BytePredicate;
import org.eclipse.collections.api.block.procedure.primitive.ByteProcedure;
import org.eclipse.collections.api.iterator.MutableByteIterator;
import org.eclipse.collections.api.collection.MutableCollection;

/**
 * This file was automatically generated from template file mutablePrimitiveCollection.stg.
 *
 * @since 3.0.
 */
public interface MutableByteCollection extends ByteIterable
{
    @Override
    MutableByteIterator byteIterator();

    boolean add(byte element);

    boolean addAll(byte... source);

    boolean addAll(ByteIterable source);

    boolean remove(byte value);

    boolean removeAll(ByteIterable source);

    boolean removeAll(byte... source);

    /**
     * @since 9.1
     */
    default boolean removeIf(BytePredicate predicate)
    {
        boolean changed = false;
        MutableByteIterator iterator = this.byteIterator();
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
    boolean retainAll(ByteIterable elements);

    /**
     * @see Collection#retainAll(Collection)
     * @since 5.0
     */
    boolean retainAll(byte... source);

    void clear();

    @Override
    MutableByteCollection select(BytePredicate predicate);

    @Override
    MutableByteCollection reject(BytePredicate predicate);

    <V> MutableCollection<V> collect(ByteToObjectFunction<? extends V> function);

    MutableByteCollection with(byte element);

    MutableByteCollection without(byte element);

    MutableByteCollection withAll(ByteIterable elements);

    MutableByteCollection withoutAll(ByteIterable elements);

    MutableByteCollection asUnmodifiable();

    MutableByteCollection asSynchronized();

    ImmutableByteCollection toImmutable();

    /**
     * @since 9.0.
     */
    @Override
    default MutableByteCollection tap(ByteProcedure procedure)
    {
        this.forEach(procedure);
        return this;
    }

    /**
     * Creates a new empty mutable version of the same collection type.

     * @since 9.2.
     */
    default MutableByteCollection newEmpty()
    {
        throw new UnsupportedOperationException("Implement in concrete classes.");
    }
}
