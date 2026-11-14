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
import org.eclipse.collections.api.factory.list.primitive.ImmutableIntListFactory;
import org.eclipse.collections.api.factory.list.primitive.MutableIntListFactory;

/**
 * IntLists is a static utility for creating {@link ImmutableIntListFactory} and {@link MutableIntListFactory}.
 * This file was automatically generated from template file primitiveListsAPI.stg.
 *
 * @since 11.1.
 */
@SuppressWarnings("ConstantNamingConvention")
public final class IntLists
{
    public static final ImmutableIntListFactory immutable =
            ServiceLoaderUtils.loadServiceClass(ImmutableIntListFactory.class);
    public static final MutableIntListFactory mutable =
            ServiceLoaderUtils.loadServiceClass(MutableIntListFactory.class);

    private IntLists()
    {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }
}
