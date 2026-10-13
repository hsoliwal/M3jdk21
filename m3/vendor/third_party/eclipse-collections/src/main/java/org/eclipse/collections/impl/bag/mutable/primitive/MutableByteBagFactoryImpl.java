/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.bag.mutable.primitive;

import org.eclipse.collections.api.ByteIterable;
import org.eclipse.collections.api.factory.bag.primitive.MutableByteBagFactory;
import org.eclipse.collections.api.bag.primitive.MutableByteBag;
import org.eclipse.collections.impl.factory.primitive.ByteBags;
import org.eclipse.collections.impl.utility.Iterate;

/**
 * MutableByteBagFactoryImpl is a factory implementation which creates instances of type {@link MutableByteBag}.
 * This file was automatically generated from template file mutablePrimitiveBagFactoryImpl.stg.
 *
 * @since 6.0.
 */
public class MutableByteBagFactoryImpl implements MutableByteBagFactory
{
    public static final MutableByteBagFactory INSTANCE = new MutableByteBagFactoryImpl();

    @Override
    public MutableByteBag empty()
    {
        return new ByteHashBag();
    }

    @Override
    public MutableByteBag of()
    {
        return this.empty();
    }

    @Override
    public MutableByteBag with()
    {
        return this.empty();
    }

    @Override
    public MutableByteBag of(byte... items)
    {
        return this.with(items);
    }

    @Override
    public MutableByteBag with(byte... items)
    {
        if (items == null || items.length == 0)
        {
            return this.with();
        }
        return ByteHashBag.newBagWith(items);
    }

    @Override
    public MutableByteBag ofAll(ByteIterable items)
    {
        return this.withAll(items);
    }

    @Override
    public MutableByteBag withAll(ByteIterable items)
    {
        return ByteHashBag.newBag(items);
    }

    /**
     * @since 10.0
     */
    @Override
    public MutableByteBag ofAll(Iterable<Byte> iterable)
    {
        return this.withAll(iterable);
    }

    /**
     * @since 10.0
     */
    @Override
    public MutableByteBag withAll(Iterable<Byte> iterable)
    {
        return Iterate.collectByte(iterable, Byte::byteValue, ByteBags.mutable.empty());
    }
}
