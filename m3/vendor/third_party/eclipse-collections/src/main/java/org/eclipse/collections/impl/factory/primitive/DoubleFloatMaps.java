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

import org.eclipse.collections.api.factory.map.primitive.ImmutableDoubleFloatMapFactory;
import org.eclipse.collections.api.factory.map.primitive.MutableDoubleFloatMapFactory;
import org.eclipse.collections.impl.map.immutable.primitive.ImmutableDoubleFloatMapFactoryImpl;
import org.eclipse.collections.impl.map.mutable.primitive.MutableDoubleFloatMapFactoryImpl;

/**
 * DoubleFloatMaps is a static utility for creating {@link ImmutableDoubleFloatMapFactory} and {@link MutableDoubleFloatMapFactory}.
 * This file was automatically generated from template file primitivePrimitiveMaps.stg.
 *
 * @since 4.0.
 */
@SuppressWarnings("ConstantNamingConvention")
public final class DoubleFloatMaps
{
    public static final ImmutableDoubleFloatMapFactory immutable = ImmutableDoubleFloatMapFactoryImpl.INSTANCE;
    public static final MutableDoubleFloatMapFactory mutable = MutableDoubleFloatMapFactoryImpl.INSTANCE;

    private DoubleFloatMaps()
    {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }
}
