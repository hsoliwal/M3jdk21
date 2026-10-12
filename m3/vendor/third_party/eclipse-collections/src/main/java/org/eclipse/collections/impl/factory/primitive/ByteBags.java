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

import org.eclipse.collections.api.factory.bag.primitive.ImmutableByteBagFactory;
import org.eclipse.collections.api.factory.bag.primitive.MutableByteBagFactory;
import org.eclipse.collections.impl.bag.immutable.primitive.ImmutableByteBagFactoryImpl;
import org.eclipse.collections.impl.bag.mutable.primitive.MutableByteBagFactoryImpl;

/**
 * ByteBags is a static utility for creating {@link ImmutableByteBagFactory} and {@link MutableByteBagFactory}.
 * This file was automatically generated from template file primitiveBags.stg.
 *
 * @since 4.0.
 */
@SuppressWarnings("ConstantNamingConvention")
public final class ByteBags
{
    public static final ImmutableByteBagFactory immutable = ImmutableByteBagFactoryImpl.INSTANCE;
    public static final MutableByteBagFactory mutable = MutableByteBagFactoryImpl.INSTANCE;

    private ByteBags()
    {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }
}
