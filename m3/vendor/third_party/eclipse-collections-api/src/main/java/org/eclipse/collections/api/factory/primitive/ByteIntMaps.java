/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.api.factory.primitive;

import org.eclipse.collections.api.factory.ServiceLoaderUtils;
import org.eclipse.collections.api.factory.map.primitive.ImmutableByteIntMapFactory;
import org.eclipse.collections.api.factory.map.primitive.MutableByteIntMapFactory;

/**
 * ByteIntMaps is a static utility for creating {@link ImmutableByteIntMapFactory}
 * and {@link MutableByteIntMapFactory}.
 * This file was automatically generated from template file primitivePrimitiveMapsAPI.stg.
 *
 * @since 11.1.
 */
@SuppressWarnings("ConstantNamingConvention")
public final class ByteIntMaps
{
    public static final ImmutableByteIntMapFactory immutable =
            ServiceLoaderUtils.loadServiceClass(ImmutableByteIntMapFactory.class);
    public static final MutableByteIntMapFactory mutable =
            ServiceLoaderUtils.loadServiceClass(MutableByteIntMapFactory.class);

    private ByteIntMaps()
    {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }
}
