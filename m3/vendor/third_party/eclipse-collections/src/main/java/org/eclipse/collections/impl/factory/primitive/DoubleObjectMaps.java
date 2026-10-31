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

import org.eclipse.collections.api.factory.map.primitive.ImmutableDoubleObjectMapFactory;
import org.eclipse.collections.api.factory.map.primitive.MutableDoubleObjectMapFactory;
import org.eclipse.collections.impl.map.immutable.primitive.ImmutableDoubleObjectMapFactoryImpl;
import org.eclipse.collections.impl.map.mutable.primitive.MutableDoubleObjectMapFactoryImpl;

/**
 * DoubleObjectMaps is a static utility for creating {@link ImmutableDoubleObjectMapFactory} and {@link MutableDoubleObjectMapFactory}.
 * This file was automatically generated from template file primitiveObjectMaps.stg.
 *
 * @since 4.0.
 */
@SuppressWarnings("ConstantNamingConvention")
public final class DoubleObjectMaps
{
    public static final ImmutableDoubleObjectMapFactory immutable = ImmutableDoubleObjectMapFactoryImpl.INSTANCE;
    public static final MutableDoubleObjectMapFactory mutable = MutableDoubleObjectMapFactoryImpl.INSTANCE;

    private DoubleObjectMaps()
    {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }
}
