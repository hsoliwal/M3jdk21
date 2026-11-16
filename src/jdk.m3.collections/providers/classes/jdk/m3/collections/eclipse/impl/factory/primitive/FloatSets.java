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
import jdk.m3.collections.eclipse.api.block.function.primitive.FloatFloatToObjectFunction;
import jdk.m3.collections.eclipse.api.factory.set.primitive.ImmutableFloatSetFactory;
import jdk.m3.collections.eclipse.api.factory.set.primitive.MutableFloatSetFactory;
import jdk.m3.collections.eclipse.api.set.primitive.FloatSet;
import jdk.m3.collections.eclipse.impl.set.immutable.primitive.ImmutableFloatSetFactoryImpl;
import jdk.m3.collections.eclipse.impl.set.mutable.primitive.MutableFloatSetFactoryImpl;
import jdk.m3.collections.eclipse.api.tuple.primitive.FloatFloatPair;
import jdk.m3.collections.eclipse.impl.tuple.primitive.PrimitiveTuples;

/**
 * FloatSets is a static utility for creating {@link ImmutableFloatSetFactory} and {@link MutableFloatSetFactory}.
 * This file was automatically generated from template file primitiveSets.stg.
 *
 * @since 3.2.
 */
@SuppressWarnings("ConstantNamingConvention")
public final class FloatSets
{
    public static final ImmutableFloatSetFactory immutable = ImmutableFloatSetFactoryImpl.INSTANCE;
    public static final MutableFloatSetFactory mutable = MutableFloatSetFactoryImpl.INSTANCE;

    private FloatSets()
    {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }

    /**
     * @since 9.0
     */
    public static <A, B> LazyIterable<FloatFloatPair> cartesianProduct(FloatSet set1, FloatSet set2)
    {
        return FloatSets.cartesianProduct(set1, set2, PrimitiveTuples::pair);
    }

    /**
     * @since 9.0
     */
    public static <A, B, C> LazyIterable<C> cartesianProduct(FloatSet set1, FloatSet set2, FloatFloatToObjectFunction<C> function)
    {
        return set1.asLazy()
                .flatCollect(first ->
                    set2.asLazy()
                        .collect(second -> function.value(first, second)));
    }
}
