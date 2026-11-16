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

import jdk.m3.collections.eclipse.api.factory.list.primitive.ImmutableCharListFactory;
import jdk.m3.collections.eclipse.api.factory.list.primitive.MutableCharListFactory;
import jdk.m3.collections.eclipse.impl.list.immutable.primitive.ImmutableCharListFactoryImpl;
import jdk.m3.collections.eclipse.impl.list.mutable.primitive.MutableCharListFactoryImpl;

/**
 * CharLists is a static utility for creating {@link ImmutableCharListFactory} and {@link MutableCharListFactory}.
 * This file was automatically generated from template file primitiveLists.stg.
 *
 * @since 3.2.
 */
@SuppressWarnings("ConstantNamingConvention")
public final class CharLists
{
    public static final ImmutableCharListFactory immutable = ImmutableCharListFactoryImpl.INSTANCE;
    public static final MutableCharListFactory mutable = MutableCharListFactoryImpl.INSTANCE;

    private CharLists()
    {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }
}
