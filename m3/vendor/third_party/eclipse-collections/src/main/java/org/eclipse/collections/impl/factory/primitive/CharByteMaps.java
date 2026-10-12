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

import org.eclipse.collections.api.factory.map.primitive.ImmutableCharByteMapFactory;
import org.eclipse.collections.api.factory.map.primitive.MutableCharByteMapFactory;
import org.eclipse.collections.impl.map.immutable.primitive.ImmutableCharByteMapFactoryImpl;
import org.eclipse.collections.impl.map.mutable.primitive.MutableCharByteMapFactoryImpl;

/**
 * CharByteMaps is a static utility for creating {@link ImmutableCharByteMapFactory} and {@link MutableCharByteMapFactory}.
 * This file was automatically generated from template file primitivePrimitiveMaps.stg.
 *
 * @since 4.0.
 */
@SuppressWarnings("ConstantNamingConvention")
public final class CharByteMaps
{
    public static final ImmutableCharByteMapFactory immutable = ImmutableCharByteMapFactoryImpl.INSTANCE;
    public static final MutableCharByteMapFactory mutable = MutableCharByteMapFactoryImpl.INSTANCE;

    private CharByteMaps()
    {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }
}
