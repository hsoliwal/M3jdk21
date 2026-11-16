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

import jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableShortObjectMapFactory;
import jdk.m3.collections.eclipse.api.factory.map.primitive.MutableShortObjectMapFactory;
import jdk.m3.collections.eclipse.impl.map.immutable.primitive.ImmutableShortObjectMapFactoryImpl;
import jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableShortObjectMapFactoryImpl;

/**
 * ShortObjectMaps is a static utility for creating {@link ImmutableShortObjectMapFactory} and {@link MutableShortObjectMapFactory}.
 * This file was automatically generated from template file primitiveObjectMaps.stg.
 *
 * @since 4.0.
 */
@SuppressWarnings("ConstantNamingConvention")
public final class ShortObjectMaps
{
    public static final ImmutableShortObjectMapFactory immutable = ImmutableShortObjectMapFactoryImpl.INSTANCE;
    public static final MutableShortObjectMapFactory mutable = MutableShortObjectMapFactoryImpl.INSTANCE;

    private ShortObjectMaps()
    {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }
}
