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

import org.eclipse.collections.api.tuple.primitive.BooleanDoublePair;

/**
 * This file was automatically generated from template file primitivePrimitivePairImpl.stg
 */
public class BooleanDoublePairImpl implements BooleanDoublePair
{
    private static final long serialVersionUID = 1L;

    private final boolean one;
    private final double two;

    BooleanDoublePairImpl(boolean newOne, double newTwo)
    {
        this.one = newOne;
        this.two = newTwo;
    }

    @Override
    public boolean getOne()
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
        if (!(o instanceof BooleanDoublePair))
        {
            return false;
        }

        BooleanDoublePair that = (BooleanDoublePair) o;

        return (this.one == that.getOne())
                && (Double.compare(this.two, that.getTwo()) == 0);
    }

    @Override
    public int hashCode()
    {
        return 29 * (this.one ? 1231 : 1237) + (int) (Double.doubleToLongBits(this.two) ^ Double.doubleToLongBits(this.two) >>> 32);
    }

    @Override
    public String toString()
    {
        return this.one + ":" + this.two;
    }

    @Override
    public int compareTo(BooleanDoublePair that)
    {
        int i = this.one == that.getOne() ? 0 : this.one ? 1 : -1;
        if (i != 0)
        {
            return i;
        }
        return Double.compare(this.two, that.getTwo());
    }
}
