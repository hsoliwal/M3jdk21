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

import org.eclipse.collections.api.factory.stack.primitive.ImmutableCharStackFactory;
import org.eclipse.collections.api.factory.stack.primitive.MutableCharStackFactory;
import org.eclipse.collections.impl.stack.immutable.primitive.ImmutableCharStackFactoryImpl;
import org.eclipse.collections.impl.stack.mutable.primitive.MutableCharStackFactoryImpl;

/**
 * CharStacks is a static utility for creating {@link ImmutableCharStackFactory} and {@link MutableCharStackFactory}.
 * This file was automatically generated from template file primitiveStacks.stg.
 *
 * @since 4.0.
 */
@SuppressWarnings("ConstantNamingConvention")
public final class CharStacks
{
    public static final ImmutableCharStackFactory immutable = ImmutableCharStackFactoryImpl.INSTANCE;
    public static final MutableCharStackFactory mutable = MutableCharStackFactoryImpl.INSTANCE;

    private CharStacks()
    {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }
}
