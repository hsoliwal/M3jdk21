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

import org.eclipse.collections.api.factory.list.primitive.ImmutableIntListFactory;
import org.eclipse.collections.api.factory.list.primitive.MutableIntListFactory;
import org.eclipse.collections.impl.list.immutable.primitive.ImmutableIntListFactoryImpl;
import org.eclipse.collections.impl.list.mutable.primitive.MutableIntListFactoryImpl;

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
