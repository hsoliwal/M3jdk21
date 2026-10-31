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
import org.eclipse.collections.api.factory.set.primitive.ImmutableShortSetFactory;
import org.eclipse.collections.api.factory.set.primitive.MutableShortSetFactory;

/**
 * ShortSets is a static utility for creating {@link ImmutableShortSetFactory} and {@link MutableShortSetFactory}.
 * This file was automatically generated from template file primitiveSetsAPI.stg.
 *
 * @since 11.1.
 */
@SuppressWarnings("ConstantNamingConvention")
public final class ShortSets
{
    public static final ImmutableShortSetFactory immutable =
            ServiceLoaderUtils.loadServiceClass(ImmutableShortSetFactory.class);
    public static final MutableShortSetFactory mutable =
            ServiceLoaderUtils.loadServiceClass(MutableShortSetFactory.class);

    private ShortSets()
    {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }
}
