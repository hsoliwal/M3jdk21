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
import org.eclipse.collections.api.block.function.primitive.ShortShortToObjectFunction;
import org.eclipse.collections.api.factory.set.primitive.ImmutableShortSetFactory;
import org.eclipse.collections.api.factory.set.primitive.MutableShortSetFactory;
import org.eclipse.collections.api.set.primitive.ShortSet;
import org.eclipse.collections.impl.set.immutable.primitive.ImmutableShortSetFactoryImpl;
import org.eclipse.collections.impl.set.mutable.primitive.MutableShortSetFactoryImpl;
import org.eclipse.collections.api.tuple.primitive.ShortShortPair;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;

/**
 * ShortSets is a static utility for creating {@link ImmutableShortSetFactory} and {@link MutableShortSetFactory}.
 * This file was automatically generated from template file primitiveSets.stg.
 *
 * @since 3.2.
 */
@SuppressWarnings("ConstantNamingConvention")
public final class ShortSets
{
    public static final ImmutableShortSetFactory immutable = ImmutableShortSetFactoryImpl.INSTANCE;
    public static final MutableShortSetFactory mutable = MutableShortSetFactoryImpl.INSTANCE;

    private ShortSets()
    {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }

    /**
     * @since 9.0
     */
    public static <A, B> LazyIterable<ShortShortPair> cartesianProduct(ShortSet set1, ShortSet set2)
    {
        return ShortSets.cartesianProduct(set1, set2, PrimitiveTuples::pair);
    }

    /**
     * @since 9.0
     */
    public static <A, B, C> LazyIterable<C> cartesianProduct(ShortSet set1, ShortSet set2, ShortShortToObjectFunction<C> function)
    {
        return set1.asLazy()
                .flatCollect(first ->
                    set2.asLazy()
                        .collect(second -> function.value(first, second)));
    }
}
