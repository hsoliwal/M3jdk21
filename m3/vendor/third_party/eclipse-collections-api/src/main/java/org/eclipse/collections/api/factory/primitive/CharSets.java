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
import org.eclipse.collections.api.factory.set.primitive.ImmutableCharSetFactory;
import org.eclipse.collections.api.factory.set.primitive.MutableCharSetFactory;

/**
 * CharSets is a static utility for creating {@link ImmutableCharSetFactory} and {@link MutableCharSetFactory}.
 * This file was automatically generated from template file primitiveSetsAPI.stg.
 *
 * @since 11.1.
 */
@SuppressWarnings("ConstantNamingConvention")
public final class CharSets
{
    public static final ImmutableCharSetFactory immutable =
            ServiceLoaderUtils.loadServiceClass(ImmutableCharSetFactory.class);
    public static final MutableCharSetFactory mutable =
            ServiceLoaderUtils.loadServiceClass(MutableCharSetFactory.class);

    private CharSets()
    {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }
}
