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

import java.util.Set;

import org.eclipse.collections.api.CharIterable;
import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.block.function.primitive.CharToObjectFunction;
import org.eclipse.collections.api.block.predicate.primitive.CharPredicate;
import org.eclipse.collections.api.block.procedure.primitive.CharProcedure;
import org.eclipse.collections.api.set.SetIterable;
import org.eclipse.collections.api.tuple.primitive.CharCharPair;

/**
 * This file was automatically generated from template file primitiveSet.stg.
 *
 * @since 3.0.
 */
public interface CharSet extends CharIterable
{
    /**
     * @since 9.0.
     */
    @Override
    default CharSet tap(CharProcedure procedure)
    {
        this.forEach(procedure);
        return this;
    }

    /**
     * @since 11.0.
     */
    CharSet union(CharSet set);

    /**
     * Returns the set of all objects that are members of both {@code this} and {@code set}. The intersection of
     * [1, 2, 3] and [2, 3, 4] is the set [2, 3].
     *
     * @since 11.0.
     */
    CharSet intersect(CharSet set);

    /**
     * Returns the set of all members of {@code this} that are not members of {@code set}. The difference of
     * [1, 2, 3] and [2, 3, 4] is the set [1].
     *
     * @since 11.0.
     */
    CharSet difference(CharSet set);

    /**
     * Returns the set of all objects that are a member of exactly one of {@code this} and {@code set} (elements which
     * are in one of the sets, but not in both). For instance, for the sets [1, 2, 3] and [2, 3, 4], the symmetric
     * difference set is [1, 4] . It is the set difference of the union and the intersection.
     *
     * @since 11.0.
     */
    CharSet symmetricDifference(CharSet set);

    /**
     * Returns {@literal true} if all the members of {@code this} are also members of {@code set}.
     * For example, [1, 2] is a subset of [1, 2, 3], but [1, 4] is not.
     *
     * @since 11.0.
     */
    default boolean isSubsetOf(CharSet set)
    {
        return this.size() <= set.size() && set.containsAll(this);
    }

    /**
     * Returns {@literal true} if all the members of {@code this} are also members of {@code set} and the
     * two sets are not equal. For example, [1, 2] is a proper subset of [1, 2, 3], but [1, 2, 3] is not.
     *
     * @since 11.0.
     */
    default boolean isProperSubsetOf(CharSet set)
    {
        return this.size() < set.size() && set.containsAll(this);
    }

    /**
     * Returns the set whose members are all possible ordered pairs (a, b) where a is a member of {@code this} and b is a
     * member of {@code set}.
     *
     * @since 11.0.
     */
    LazyIterable<CharCharPair> cartesianProduct(CharSet set);

    /**
     * Follows the same general contract as {@link Set#equals(Object)}.
     */
    @Override
    boolean equals(Object o);

    /**
     * Follows the same general contract as {@link Set#hashCode()}.
     */
    @Override
    int hashCode();

    @Override
    CharSet select(CharPredicate predicate);

    @Override
    CharSet reject(CharPredicate predicate);

    @Override
    <V> SetIterable<V> collect(CharToObjectFunction<? extends V> function);

    /**
     * Returns a frozen copy of this set. If the set is frozen, it returns itself. A frozen copy is the same thing as an immutable copy without safe-publish guarantees.
     */
    CharSet freeze();

    /**
     * Returns an immutable copy of this set. If the set is immutable, it returns itself.
     */
    ImmutableCharSet toImmutable();
}
