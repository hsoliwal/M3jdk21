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

import jdk.m3.collections.eclipse.api.factory.stack.primitive.ImmutableByteStackFactory;
import jdk.m3.collections.eclipse.api.factory.stack.primitive.MutableByteStackFactory;
import jdk.m3.collections.eclipse.impl.stack.immutable.primitive.ImmutableByteStackFactoryImpl;
import jdk.m3.collections.eclipse.impl.stack.mutable.primitive.MutableByteStackFactoryImpl;

/**
 * ByteStacks is a static utility for creating {@link ImmutableByteStackFactory} and {@link MutableByteStackFactory}.
 * This file was automatically generated from template file primitiveStacks.stg.
 *
 * @since 4.0.
 */
@SuppressWarnings("ConstantNamingConvention")
public final class ByteStacks
{
    public static final ImmutableByteStackFactory immutable = ImmutableByteStackFactoryImpl.INSTANCE;
    public static final MutableByteStackFactory mutable = MutableByteStackFactoryImpl.INSTANCE;

    private ByteStacks()
    {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }
}
