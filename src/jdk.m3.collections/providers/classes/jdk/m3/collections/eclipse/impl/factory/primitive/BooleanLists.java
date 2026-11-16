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

import jdk.m3.collections.eclipse.api.factory.list.primitive.ImmutableBooleanListFactory;
import jdk.m3.collections.eclipse.api.factory.list.primitive.MutableBooleanListFactory;
import jdk.m3.collections.eclipse.impl.list.immutable.primitive.ImmutableBooleanListFactoryImpl;
import jdk.m3.collections.eclipse.impl.list.mutable.primitive.MutableBooleanListFactoryImpl;

/**
 * BooleanLists is a static utility for creating {@link ImmutableBooleanListFactory} and {@link MutableBooleanListFactory}.
 * This file was automatically generated from template file primitiveLists.stg.
 *
 * @since 3.2.
 */
@SuppressWarnings("ConstantNamingConvention")
public final class BooleanLists
{
    public static final ImmutableBooleanListFactory immutable = ImmutableBooleanListFactoryImpl.INSTANCE;
    public static final MutableBooleanListFactory mutable = MutableBooleanListFactoryImpl.INSTANCE;

    private BooleanLists()
    {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }
}
