/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.list.mutable.primitive;

import org.eclipse.collections.api.ByteIterable;
import org.eclipse.collections.api.factory.list.primitive.MutableByteListFactory;
import org.eclipse.collections.api.list.primitive.MutableByteList;
import org.eclipse.collections.impl.factory.primitive.ByteLists;
import org.eclipse.collections.impl.utility.Iterate;

/**
 * MutableByteListFactoryImpl is a factory implementation which creates instances of type {@link MutableByteList}.
 * This file was automatically generated from template file mutablePrimitiveListFactoryImpl.stg.
 *
 * @since 6.0.
 */
public class MutableByteListFactoryImpl implements MutableByteListFactory
{
    public static final MutableByteListFactory INSTANCE = new MutableByteListFactoryImpl();

    @Override
    public MutableByteList empty()
    {
        return new ByteArrayList();
    }

    @Override
    public MutableByteList of()
    {
        return this.empty();
    }

    @Override
    public MutableByteList with()
    {
        return this.empty();
    }

    @Override
    public MutableByteList withInitialCapacity(int capacity)
    {
        return new ByteArrayList(capacity);
    }

    @Override
    public MutableByteList of(byte... items)
    {
        return this.with(items);
    }

    /**
     * Creates a new list using the passed {@code items} argument as the backing store.
     * <p>
     * !!! WARNING: This method uses the passed in array, so can be very unsafe if the original
     * array is held onto anywhere else. !!!
     */
    @Override
    public MutableByteList with(byte... items)
    {
        if (items == null || items.length == 0)
        {
            return this.with();
        }
        return ByteArrayList.newListWith(items);
    }

    @Override
    public MutableByteList ofAll(ByteIterable items)
    {
        return this.withAll(items);
    }

    @Override
    public MutableByteList withAll(ByteIterable items)
    {
        return ByteArrayList.newList(items);
    }

    /**
     * @since 10.0
     */
    @Override
    public MutableByteList ofAll(Iterable<Byte> iterable)
    {
        return this.withAll(iterable);
    }

    /**
     * @since 10.0
     */
    @Override
    public MutableByteList withAll(Iterable<Byte> iterable)
    {
        return Iterate.collectByte(iterable, Byte::byteValue, ByteLists.mutable.empty());
    }
}
