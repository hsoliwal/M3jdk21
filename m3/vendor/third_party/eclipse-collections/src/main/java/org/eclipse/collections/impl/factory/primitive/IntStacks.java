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

import org.eclipse.collections.api.factory.stack.primitive.ImmutableIntStackFactory;
import org.eclipse.collections.api.factory.stack.primitive.MutableIntStackFactory;
import org.eclipse.collections.impl.stack.immutable.primitive.ImmutableIntStackFactoryImpl;
import org.eclipse.collections.impl.stack.mutable.primitive.MutableIntStackFactoryImpl;

/**
 * IntStacks is a static utility for creating {@link ImmutableIntStackFactory} and {@link MutableIntStackFactory}.
 * This file was automatically generated from template file primitiveStacks.stg.
 *
 * @since 4.0.
 */
@SuppressWarnings("ConstantNamingConvention")
public final class IntStacks
{
    public static final ImmutableIntStackFactory immutable = ImmutableIntStackFactoryImpl.INSTANCE;
    public static final MutableIntStackFactory mutable = MutableIntStackFactoryImpl.INSTANCE;

    private IntStacks()
    {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }
}
