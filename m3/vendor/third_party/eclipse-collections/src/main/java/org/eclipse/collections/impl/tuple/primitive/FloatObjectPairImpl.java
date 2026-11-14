/*
 * Copyright (c) 2022 Goldman Sachs.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.tuple.primitive;

import org.eclipse.collections.api.tuple.primitive.FloatObjectPair;
import org.eclipse.collections.impl.block.factory.Comparators;

/**
 * This file was automatically generated from template file primitiveObjectPairImpl.stg
 */
public class FloatObjectPairImpl<T> implements FloatObjectPair<T>
{
    private static final long serialVersionUID = 1L;

    private final float one;
    private final T two;

    FloatObjectPairImpl(float newOne, T newTwo)
    {
        this.one = newOne;
        this.two = newTwo;
    }

    @Override
    public float getOne()
    {
        return this.one;
    }

    @Override
    public T getTwo()
    {
        return this.two;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof FloatObjectPair))
        {
            return false;
        }

        FloatObjectPair<?> that = (FloatObjectPair<?>) o;

        return Float.compare(this.one, that.getOne()) == 0
                && Comparators.nullSafeEquals(this.two, that.getTwo());
    }

    @Override
    public int hashCode()
    {
        return 29 * Float.floatToIntBits(this.one) + (this.two == null ? 0 : this.two.hashCode());
    }

    @Override
    public String toString()
    {
        return this.one + ":" + this.two;
    }

    @Override
    public int compareTo(FloatObjectPair<T> that)
    {
        int i = Float.compare(this.one, that.getOne());
        if (i != 0)
        {
            return i;
        }
        return ((Comparable<T>) this.two).compareTo(that.getTwo());
    }
}
