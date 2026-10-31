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
import org.eclipse.collections.api.block.function.primitive.DoubleDoubleToObjectFunction;
import org.eclipse.collections.api.factory.set.primitive.ImmutableDoubleSetFactory;
import org.eclipse.collections.api.factory.set.primitive.MutableDoubleSetFactory;
import org.eclipse.collections.api.set.primitive.DoubleSet;
import org.eclipse.collections.impl.set.immutable.primitive.ImmutableDoubleSetFactoryImpl;
import org.eclipse.collections.impl.set.mutable.primitive.MutableDoubleSetFactoryImpl;
import org.eclipse.collections.api.tuple.primitive.DoubleDoublePair;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;

/**
 * DoubleSets is a static utility for creating {@link ImmutableDoubleSetFactory} and {@link MutableDoubleSetFactory}.
 * This file was automatically generated from template file primitiveSets.stg.
 *
 * @since 3.2.
 */
@SuppressWarnings("ConstantNamingConvention")
public final class DoubleSets
{
    public static final ImmutableDoubleSetFactory immutable = ImmutableDoubleSetFactoryImpl.INSTANCE;
    public static final MutableDoubleSetFactory mutable = MutableDoubleSetFactoryImpl.INSTANCE;

    private DoubleSets()
    {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }

    /**
     * @since 9.0
     */
    public static <A, B> LazyIterable<DoubleDoublePair> cartesianProduct(DoubleSet set1, DoubleSet set2)
    {
        return DoubleSets.cartesianProduct(set1, set2, PrimitiveTuples::pair);
    }

    /**
     * @since 9.0
     */
    public static <A, B, C> LazyIterable<C> cartesianProduct(DoubleSet set1, DoubleSet set2, DoubleDoubleToObjectFunction<C> function)
    {
        return set1.asLazy()
                .flatCollect(first ->
                    set2.asLazy()
                        .collect(second -> function.value(first, second)));
    }
}
