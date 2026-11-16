/*
 * Copyright (c) 2026 Goldman Sachs.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.impl.factory.primitive;

import jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableDoubleIntMapFactory;
import jdk.m3.collections.eclipse.api.factory.map.primitive.MutableDoubleIntMapFactory;
import jdk.m3.collections.eclipse.impl.map.immutable.primitive.ImmutableDoubleIntMapFactoryImpl;
import jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableDoubleIntMapFactoryImpl;

/**
 * DoubleIntMaps is a static utility for creating {@link ImmutableDoubleIntMapFactory} and {@link MutableDoubleIntMapFactory}.
 * This file was automatically generated from template file primitivePrimitiveMaps.stg.
 *
 * @since 4.0.
 */
@SuppressWarnings("ConstantNamingConvention")
public final class DoubleIntMaps
{
    public static final ImmutableDoubleIntMapFactory immutable = ImmutableDoubleIntMapFactoryImpl.INSTANCE;
    public static final MutableDoubleIntMapFactory mutable = MutableDoubleIntMapFactoryImpl.INSTANCE;

    private DoubleIntMaps()
    {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }
}
