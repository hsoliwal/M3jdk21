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

import org.eclipse.collections.api.factory.stack.primitive.ImmutableBooleanStackFactory;
import org.eclipse.collections.api.factory.stack.primitive.MutableBooleanStackFactory;
import org.eclipse.collections.impl.stack.immutable.primitive.ImmutableBooleanStackFactoryImpl;
import org.eclipse.collections.impl.stack.mutable.primitive.MutableBooleanStackFactoryImpl;

/**
 * BooleanStacks is a static utility for creating {@link ImmutableBooleanStackFactory} and {@link MutableBooleanStackFactory}.
 * This file was automatically generated from template file primitiveStacks.stg.
 *
 * @since 4.0.
 */
@SuppressWarnings("ConstantNamingConvention")
public final class BooleanStacks
{
    public static final ImmutableBooleanStackFactory immutable = ImmutableBooleanStackFactoryImpl.INSTANCE;
    public static final MutableBooleanStackFactory mutable = MutableBooleanStackFactoryImpl.INSTANCE;

    private BooleanStacks()
    {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }
}
