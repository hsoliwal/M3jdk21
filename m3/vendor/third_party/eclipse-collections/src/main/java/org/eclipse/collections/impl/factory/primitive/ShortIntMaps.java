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

import org.eclipse.collections.api.factory.map.primitive.ImmutableShortIntMapFactory;
import org.eclipse.collections.api.factory.map.primitive.MutableShortIntMapFactory;
import org.eclipse.collections.impl.map.immutable.primitive.ImmutableShortIntMapFactoryImpl;
import org.eclipse.collections.impl.map.mutable.primitive.MutableShortIntMapFactoryImpl;

/**
 * ShortIntMaps is a static utility for creating {@link ImmutableShortIntMapFactory} and {@link MutableShortIntMapFactory}.
 * This file was automatically generated from template file primitivePrimitiveMaps.stg.
 *
 * @since 4.0.
 */
@SuppressWarnings("ConstantNamingConvention")
public final class ShortIntMaps
{
    public static final ImmutableShortIntMapFactory immutable = ImmutableShortIntMapFactoryImpl.INSTANCE;
    public static final MutableShortIntMapFactory mutable = MutableShortIntMapFactoryImpl.INSTANCE;

    private ShortIntMaps()
    {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }
}
