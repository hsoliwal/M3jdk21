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
import jdk.m3.collections.eclipse.api.block.function.primitive.LongLongToObjectFunction;
import jdk.m3.collections.eclipse.api.factory.set.primitive.ImmutableLongSetFactory;
import jdk.m3.collections.eclipse.api.factory.set.primitive.MutableLongSetFactory;
import jdk.m3.collections.eclipse.api.set.primitive.LongSet;
import jdk.m3.collections.eclipse.impl.set.immutable.primitive.ImmutableLongSetFactoryImpl;
import jdk.m3.collections.eclipse.impl.set.mutable.primitive.MutableLongSetFactoryImpl;
import jdk.m3.collections.eclipse.api.tuple.primitive.LongLongPair;
import jdk.m3.collections.eclipse.impl.tuple.primitive.PrimitiveTuples;

/**
 * LongSets is a static utility for creating {@link ImmutableLongSetFactory} and {@link MutableLongSetFactory}.
 * This file was automatically generated from template file primitiveSets.stg.
 *
 * @since 3.2.
 */
@SuppressWarnings("ConstantNamingConvention")
public final class LongSets
{
    public static final ImmutableLongSetFactory immutable = ImmutableLongSetFactoryImpl.INSTANCE;
    public static final MutableLongSetFactory mutable = MutableLongSetFactoryImpl.INSTANCE;

    private LongSets()
    {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }

    /**
     * @since 9.0
     */
    public static <A, B> LazyIterable<LongLongPair> cartesianProduct(LongSet set1, LongSet set2)
    {
        return LongSets.cartesianProduct(set1, set2, PrimitiveTuples::pair);
    }

    /**
     * @since 9.0
     */
    public static <A, B, C> LazyIterable<C> cartesianProduct(LongSet set1, LongSet set2, LongLongToObjectFunction<C> function)
    {
        return set1.asLazy()
                .flatCollect(first ->
                    set2.asLazy()
                        .collect(second -> function.value(first, second)));
    }
}
