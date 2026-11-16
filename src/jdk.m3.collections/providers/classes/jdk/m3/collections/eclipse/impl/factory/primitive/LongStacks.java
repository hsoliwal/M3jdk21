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

import jdk.m3.collections.eclipse.api.factory.stack.primitive.ImmutableLongStackFactory;
import jdk.m3.collections.eclipse.api.factory.stack.primitive.MutableLongStackFactory;
import jdk.m3.collections.eclipse.impl.stack.immutable.primitive.ImmutableLongStackFactoryImpl;
import jdk.m3.collections.eclipse.impl.stack.mutable.primitive.MutableLongStackFactoryImpl;

/**
 * LongStacks is a static utility for creating {@link ImmutableLongStackFactory} and {@link MutableLongStackFactory}.
 * This file was automatically generated from template file primitiveStacks.stg.
 *
 * @since 4.0.
 */
@SuppressWarnings("ConstantNamingConvention")
public final class LongStacks
{
    public static final ImmutableLongStackFactory immutable = ImmutableLongStackFactoryImpl.INSTANCE;
    public static final MutableLongStackFactory mutable = MutableLongStackFactoryImpl.INSTANCE;

    private LongStacks()
    {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }
}
