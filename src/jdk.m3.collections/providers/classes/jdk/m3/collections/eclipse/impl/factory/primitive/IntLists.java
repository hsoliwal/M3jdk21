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

import jdk.m3.collections.eclipse.api.factory.list.primitive.ImmutableIntListFactory;
import jdk.m3.collections.eclipse.api.factory.list.primitive.MutableIntListFactory;
import jdk.m3.collections.eclipse.impl.list.immutable.primitive.ImmutableIntListFactoryImpl;
import jdk.m3.collections.eclipse.impl.list.mutable.primitive.MutableIntListFactoryImpl;

/**
 * IntLists is a static utility for creating {@link ImmutableIntListFactory} and {@link MutableIntListFactory}.
 * This file was automatically generated from template file primitiveLists.stg.
 *
 * @since 3.2.
 */
@SuppressWarnings("ConstantNamingConvention")
public final class IntLists
{
    public static final ImmutableIntListFactory immutable = ImmutableIntListFactoryImpl.INSTANCE;
    public static final MutableIntListFactory mutable = MutableIntListFactoryImpl.INSTANCE;

    private IntLists()
    {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }
}
