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

import org.eclipse.collections.api.factory.bag.primitive.ImmutableCharBagFactory;
import org.eclipse.collections.api.factory.bag.primitive.MutableCharBagFactory;
import org.eclipse.collections.impl.bag.immutable.primitive.ImmutableCharBagFactoryImpl;
import org.eclipse.collections.impl.bag.mutable.primitive.MutableCharBagFactoryImpl;

/**
 * CharBags is a static utility for creating {@link ImmutableCharBagFactory} and {@link MutableCharBagFactory}.
 * This file was automatically generated from template file primitiveBags.stg.
 *
 * @since 4.0.
 */
@SuppressWarnings("ConstantNamingConvention")
public final class CharBags
{
    public static final ImmutableCharBagFactory immutable = ImmutableCharBagFactoryImpl.INSTANCE;
    public static final MutableCharBagFactory mutable = MutableCharBagFactoryImpl.INSTANCE;

    private CharBags()
    {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }
}
