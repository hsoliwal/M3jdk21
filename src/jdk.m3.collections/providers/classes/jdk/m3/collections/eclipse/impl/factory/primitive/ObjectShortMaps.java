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

import jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableObjectShortMapFactory;
import jdk.m3.collections.eclipse.api.factory.map.primitive.MutableObjectShortMapFactory;
import jdk.m3.collections.eclipse.impl.map.immutable.primitive.ImmutableObjectShortMapFactoryImpl;
import jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableObjectShortMapFactoryImpl;

/**
 * ObjectShortMaps is a static utility for creating {@link ImmutableObjectShortMapFactory} and {@link MutableObjectShortMapFactory}.
 * This file was automatically generated from template file objectPrimitiveMaps.stg.
 *
 * @since 4.0.
 */
@SuppressWarnings("ConstantNamingConvention")
public final class ObjectShortMaps
{
    public static final ImmutableObjectShortMapFactory immutable = ImmutableObjectShortMapFactoryImpl.INSTANCE;
    public static final MutableObjectShortMapFactory mutable = MutableObjectShortMapFactoryImpl.INSTANCE;

    private ObjectShortMaps()
    {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }
}
