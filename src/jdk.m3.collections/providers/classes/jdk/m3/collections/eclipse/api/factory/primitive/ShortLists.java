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
import jdk.m3.collections.eclipse.api.factory.list.primitive.ImmutableShortListFactory;
import jdk.m3.collections.eclipse.api.factory.list.primitive.MutableShortListFactory;

/**
 * ShortLists is a static utility for creating {@link ImmutableShortListFactory} and {@link MutableShortListFactory}.
 * This file was automatically generated from template file primitiveListsAPI.stg.
 *
 * @since 11.1.
 */
@SuppressWarnings("ConstantNamingConvention")
public final class ShortLists
{
    public static final ImmutableShortListFactory immutable =
            ServiceLoaderUtils.loadServiceClass(ImmutableShortListFactory.class);
    public static final MutableShortListFactory mutable =
            ServiceLoaderUtils.loadServiceClass(MutableShortListFactory.class);

    private ShortLists()
    {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }
}
