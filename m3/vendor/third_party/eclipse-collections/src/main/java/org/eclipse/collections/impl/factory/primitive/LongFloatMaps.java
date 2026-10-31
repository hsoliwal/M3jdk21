/*
 * Copyright (c) 2022 Goldman Sachs.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.factory.primitive;

import org.eclipse.collections.api.factory.map.primitive.ImmutableLongFloatMapFactory;
import org.eclipse.collections.api.factory.map.primitive.MutableLongFloatMapFactory;
import org.eclipse.collections.impl.map.immutable.primitive.ImmutableLongFloatMapFactoryImpl;
import org.eclipse.collections.impl.map.mutable.primitive.MutableLongFloatMapFactoryImpl;

/**
 * LongFloatMaps is a static utility for creating {@link ImmutableLongFloatMapFactory} and {@link MutableLongFloatMapFactory}.
 * This file was automatically generated from template file primitivePrimitiveMaps.stg.
 *
 * @since 4.0.
 */
@SuppressWarnings("ConstantNamingConvention")
public final class LongFloatMaps
{
    public static final ImmutableLongFloatMapFactory immutable = ImmutableLongFloatMapFactoryImpl.INSTANCE;
    public static final MutableLongFloatMapFactory mutable = MutableLongFloatMapFactoryImpl.INSTANCE;

    private LongFloatMaps()
    {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }
}
