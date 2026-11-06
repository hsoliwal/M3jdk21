/*
 * Copyright (c) 2021 Goldman Sachs.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.test.list;

import jdk.m3.collections.eclipse.api.factory.Lists;
import jdk.m3.collections.eclipse.api.list.ListIterable;
import jdk.m3.collections.eclipse.api.list.MutableList;
import jdk.m3.collections.eclipse.api.list.primitive.MutableBooleanList;
import jdk.m3.collections.eclipse.api.list.primitive.MutableByteList;
import jdk.m3.collections.eclipse.api.list.primitive.MutableCharList;
import jdk.m3.collections.eclipse.api.list.primitive.MutableDoubleList;
import jdk.m3.collections.eclipse.api.list.primitive.MutableFloatList;
import jdk.m3.collections.eclipse.api.list.primitive.MutableIntList;
import jdk.m3.collections.eclipse.api.list.primitive.MutableLongList;
import jdk.m3.collections.eclipse.api.list.primitive.MutableShortList;
import jdk.m3.collections.eclipse.impl.list.mutable.primitive.BooleanArrayList;
import jdk.m3.collections.eclipse.impl.list.mutable.primitive.ByteArrayList;
import jdk.m3.collections.eclipse.impl.list.mutable.primitive.CharArrayList;
import jdk.m3.collections.eclipse.impl.list.mutable.primitive.DoubleArrayList;
import jdk.m3.collections.eclipse.impl.list.mutable.primitive.FloatArrayList;
import jdk.m3.collections.eclipse.impl.list.mutable.primitive.IntArrayList;
import jdk.m3.collections.eclipse.impl.list.mutable.primitive.LongArrayList;
import jdk.m3.collections.eclipse.impl.list.mutable.primitive.ShortArrayList;
import jdk.m3.collections.eclipse.test.RichIterableTestCase;

public interface TransformsToListTrait extends RichIterableTestCase
{
    @Override
    default <T> ListIterable<T> getExpectedTransformed(T... elements)
    {
        return Lists.immutable.with(elements);
    }

    @Override
    default <T> MutableList<T> newMutableForTransform(T... elements)
    {
        return Lists.mutable.with(elements);
    }

    @Override
    default MutableBooleanList newBooleanForTransform(boolean... elements)
    {
        return new BooleanArrayList(elements);
    }

    @Override
    default MutableByteList newByteForTransform(byte... elements)
    {
        return new ByteArrayList(elements);
    }

    @Override
    default MutableCharList newCharForTransform(char... elements)
    {
        return new CharArrayList(elements);
    }

    @Override
    default MutableDoubleList newDoubleForTransform(double... elements)
    {
        return new DoubleArrayList(elements);
    }

    @Override
    default MutableFloatList newFloatForTransform(float... elements)
    {
        return new FloatArrayList(elements);
    }

    @Override
    default MutableIntList newIntForTransform(int... elements)
    {
        return new IntArrayList(elements);
    }

    @Override
    default MutableLongList newLongForTransform(long... elements)
    {
        return new LongArrayList(elements);
    }

    @Override
    default MutableShortList newShortForTransform(short... elements)
    {
        return new ShortArrayList(elements);
    }
}
