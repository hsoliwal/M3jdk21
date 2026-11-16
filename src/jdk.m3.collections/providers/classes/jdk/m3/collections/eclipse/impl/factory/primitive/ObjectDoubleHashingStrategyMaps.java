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

import jdk.m3.collections.eclipse.api.factory.map.primitive.MutableObjectDoubleHashingStrategyMapFactory;
import jdk.m3.collections.eclipse.impl.map.mutable.primitive.MutableObjectDoubleHashingStrategyMapFactoryImpl;

/**
 * ObjectDoubleHashingStrategyMaps is a static utility for creating {@link MutableObjectDoubleHashingStrategyMapFactory}.
 * This file was automatically generated from template file objectPrimitiveHashingStrategyMaps.stg.
 *
 * @since 11.1.
 */
@SuppressWarnings("ConstantNamingConvention")
public final class ObjectDoubleHashingStrategyMaps
{
    public static final MutableObjectDoubleHashingStrategyMapFactory mutable = MutableObjectDoubleHashingStrategyMapFactoryImpl.INSTANCE;

    private ObjectDoubleHashingStrategyMaps()
    {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }
}
