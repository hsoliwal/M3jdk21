/*
 * Copyright (c) 2022 Goldman Sachs.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.factory.primitive;

import org.eclipse.collections.api.factory.bag.primitive.ImmutableFloatBagFactory;
import org.eclipse.collections.api.factory.bag.primitive.MutableFloatBagFactory;
import org.eclipse.collections.impl.bag.immutable.primitive.ImmutableFloatBagFactoryImpl;
import org.eclipse.collections.impl.bag.mutable.primitive.MutableFloatBagFactoryImpl;

/**
 * FloatBags is a static utility for creating {@link ImmutableFloatBagFactory} and {@link MutableFloatBagFactory}.
 * This file was automatically generated from template file primitiveBags.stg.
 *
 * @since 4.0.
 */
@SuppressWarnings("ConstantNamingConvention")
public final class FloatBags
{
    public static final ImmutableFloatBagFactory immutable = ImmutableFloatBagFactoryImpl.INSTANCE;
    public static final MutableFloatBagFactory mutable = MutableFloatBagFactoryImpl.INSTANCE;

    private FloatBags()
    {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }
}
