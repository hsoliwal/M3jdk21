/*
 * Copyright (c) 2026 Goldman Sachs.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.impl.factory.primitive;

import jdk.m3.collections.eclipse.api.factory.bag.primitive.ImmutableShortBagFactory;
import jdk.m3.collections.eclipse.api.factory.bag.primitive.MutableShortBagFactory;
import jdk.m3.collections.eclipse.impl.bag.immutable.primitive.ImmutableShortBagFactoryImpl;
import jdk.m3.collections.eclipse.impl.bag.mutable.primitive.MutableShortBagFactoryImpl;

/**
 * ShortBags is a static utility for creating {@link ImmutableShortBagFactory} and {@link MutableShortBagFactory}.
 * This file was automatically generated from template file primitiveBags.stg.
 *
 * @since 4.0.
 */
@SuppressWarnings("ConstantNamingConvention")
public final class ShortBags
{
    public static final ImmutableShortBagFactory immutable = ImmutableShortBagFactoryImpl.INSTANCE;
    public static final MutableShortBagFactory mutable = MutableShortBagFactoryImpl.INSTANCE;

    private ShortBags()
    {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }
}
