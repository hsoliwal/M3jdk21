/*
 * Copyright (c) 2026 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.api.collection.primitive;

import java.util.Collection;

import jdk.m3.collections.eclipse.api.DoubleIterable;
import jdk.m3.collections.eclipse.api.block.function.primitive.DoubleToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.DoublePredicate;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.DoubleProcedure;
import jdk.m3.collections.eclipse.api.iterator.MutableDoubleIterator;
import jdk.m3.collections.eclipse.api.collection.MutableCollection;

/**
 * This file was automatically generated from template file mutablePrimitiveCollection.stg.
 *
 * @since 3.0.
 */
public interface MutableDoubleCollection extends DoubleIterable
{
    @Override
    MutableDoubleIterator doubleIterator();

    boolean add(double element);

    boolean addAll(double... source);

    boolean addAll(DoubleIterable source);

    boolean remove(double value);

    boolean removeAll(DoubleIterable source);

    boolean removeAll(double... source);

    /**
     * @since 9.1
     */
    default boolean removeIf(DoublePredicate predicate)
    {
        boolean changed = false;
        MutableDoubleIterator iterator = this.doubleIterator();
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
    boolean retainAll(DoubleIterable elements);

    /**
     * @see Collection#retainAll(Collection)
     * @since 5.0
     */
    boolean retainAll(double... source);

    void clear();

    @Override
    MutableDoubleCollection select(DoublePredicate predicate);

    @Override
    MutableDoubleCollection reject(DoublePredicate predicate);

    <V> MutableCollection<V> collect(DoubleToObjectFunction<? extends V> function);

    MutableDoubleCollection with(double element);

    MutableDoubleCollection without(double element);

    MutableDoubleCollection withAll(DoubleIterable elements);

    MutableDoubleCollection withoutAll(DoubleIterable elements);

    MutableDoubleCollection asUnmodifiable();

    MutableDoubleCollection asSynchronized();

    ImmutableDoubleCollection toImmutable();

    /**
     * @since 9.0.
     */
    @Override
    default MutableDoubleCollection tap(DoubleProcedure procedure)
    {
        this.forEach(procedure);
        return this;
    }

    /**
     * Creates a new empty mutable version of the same collection type.

     * @since 9.2.
     */
    default MutableDoubleCollection newEmpty()
    {
        throw new UnsupportedOperationException("Implement in concrete classes.");
    }
}
