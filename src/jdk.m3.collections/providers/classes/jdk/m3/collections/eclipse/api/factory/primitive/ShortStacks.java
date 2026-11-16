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
import jdk.m3.collections.eclipse.api.factory.stack.primitive.ImmutableShortStackFactory;
import jdk.m3.collections.eclipse.api.factory.stack.primitive.MutableShortStackFactory;

/**
 * ShortStacks is a static utility for creating {@link ImmutableShortStackFactory} and {@link MutableShortStackFactory}.
 * This file was automatically generated from template file primitiveStacksAPI.stg.
 *
 * @since 11.1.
 */
@SuppressWarnings("ConstantNamingConvention")
public final class ShortStacks
{
    public static final ImmutableShortStackFactory immutable =
            ServiceLoaderUtils.loadServiceClass(ImmutableShortStackFactory.class);
    public static final MutableShortStackFactory mutable =
            ServiceLoaderUtils.loadServiceClass(MutableShortStackFactory.class);

    private ShortStacks()
    {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }
}
