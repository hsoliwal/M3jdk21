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
import jdk.m3.collections.eclipse.api.factory.list.primitive.ImmutableCharListFactory;
import jdk.m3.collections.eclipse.api.factory.list.primitive.MutableCharListFactory;

/**
 * CharLists is a static utility for creating {@link ImmutableCharListFactory} and {@link MutableCharListFactory}.
 * This file was automatically generated from template file primitiveListsAPI.stg.
 *
 * @since 11.1.
 */
@SuppressWarnings("ConstantNamingConvention")
public final class CharLists
{
    public static final ImmutableCharListFactory immutable =
            ServiceLoaderUtils.loadServiceClass(ImmutableCharListFactory.class);
    public static final MutableCharListFactory mutable =
            ServiceLoaderUtils.loadServiceClass(MutableCharListFactory.class);

    private CharLists()
    {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }
}
