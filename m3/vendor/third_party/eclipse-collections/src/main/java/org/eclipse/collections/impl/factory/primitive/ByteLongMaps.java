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

import org.eclipse.collections.api.factory.map.primitive.ImmutableByteLongMapFactory;
import org.eclipse.collections.api.factory.map.primitive.MutableByteLongMapFactory;
import org.eclipse.collections.impl.map.immutable.primitive.ImmutableByteLongMapFactoryImpl;
import org.eclipse.collections.impl.map.mutable.primitive.MutableByteLongMapFactoryImpl;

/**
 * ByteLongMaps is a static utility for creating {@link ImmutableByteLongMapFactory} and {@link MutableByteLongMapFactory}.
 * This file was automatically generated from template file primitivePrimitiveMaps.stg.
 *
 * @since 4.0.
 */
@SuppressWarnings("ConstantNamingConvention")
public final class ByteLongMaps
{
    public static final ImmutableByteLongMapFactory immutable = ImmutableByteLongMapFactoryImpl.INSTANCE;
    public static final MutableByteLongMapFactory mutable = MutableByteLongMapFactoryImpl.INSTANCE;

    private ByteLongMaps()
    {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }
}
