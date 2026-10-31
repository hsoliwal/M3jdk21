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

import org.eclipse.collections.api.factory.map.primitive.MutableObjectIntHashingStrategyMapFactory;
import org.eclipse.collections.impl.map.mutable.primitive.MutableObjectIntHashingStrategyMapFactoryImpl;

/**
 * ObjectIntHashingStrategyMaps is a static utility for creating {@link MutableObjectIntHashingStrategyMapFactory}.
 * This file was automatically generated from template file objectPrimitiveHashingStrategyMaps.stg.
 *
 * @since 11.1.
 */
@SuppressWarnings("ConstantNamingConvention")
public final class ObjectIntHashingStrategyMaps
{
    public static final MutableObjectIntHashingStrategyMapFactory mutable = MutableObjectIntHashingStrategyMapFactoryImpl.INSTANCE;

    private ObjectIntHashingStrategyMaps()
    {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }
}
