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

import org.eclipse.collections.api.tuple.primitive.ByteBytePair;

/**
 * This file was automatically generated from template file primitivePrimitivePairImpl.stg
 */
public class ByteBytePairImpl implements ByteBytePair
{
    private static final long serialVersionUID = 1L;

    private final byte one;
    private final byte two;

    ByteBytePairImpl(byte newOne, byte newTwo)
    {
        this.one = newOne;
        this.two = newTwo;
    }

    @Override
    public byte getOne()
    {
        return this.one;
    }

    @Override
    public byte getTwo()
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
        if (!(o instanceof ByteBytePair))
        {
            return false;
        }

        ByteBytePair that = (ByteBytePair) o;

        return (this.one == that.getOne())
                && (this.two == that.getTwo());
    }

    @Override
    public int hashCode()
    {
        return 29 * (int) this.one + (int) this.two;
    }

    @Override
    public String toString()
    {
        return this.one + ":" + this.two;
    }

    @Override
    public int compareTo(ByteBytePair that)
    {
        int i = this.one - that.getOne();
        if (i != 0)
        {
            return i;
        }
        return this.two - that.getTwo();
    }
}
