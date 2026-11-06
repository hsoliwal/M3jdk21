/*
 * Copyright (c) 2021 Goldman Sachs.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.test.set;

import jdk.m3.collections.eclipse.api.factory.Sets;
import jdk.m3.collections.eclipse.api.set.MutableSet;
import jdk.m3.collections.eclipse.api.set.UnsortedSetIterable;
import jdk.m3.collections.eclipse.api.set.primitive.MutableBooleanSet;
import jdk.m3.collections.eclipse.api.set.primitive.MutableByteSet;
import jdk.m3.collections.eclipse.api.set.primitive.MutableCharSet;
import jdk.m3.collections.eclipse.api.set.primitive.MutableDoubleSet;
import jdk.m3.collections.eclipse.api.set.primitive.MutableFloatSet;
import jdk.m3.collections.eclipse.api.set.primitive.MutableIntSet;
import jdk.m3.collections.eclipse.api.set.primitive.MutableLongSet;
import jdk.m3.collections.eclipse.api.set.primitive.MutableShortSet;
import jdk.m3.collections.eclipse.impl.set.mutable.primitive.BooleanHashSet;
import jdk.m3.collections.eclipse.impl.set.mutable.primitive.ByteHashSet;
import jdk.m3.collections.eclipse.impl.set.mutable.primitive.CharHashSet;
import jdk.m3.collections.eclipse.impl.set.mutable.primitive.DoubleHashSet;
import jdk.m3.collections.eclipse.impl.set.mutable.primitive.FloatHashSet;
import jdk.m3.collections.eclipse.impl.set.mutable.primitive.IntHashSet;
import jdk.m3.collections.eclipse.impl.set.mutable.primitive.LongHashSet;
import jdk.m3.collections.eclipse.impl.set.mutable.primitive.ShortHashSet;
import jdk.m3.collections.eclipse.test.RichIterableTestCase;

// TODO Collected sets should return bags
public interface TransformsToUnsortedSetTrait extends RichIterableTestCase
{
    @Override
    default <T> UnsortedSetIterable<T> getExpectedTransformed(T... elements)
    {
        return Sets.immutable.with(elements);
    }

    @Override
    default <T> MutableSet<T> newMutableForTransform(T... elements)
    {
        return Sets.mutable.with(elements);
    }

    @Override
    default MutableBooleanSet newBooleanForTransform(boolean... elements)
    {
        return new BooleanHashSet(elements);
    }

    @Override
    default MutableByteSet newByteForTransform(byte... elements)
    {
        return new ByteHashSet(elements);
    }

    @Override
    default MutableCharSet newCharForTransform(char... elements)
    {
        return new CharHashSet(elements);
    }

    @Override
    default MutableDoubleSet newDoubleForTransform(double... elements)
    {
        return new DoubleHashSet(elements);
    }

    @Override
    default MutableFloatSet newFloatForTransform(float... elements)
    {
        return new FloatHashSet(elements);
    }

    @Override
    default MutableIntSet newIntForTransform(int... elements)
    {
        return new IntHashSet(elements);
    }

    @Override
    default MutableLongSet newLongForTransform(long... elements)
    {
        return new LongHashSet(elements);
    }

    @Override
    default MutableShortSet newShortForTransform(short... elements)
    {
        return new ShortHashSet(elements);
    }
}
