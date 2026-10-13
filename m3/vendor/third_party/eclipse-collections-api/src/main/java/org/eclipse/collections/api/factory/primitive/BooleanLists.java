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
import org.eclipse.collections.api.factory.list.primitive.ImmutableBooleanListFactory;
import org.eclipse.collections.api.factory.list.primitive.MutableBooleanListFactory;

/**
 * BooleanLists is a static utility for creating {@link ImmutableBooleanListFactory} and {@link MutableBooleanListFactory}.
 * This file was automatically generated from template file primitiveListsAPI.stg.
 *
 * @since 11.1.
 */
@SuppressWarnings("ConstantNamingConvention")
public final class BooleanLists
{
    public static final ImmutableBooleanListFactory immutable =
            ServiceLoaderUtils.loadServiceClass(ImmutableBooleanListFactory.class);
    public static final MutableBooleanListFactory mutable =
            ServiceLoaderUtils.loadServiceClass(MutableBooleanListFactory.class);

    private BooleanLists()
    {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }
}
