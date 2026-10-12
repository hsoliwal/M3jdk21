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

import org.eclipse.collections.api.factory.list.primitive.ImmutableBooleanListFactory;
import org.eclipse.collections.api.factory.list.primitive.MutableBooleanListFactory;
import org.eclipse.collections.impl.list.immutable.primitive.ImmutableBooleanListFactoryImpl;
import org.eclipse.collections.impl.list.mutable.primitive.MutableBooleanListFactoryImpl;

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
