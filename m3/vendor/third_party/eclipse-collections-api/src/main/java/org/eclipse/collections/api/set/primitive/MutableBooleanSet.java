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

import org.eclipse.collections.api.BooleanIterable;
import org.eclipse.collections.api.block.function.primitive.BooleanToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.BooleanPredicate;
import org.eclipse.collections.api.block.procedure.primitive.BooleanProcedure;
import org.eclipse.collections.api.collection.primitive.MutableBooleanCollection;
import org.eclipse.collections.api.set.MutableSet;

/**
 * This file was automatically generated from template file mutablePrimitiveSet.stg.
 *
 * @since 3.0.
 */
public interface MutableBooleanSet extends MutableBooleanCollection, BooleanSet
{
    @Override
    MutableBooleanSet select(BooleanPredicate predicate);

    @Override
    MutableBooleanSet reject(BooleanPredicate predicate);

    /**
     * @since 9.0.
     */
    @Override
    default MutableBooleanSet tap(BooleanProcedure procedure)
    {
        this.forEach(procedure);
        return this;
    }

    @Override
    <V> MutableSet<V> collect(BooleanToObjectFunction<? extends V> function);

    @Override
    MutableBooleanSet with(boolean element);

    @Override
    MutableBooleanSet without(boolean element);

    @Override
    MutableBooleanSet withAll(BooleanIterable elements);

    @Override
    MutableBooleanSet withoutAll(BooleanIterable elements);

    @Override
    MutableBooleanSet asUnmodifiable();

    @Override
    MutableBooleanSet asSynchronized();

    /**
     * Returns a frozen copy of this set. A frozen copy is the same thing as an immutable copy without safe-publish guarantees.
     */
    @Override
    BooleanSet freeze();

    /**
     * Returns an immutable copy of this set.
     */
    @Override
    ImmutableBooleanSet toImmutable();

    /**
     * Creates a new empty mutable version of the same Set type.
     *
     * @since 9.2.
     */
    default MutableBooleanSet newEmpty()
    {
        throw new UnsupportedOperationException("Implement in concrete classes.");
    }

    /**
     * @since 11.0.
     */
    @Override
    default MutableBooleanSet union(BooleanSet set)
    {
        if (this.size() > set.size())
        {
            return this.toSet().withAll(set);
        }
        else
        {
            return set.toSet().withAll(this);
        }
    }

    /**
     * Returns the set of all objects that are members of both {@code this} and {@code set}. The intersection of
     * [1, 2, 3] and [2, 3, 4] is the set [2, 3].
     *
     * @since 11.0.
     */
    @Override
    default MutableBooleanSet intersect(BooleanSet set)
    {
        if (this.size() < set.size())
        {
            return this.select(set::contains);
        }
        else
        {
            return set.select(this::contains, this.newEmpty());
        }
    }

    /**
     * Returns the set of all members of {@code this} that are not members of {@code set}. The difference of
     * [1, 2, 3] and [2, 3, 4] is [1].
     *
     * @since 11.0.
     */
    @Override
    default MutableBooleanSet difference(BooleanSet set)
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
    default MutableBooleanSet symmetricDifference(BooleanSet set)
    {
        return set.reject(this::contains, this.difference(set));
    }
}
