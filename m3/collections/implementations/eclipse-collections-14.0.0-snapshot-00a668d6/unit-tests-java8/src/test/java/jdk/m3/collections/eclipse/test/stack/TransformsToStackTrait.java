/*
 * Copyright (c) 2021 Goldman Sachs.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.test.stack;

import jdk.m3.collections.eclipse.api.factory.Lists;
import jdk.m3.collections.eclipse.api.factory.Stacks;
import jdk.m3.collections.eclipse.api.list.MutableList;
import jdk.m3.collections.eclipse.api.list.primitive.MutableBooleanList;
import jdk.m3.collections.eclipse.api.list.primitive.MutableByteList;
import jdk.m3.collections.eclipse.api.list.primitive.MutableCharList;
import jdk.m3.collections.eclipse.api.list.primitive.MutableDoubleList;
import jdk.m3.collections.eclipse.api.list.primitive.MutableFloatList;
import jdk.m3.collections.eclipse.api.list.primitive.MutableIntList;
import jdk.m3.collections.eclipse.api.list.primitive.MutableLongList;
import jdk.m3.collections.eclipse.api.list.primitive.MutableShortList;
import jdk.m3.collections.eclipse.api.stack.StackIterable;
import jdk.m3.collections.eclipse.api.stack.primitive.BooleanStack;
import jdk.m3.collections.eclipse.api.stack.primitive.ByteStack;
import jdk.m3.collections.eclipse.api.stack.primitive.CharStack;
import jdk.m3.collections.eclipse.api.stack.primitive.DoubleStack;
import jdk.m3.collections.eclipse.api.stack.primitive.FloatStack;
import jdk.m3.collections.eclipse.api.stack.primitive.IntStack;
import jdk.m3.collections.eclipse.api.stack.primitive.LongStack;
import jdk.m3.collections.eclipse.api.stack.primitive.ShortStack;
import jdk.m3.collections.eclipse.impl.factory.primitive.BooleanLists;
import jdk.m3.collections.eclipse.impl.factory.primitive.BooleanStacks;
import jdk.m3.collections.eclipse.impl.factory.primitive.ByteLists;
import jdk.m3.collections.eclipse.impl.factory.primitive.ByteStacks;
import jdk.m3.collections.eclipse.impl.factory.primitive.CharLists;
import jdk.m3.collections.eclipse.impl.factory.primitive.CharStacks;
import jdk.m3.collections.eclipse.impl.factory.primitive.DoubleLists;
import jdk.m3.collections.eclipse.impl.factory.primitive.DoubleStacks;
import jdk.m3.collections.eclipse.impl.factory.primitive.FloatLists;
import jdk.m3.collections.eclipse.impl.factory.primitive.FloatStacks;
import jdk.m3.collections.eclipse.impl.factory.primitive.IntLists;
import jdk.m3.collections.eclipse.impl.factory.primitive.IntStacks;
import jdk.m3.collections.eclipse.impl.factory.primitive.LongLists;
import jdk.m3.collections.eclipse.impl.factory.primitive.LongStacks;
import jdk.m3.collections.eclipse.impl.factory.primitive.ShortLists;
import jdk.m3.collections.eclipse.impl.factory.primitive.ShortStacks;
import jdk.m3.collections.eclipse.impl.list.mutable.primitive.BooleanArrayList;
import jdk.m3.collections.eclipse.impl.list.mutable.primitive.ByteArrayList;
import jdk.m3.collections.eclipse.impl.list.mutable.primitive.CharArrayList;
import jdk.m3.collections.eclipse.impl.list.mutable.primitive.DoubleArrayList;
import jdk.m3.collections.eclipse.impl.list.mutable.primitive.FloatArrayList;
import jdk.m3.collections.eclipse.impl.list.mutable.primitive.IntArrayList;
import jdk.m3.collections.eclipse.impl.list.mutable.primitive.LongArrayList;
import jdk.m3.collections.eclipse.impl.list.mutable.primitive.ShortArrayList;
import jdk.m3.collections.eclipse.test.RichIterableTestCase;

public interface TransformsToStackTrait extends RichIterableTestCase
{
    @Override
    default <T> StackIterable<T> getExpectedTransformed(T... elements)
    {
        return Stacks.immutable.withReversed(elements);
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

    @Override
    default BooleanStack getExpectedBoolean(boolean... elements)
    {
        return BooleanStacks.immutable.withAllReversed(BooleanLists.immutable.with(elements));
    }

    @Override
    default ByteStack getExpectedByte(byte... elements)
    {
        return ByteStacks.immutable.withAllReversed(ByteLists.immutable.with(elements));
    }

    @Override
    default CharStack getExpectedChar(char... elements)
    {
        return CharStacks.immutable.withAllReversed(CharLists.immutable.with(elements));
    }

    @Override
    default DoubleStack getExpectedDouble(double... elements)
    {
        return DoubleStacks.immutable.withAllReversed(DoubleLists.immutable.with(elements));
    }

    @Override
    default FloatStack getExpectedFloat(float... elements)
    {
        return FloatStacks.immutable.withAllReversed(FloatLists.immutable.with(elements));
    }

    @Override
    default IntStack getExpectedInt(int... elements)
    {
        return IntStacks.immutable.withAllReversed(IntLists.immutable.with(elements));
    }

    @Override
    default LongStack getExpectedLong(long... elements)
    {
        return LongStacks.immutable.withAllReversed(LongLists.immutable.with(elements));
    }

    @Override
    default ShortStack getExpectedShort(short... elements)
    {
        return ShortStacks.immutable.withAllReversed(ShortLists.immutable.with(elements));
    }
}
