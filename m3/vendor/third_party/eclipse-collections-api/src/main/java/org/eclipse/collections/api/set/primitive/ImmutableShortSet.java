/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.api.set.primitive;

import org.eclipse.collections.api.ShortIterable;
import org.eclipse.collections.api.block.function.primitive.ShortToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.ShortPredicate;
import org.eclipse.collections.api.block.procedure.primitive.ShortProcedure;
import org.eclipse.collections.api.collection.primitive.ImmutableShortCollection;
import org.eclipse.collections.api.set.ImmutableSet;

/**
 * This file was automatically generated from template file immutablePrimitiveSet.stg.
 *
 * @since 3.0.
 */
public interface ImmutableShortSet extends ImmutableShortCollection, ShortSet
{
    @Override
    ImmutableShortSet select(ShortPredicate predicate);

    @Override
    ImmutableShortSet reject(ShortPredicate predicate);

    /**
     * @since 9.0.
     */
    @Override
    default ImmutableShortSet tap(ShortProcedure procedure)
    {
        this.forEach(procedure);
        return this;
    }

    @Override
    <V> ImmutableSet<V> collect(ShortToObjectFunction<? extends V> function);

    /**
     * @since 11.0.
     */
    @Override
    default ImmutableShortSet union(ShortSet set)
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
    default ImmutableShortSet intersect(ShortSet set)
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
    default ImmutableShortSet difference(ShortSet set)
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
    default ImmutableShortSet symmetricDifference(ShortSet set)
    {
        return this.toSet().symmetricDifference(set).toImmutable();
    }

    @Override
    ImmutableShortSet newWith(short element);

    @Override
    ImmutableShortSet newWithout(short element);

    @Override
    ImmutableShortSet newWithAll(ShortIterable elements);

    @Override
    ImmutableShortSet newWithoutAll(ShortIterable elements);
}
