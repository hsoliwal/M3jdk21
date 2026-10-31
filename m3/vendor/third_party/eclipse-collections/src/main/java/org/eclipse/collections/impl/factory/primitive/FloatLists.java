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

import org.eclipse.collections.api.factory.list.primitive.ImmutableFloatListFactory;
import org.eclipse.collections.api.factory.list.primitive.MutableFloatListFactory;
import org.eclipse.collections.impl.list.immutable.primitive.ImmutableFloatListFactoryImpl;
import org.eclipse.collections.impl.list.mutable.primitive.MutableFloatListFactoryImpl;

/**
 * FloatLists is a static utility for creating {@link ImmutableFloatListFactory} and {@link MutableFloatListFactory}.
 * This file was automatically generated from template file primitiveLists.stg.
 *
 * @since 3.2.
 */
@SuppressWarnings("ConstantNamingConvention")
public final class FloatLists
{
    public static final ImmutableFloatListFactory immutable = ImmutableFloatListFactoryImpl.INSTANCE;
    public static final MutableFloatListFactory mutable = MutableFloatListFactoryImpl.INSTANCE;

    private FloatLists()
    {
        throw new AssertionError("Suppress default constructor for noninstantiability");
    }
}
