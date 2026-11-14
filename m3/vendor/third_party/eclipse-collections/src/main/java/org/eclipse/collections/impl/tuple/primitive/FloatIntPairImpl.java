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

import org.eclipse.collections.api.tuple.primitive.FloatIntPair;

/**
 * This file was automatically generated from template file primitivePrimitivePairImpl.stg
 */
public class FloatIntPairImpl implements FloatIntPair
{
    private static final long serialVersionUID = 1L;

    private final float one;
    private final int two;

    FloatIntPairImpl(float newOne, int newTwo)
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
    public int getTwo()
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
        if (!(o instanceof FloatIntPair))
        {
            return false;
        }

        FloatIntPair that = (FloatIntPair) o;

        return (Float.compare(this.one, that.getOne()) == 0)
                && (this.two == that.getTwo());
    }

    @Override
    public int hashCode()
    {
        return 29 * Float.floatToIntBits(this.one) + this.two;
    }

    @Override
    public String toString()
    {
        return this.one + ":" + this.two;
    }

    @Override
    public int compareTo(FloatIntPair that)
    {
        int i = Float.compare(this.one, that.getOne());
        if (i != 0)
        {
            return i;
        }
        return this.two < that.getTwo() ? -1 : this.two > that.getTwo() ? 1 : 0;
    }
}
