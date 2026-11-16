/*
 * Copyright (c) 2026 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.api.set.primitive;

import jdk.m3.collections.eclipse.api.CharIterable;
import jdk.m3.collections.eclipse.api.block.function.primitive.CharToObjectFunction;
import jdk.m3.collections.eclipse.api.block.predicate.primitive.CharPredicate;
import jdk.m3.collections.eclipse.api.block.procedure.primitive.CharProcedure;
import jdk.m3.collections.eclipse.api.collection.primitive.ImmutableCharCollection;
import jdk.m3.collections.eclipse.api.set.ImmutableSet;

/**
 * This file was automatically generated from template file immutablePrimitiveSet.stg.
 *
 * @since 3.0.
 */
public interface ImmutableCharSet extends ImmutableCharCollection, CharSet
{
    @Override
    ImmutableCharSet select(CharPredicate predicate);

    @Override
    ImmutableCharSet reject(CharPredicate predicate);

    /**
     * @since 9.0.
     */
    @Override
    default ImmutableCharSet tap(CharProcedure procedure)
    {
        this.forEach(procedure);
        return this;
    }

    @Override
    <V> ImmutableSet<V> collect(CharToObjectFunction<? extends V> function);

    /**
     * @since 11.0.
     */
    @Override
    default ImmutableCharSet union(CharSet set)
    {
        if (this.size() > set.size())
        {
            return this.toSet().withAll(set).toImmutable();
        }
        else
        {
            return set.toSet().withAll(this).toImmutable();
        }
    }

    /**
     * Returns the set of all objects that are members of both {@code this} and {@code set}. The intersection of
     * [1, 2, 3] and [2, 3, 4] is the set [2, 3].
     *
     * @since 11.0.
     */
    @Override
    default ImmutableCharSet intersect(CharSet set)
    {
        if (this.size() < set.size())
        {
            return this.select(set::contains);
        }
        else
        {
            return set.select(this::contains).toImmutable();
        }
    }

    /**
     * Returns the set of all members of {@code this} that are not members of {@code set}. The difference of
     * [1, 2, 3] and [2, 3, 4] is [1].
     *
     * @since 11.0.
     */
    @Override
    default ImmutableCharSet difference(CharSet set)
    {
        return this.reject(set::contains);
    }

    /**
     * Returns the set of all objects that are a member of exactly one of {@code this} and {@code set} (elements which
     * are in one of the sets, but not in both). For instance, for the sets [1, 2, 3] and [2, 3, 4], the symmetric
     * difference set is [1, 4] . It is the set difference of the union and the intersection.
     *
     * @since 11.0.
     */
    @Override
    default ImmutableCharSet symmetricDifference(CharSet set)
    {
        return this.toSet().symmetricDifference(set).toImmutable();
    }

    @Override
    ImmutableCharSet newWith(char element);

    @Override
    ImmutableCharSet newWithout(char element);

    @Override
    ImmutableCharSet newWithAll(CharIterable elements);

    @Override
    ImmutableCharSet newWithoutAll(CharIterable elements);
}
