/*
 * Copyright (c) 2026 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.api.factory.primitive;

import jdk.m3.collections.eclipse.api.factory.ServiceLoaderUtils;
import jdk.m3.collections.eclipse.api.factory.map.primitive.ImmutableLongLongMapFactory;
import jdk.m3.collections.eclipse.api.factory.map.primitive.MutableLongLongMapFactory;

/**
 * LongLongMaps is a static utility for creating {@link ImmutableLongLongMapFactory}
 * and {@link MutableLongLongMapFactory}.
 * This file was automatically generated from template file primitivePrimitiveMapsAPI.stg.
 *
 * @since 11.1.
 */
@SuppressWarnings("ConstantNamingConvention")
public final class LongLongMaps
{
    public static final ImmutableLongLongMapFactory immutable =
            ServiceLoaderUtils.loadServiceClass(ImmutableLongLongMapFactory.class);
    public static final MutableLongLongMapFactory mutable =
            ServiceLoaderUtils.loadServiceClass(MutableLongLongMapFactory.class);

    private LongLongMaps()
    {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }
}
