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
import org.eclipse.collections.api.factory.bag.primitive.ImmutableLongBagFactory;
import org.eclipse.collections.api.factory.bag.primitive.MutableLongBagFactory;

/**
 * LongBags is a static utility for creating {@link ImmutableLongBagFactory} and {@link MutableLongBagFactory}.
 * This file was automatically generated from template file primitiveBagsAPI.stg.
 *
 * @since 11.1.
 */
@SuppressWarnings("ConstantNamingConvention")
public final class LongBags
{
    public static final ImmutableLongBagFactory immutable =
            ServiceLoaderUtils.loadServiceClass(ImmutableLongBagFactory.class);
    public static final MutableLongBagFactory mutable =
            ServiceLoaderUtils.loadServiceClass(MutableLongBagFactory.class);

    private LongBags()
    {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }
}
