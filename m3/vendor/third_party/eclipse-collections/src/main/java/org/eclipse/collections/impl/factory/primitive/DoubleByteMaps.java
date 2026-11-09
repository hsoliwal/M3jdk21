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

import org.eclipse.collections.api.factory.map.primitive.ImmutableDoubleByteMapFactory;
import org.eclipse.collections.api.factory.map.primitive.MutableDoubleByteMapFactory;
import org.eclipse.collections.impl.map.immutable.primitive.ImmutableDoubleByteMapFactoryImpl;
import org.eclipse.collections.impl.map.mutable.primitive.MutableDoubleByteMapFactoryImpl;

/**
 * DoubleByteMaps is a static utility for creating {@link ImmutableDoubleByteMapFactory} and {@link MutableDoubleByteMapFactory}.
 * This file was automatically generated from template file primitivePrimitiveMaps.stg.
 *
 * @since 4.0.
 */
@SuppressWarnings("ConstantNamingConvention")
public final class DoubleByteMaps
{
    public static final ImmutableDoubleByteMapFactory immutable = ImmutableDoubleByteMapFactoryImpl.INSTANCE;
    public static final MutableDoubleByteMapFactory mutable = MutableDoubleByteMapFactoryImpl.INSTANCE;

    private DoubleByteMaps()
    {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }
}
