/*
 * Copyright (c) 2026 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.impl.map.mutable.primitive;

import jdk.m3.collections.eclipse.api.block.HashingStrategy;
import jdk.m3.collections.eclipse.api.factory.map.primitive.MutableObjectShortHashingStrategyMapFactory;
import jdk.m3.collections.eclipse.api.map.primitive.MutableObjectShortMap;

/**
 * MutableObjectShortHashingStrategyMapFactoryImpl is a factory implementation which creates instances of type {@link MutableObjectShortMap}.
 * This file was automatically generated from template file mutableObjectPrimitiveHashingStrategyMapFactoryImpl.stg.
 *
 * @since 11.1.
 */
@SuppressWarnings("missing-explicit-ctor")
public class MutableObjectShortHashingStrategyMapFactoryImpl implements MutableObjectShortHashingStrategyMapFactory
{
    public static final MutableObjectShortHashingStrategyMapFactory INSTANCE = new MutableObjectShortHashingStrategyMapFactoryImpl();

    @Override
    public <K> MutableObjectShortMap<K> empty(HashingStrategy<? super K> hashingStrategy)
    {
        return ObjectShortHashMapWithHashingStrategy.newMap(hashingStrategy);
    }

    @Override
    public <K> MutableObjectShortMap<K> of(HashingStrategy<? super K> hashingStrategy)
    {
        return this.empty(hashingStrategy);
    }

    @Override
    public <K> MutableObjectShortMap<K> with(HashingStrategy<? super K> hashingStrategy)
    {
        return this.empty(hashingStrategy);
    }

    @Override
    public <K> MutableObjectShortMap<K> withInitialCapacity(HashingStrategy<? super K> hashingStrategy, int initialCapacity)
    {
        return ObjectShortHashMapWithHashingStrategy.newMapWithInitialCapacity(hashingStrategy, initialCapacity);
    }
}
