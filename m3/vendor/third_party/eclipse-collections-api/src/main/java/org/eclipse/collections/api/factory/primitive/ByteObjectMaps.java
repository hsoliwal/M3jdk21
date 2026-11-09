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
import org.eclipse.collections.api.factory.map.primitive.ImmutableByteObjectMapFactory;
import org.eclipse.collections.api.factory.map.primitive.MutableByteObjectMapFactory;

/**
 * ByteObjectMaps is a static utility for creating {@link ImmutableByteObjectMapFactory} and {@link MutableByteObjectMapFactory}.
 * This file was automatically generated from template file primitiveObjectMapsAPI.stg.
 *
 * @since 11.1.
 */
@SuppressWarnings("ConstantNamingConvention")
public final class ByteObjectMaps
{
    public static final ImmutableByteObjectMapFactory immutable =
            ServiceLoaderUtils.loadServiceClass(ImmutableByteObjectMapFactory.class);
    public static final MutableByteObjectMapFactory mutable =
            ServiceLoaderUtils.loadServiceClass(MutableByteObjectMapFactory.class);

    private ByteObjectMaps()
    {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }
}
