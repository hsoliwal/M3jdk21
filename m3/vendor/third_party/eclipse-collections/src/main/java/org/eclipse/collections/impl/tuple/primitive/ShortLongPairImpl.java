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

import org.eclipse.collections.api.tuple.primitive.ShortLongPair;

/**
 * This file was automatically generated from template file primitivePrimitivePairImpl.stg
 */
public class ShortLongPairImpl implements ShortLongPair
{
    private static final long serialVersionUID = 1L;

    private final short one;
    private final long two;

    ShortLongPairImpl(short newOne, long newTwo)
    {
        this.one = newOne;
        this.two = newTwo;
    }

    @Override
    public short getOne()
    {
        return this.one;
    }

    @Override
    public long getTwo()
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
        if (!(o instanceof ShortLongPair))
        {
            return false;
        }

        ShortLongPair that = (ShortLongPair) o;

        return (this.one == that.getOne())
                && (this.two == that.getTwo());
    }

    @Override
    public int hashCode()
    {
        return 29 * (int) this.one + (int) (this.two ^ this.two >>> 32);
    }

    @Override
    public String toString()
    {
        return this.one + ":" + this.two;
    }

    @Override
    public int compareTo(ShortLongPair that)
    {
        int i = this.one - that.getOne();
        if (i != 0)
        {
            return i;
        }
        return this.two < that.getTwo() ? -1 : this.two > that.getTwo() ? 1 : 0;
    }
}
