/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.factory.primitive;

import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.block.function.primitive.BooleanBooleanToObjectFunction;
import org.eclipse.collections.api.factory.set.primitive.ImmutableBooleanSetFactory;
import org.eclipse.collections.api.factory.set.primitive.MutableBooleanSetFactory;
import org.eclipse.collections.api.set.primitive.BooleanSet;
import org.eclipse.collections.impl.set.immutable.primitive.ImmutableBooleanSetFactoryImpl;
import org.eclipse.collections.impl.set.mutable.primitive.MutableBooleanSetFactoryImpl;
import org.eclipse.collections.api.tuple.primitive.BooleanBooleanPair;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;

/**
 * BooleanSets is a static utility for creating {@link ImmutableBooleanSetFactory} and {@link MutableBooleanSetFactory}.
 * This file was automatically generated from template file primitiveSets.stg.
 *
 * @since 3.2.
 */
@SuppressWarnings("ConstantNamingConvention")
public final class BooleanSets
{
    public static final ImmutableBooleanSetFactory immutable = ImmutableBooleanSetFactoryImpl.INSTANCE;
    public static final MutableBooleanSetFactory mutable = MutableBooleanSetFactoryImpl.INSTANCE;

    private BooleanSets()
    {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }

    /**
     * @since 9.0
     */
    public static <A, B> LazyIterable<BooleanBooleanPair> cartesianProduct(BooleanSet set1, BooleanSet set2)
    {
        return BooleanSets.cartesianProduct(set1, set2, PrimitiveTuples::pair);
    }

    /**
     * @since 9.0
     */
    public static <A, B, C> LazyIterable<C> cartesianProduct(BooleanSet set1, BooleanSet set2, BooleanBooleanToObjectFunction<C> function)
    {
        return set1.asLazy()
                .flatCollect(first ->
                    set2.asLazy()
                        .collect(second -> function.value(first, second)));
    }
}
