/*
 * Copyright (c) 2026 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.impl.factory.primitive;

import jdk.m3.collections.eclipse.api.LazyIterable;
import jdk.m3.collections.eclipse.api.block.function.primitive.IntIntToObjectFunction;
import jdk.m3.collections.eclipse.api.factory.set.primitive.ImmutableIntSetFactory;
import jdk.m3.collections.eclipse.api.factory.set.primitive.MutableIntSetFactory;
import jdk.m3.collections.eclipse.api.set.primitive.IntSet;
import jdk.m3.collections.eclipse.impl.set.immutable.primitive.ImmutableIntSetFactoryImpl;
import jdk.m3.collections.eclipse.impl.set.mutable.primitive.MutableIntSetFactoryImpl;
import jdk.m3.collections.eclipse.api.tuple.primitive.IntIntPair;
import jdk.m3.collections.eclipse.impl.tuple.primitive.PrimitiveTuples;

/**
 * IntSets is a static utility for creating {@link ImmutableIntSetFactory} and {@link MutableIntSetFactory}.
 * This file was automatically generated from template file primitiveSets.stg.
 *
 * @since 3.2.
 */
@SuppressWarnings("ConstantNamingConvention")
public final class IntSets
{
    public static final ImmutableIntSetFactory immutable = ImmutableIntSetFactoryImpl.INSTANCE;
    public static final MutableIntSetFactory mutable = MutableIntSetFactoryImpl.INSTANCE;

    private IntSets()
    {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }

    /**
     * @since 9.0
     */
    public static <A, B> LazyIterable<IntIntPair> cartesianProduct(IntSet set1, IntSet set2)
    {
        return IntSets.cartesianProduct(set1, set2, PrimitiveTuples::pair);
    }

    /**
     * @since 9.0
     */
    public static <A, B, C> LazyIterable<C> cartesianProduct(IntSet set1, IntSet set2, IntIntToObjectFunction<C> function)
    {
        return set1.asLazy()
                .flatCollect(first ->
                    set2.asLazy()
                        .collect(second -> function.value(first, second)));
    }
}
