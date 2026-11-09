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

import org.eclipse.collections.api.tuple.primitive.ObjectBooleanPair;
import org.eclipse.collections.impl.block.factory.Comparators;

/**
 * This file was automatically generated from template file objectPrimitivePairImpl.stg
 */
public class ObjectBooleanPairImpl<T> implements ObjectBooleanPair<T>
{
    private static final long serialVersionUID = 1L;

    private final T one;
    private final boolean two;

    ObjectBooleanPairImpl(T newOne, boolean newTwo)
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
    public boolean getTwo()
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
        if (!(o instanceof ObjectBooleanPair))
        {
            return false;
        }

        ObjectBooleanPair<?> that = (ObjectBooleanPair<?>) o;

        return Comparators.nullSafeEquals(this.one, that.getOne())
                && this.two == that.getTwo();
    }

    @Override
    public int hashCode()
    {
        int result = this.one == null ? 0 : this.one.hashCode();
        result = 29 * result + (this.two ? 1231 : 1237);
        return result;
    }

    @Override
    public String toString()
    {
        return this.one + ":" + this.two;
    }

    @Override
    public int compareTo(ObjectBooleanPair<T> that)
    {
        int i = ((Comparable<T>) this.one).compareTo(that.getOne());
        if (i != 0)
        {
            return i;
        }
        return this.two == that.getTwo() ? 0 : this.two ? 1 : -1;
    }
}
