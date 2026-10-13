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

import org.eclipse.collections.api.tuple.primitive.ObjectDoublePair;
import org.eclipse.collections.impl.block.factory.Comparators;

/**
 * This file was automatically generated from template file objectPrimitivePairImpl.stg
 */
public class ObjectDoublePairImpl<T> implements ObjectDoublePair<T>
{
    private static final long serialVersionUID = 1L;

    private final T one;
    private final double two;

    ObjectDoublePairImpl(T newOne, double newTwo)
    {
        this.one = newOne;
        this.two = newTwo;
    }

    @Override
    public T getOne()
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
        if (!(o instanceof ObjectDoublePair))
        {
            return false;
        }

        ObjectDoublePair<?> that = (ObjectDoublePair<?>) o;

        return Comparators.nullSafeEquals(this.one, that.getOne())
                && Double.compare(this.two, that.getTwo()) == 0;
    }

    @Override
    public int hashCode()
    {
        int result = this.one == null ? 0 : this.one.hashCode();
        result = 29 * result + (int) (Double.doubleToLongBits(this.two) ^ Double.doubleToLongBits(this.two) >>> 32);
        return result;
    }

    @Override
    public String toString()
    {
        return this.one + ":" + this.two;
    }

    @Override
    public int compareTo(ObjectDoublePair<T> that)
    {
        int i = ((Comparable<T>) this.one).compareTo(that.getOne());
        if (i != 0)
        {
            return i;
        }
        return Double.compare(this.two, that.getTwo());
    }
}
