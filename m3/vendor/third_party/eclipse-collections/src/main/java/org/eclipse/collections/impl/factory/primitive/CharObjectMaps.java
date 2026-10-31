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

import org.eclipse.collections.api.factory.map.primitive.ImmutableCharObjectMapFactory;
import org.eclipse.collections.api.factory.map.primitive.MutableCharObjectMapFactory;
import org.eclipse.collections.impl.map.immutable.primitive.ImmutableCharObjectMapFactoryImpl;
import org.eclipse.collections.impl.map.mutable.primitive.MutableCharObjectMapFactoryImpl;

/**
 * CharObjectMaps is a static utility for creating {@link ImmutableCharObjectMapFactory} and {@link MutableCharObjectMapFactory}.
 * This file was automatically generated from template file primitiveObjectMaps.stg.
 *
 * @since 4.0.
 */
@SuppressWarnings("ConstantNamingConvention")
public final class CharObjectMaps
{
    public static final ImmutableCharObjectMapFactory immutable = ImmutableCharObjectMapFactoryImpl.INSTANCE;
    public static final MutableCharObjectMapFactory mutable = MutableCharObjectMapFactoryImpl.INSTANCE;

    private CharObjectMaps()
    {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }
}
