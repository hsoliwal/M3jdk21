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

import org.eclipse.collections.api.tuple.primitive.IntDoublePair;

/**
 * This file was automatically generated from template file primitivePrimitivePairImpl.stg
 */
public class IntDoublePairImpl implements IntDoublePair
{
    private static final long serialVersionUID = 1L;

    private final int one;
    private final double two;

    IntDoublePairImpl(int newOne, double newTwo)
    {
        this.one = newOne;
        this.two = newTwo;
    }

    @Override
    public int getOne()
    {
        return this.one;
    }

    @Override
    public double getTwo()
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
        if (!(o instanceof IntDoublePair))
        {
            return false;
        }

        IntDoublePair that = (IntDoublePair) o;

        return (this.one == that.getOne())
                && (Double.compare(this.two, that.getTwo()) == 0);
    }

    @Override
    public int hashCode()
    {
        return 29 * this.one + (int) (Double.doubleToLongBits(this.two) ^ Double.doubleToLongBits(this.two) >>> 32);
    }

    @Override
    public String toString()
    {
        return this.one + ":" + this.two;
    }

    @Override
    public int compareTo(IntDoublePair that)
    {
        int i = this.one < that.getOne() ? -1 : this.one > that.getOne() ? 1 : 0;
        if (i != 0)
        {
            return i;
        }
        return Double.compare(this.two, that.getTwo());
    }
}
