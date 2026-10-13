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
import org.eclipse.collections.api.factory.map.primitive.ImmutableDoubleByteMapFactory;
import org.eclipse.collections.api.factory.map.primitive.MutableDoubleByteMapFactory;

/**
 * DoubleByteMaps is a static utility for creating {@link ImmutableDoubleByteMapFactory}
 * and {@link MutableDoubleByteMapFactory}.
 * This file was automatically generated from template file primitivePrimitiveMapsAPI.stg.
 *
 * @since 11.1.
 */
@SuppressWarnings("ConstantNamingConvention")
public final class DoubleByteMaps
{
    public static final ImmutableDoubleByteMapFactory immutable =
            ServiceLoaderUtils.loadServiceClass(ImmutableDoubleByteMapFactory.class);
    public static final MutableDoubleByteMapFactory mutable =
            ServiceLoaderUtils.loadServiceClass(MutableDoubleByteMapFactory.class);

    private DoubleByteMaps()
    {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }
}
