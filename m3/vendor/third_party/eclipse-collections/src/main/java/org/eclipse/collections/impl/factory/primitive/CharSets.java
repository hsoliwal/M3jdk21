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
import org.eclipse.collections.api.block.function.primitive.CharCharToObjectFunction;
import org.eclipse.collections.api.factory.set.primitive.ImmutableCharSetFactory;
import org.eclipse.collections.api.factory.set.primitive.MutableCharSetFactory;
import org.eclipse.collections.api.set.primitive.CharSet;
import org.eclipse.collections.impl.set.immutable.primitive.ImmutableCharSetFactoryImpl;
import org.eclipse.collections.impl.set.mutable.primitive.MutableCharSetFactoryImpl;
import org.eclipse.collections.api.tuple.primitive.CharCharPair;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;

/**
 * CharSets is a static utility for creating {@link ImmutableCharSetFactory} and {@link MutableCharSetFactory}.
 * This file was automatically generated from template file primitiveSets.stg.
 *
 * @since 3.2.
 */
@SuppressWarnings("ConstantNamingConvention")
public final class CharSets
{
    public static final ImmutableCharSetFactory immutable = ImmutableCharSetFactoryImpl.INSTANCE;
    public static final MutableCharSetFactory mutable = MutableCharSetFactoryImpl.INSTANCE;

    private CharSets()
    {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }

    /**
     * @since 9.0
     */
    public static <A, B> LazyIterable<CharCharPair> cartesianProduct(CharSet set1, CharSet set2)
    {
        return CharSets.cartesianProduct(set1, set2, PrimitiveTuples::pair);
    }

    /**
     * @since 9.0
     */
    public static <A, B, C> LazyIterable<C> cartesianProduct(CharSet set1, CharSet set2, CharCharToObjectFunction<C> function)
    {
        return set1.asLazy()
                .flatCollect(first ->
                    set2.asLazy()
                        .collect(second -> function.value(first, second)));
    }
}
