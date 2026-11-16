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
import jdk.m3.collections.eclipse.api.factory.map.primitive.MutableObjectIntHashingStrategyMapFactory;
import jdk.m3.collections.eclipse.api.map.primitive.MutableObjectIntMap;

/**
 * MutableObjectIntHashingStrategyMapFactoryImpl is a factory implementation which creates instances of type {@link MutableObjectIntMap}.
 * This file was automatically generated from template file mutableObjectPrimitiveHashingStrategyMapFactoryImpl.stg.
 *
 * @since 11.1.
 */
@SuppressWarnings("missing-explicit-ctor")
public class MutableObjectIntHashingStrategyMapFactoryImpl implements MutableObjectIntHashingStrategyMapFactory
{
    public static final MutableObjectIntHashingStrategyMapFactory INSTANCE = new MutableObjectIntHashingStrategyMapFactoryImpl();

    @Override
    public <K> MutableObjectIntMap<K> empty(HashingStrategy<? super K> hashingStrategy)
    {
        return ObjectIntHashMapWithHashingStrategy.newMap(hashingStrategy);
    }

    @Override
    public <K> MutableObjectIntMap<K> of(HashingStrategy<? super K> hashingStrategy)
    {
        return this.empty(hashingStrategy);
    }

    @Override
    public <K> MutableObjectIntMap<K> with(HashingStrategy<? super K> hashingStrategy)
    {
        return this.empty(hashingStrategy);
    }

    @Override
    public <K> MutableObjectIntMap<K> withInitialCapacity(HashingStrategy<? super K> hashingStrategy, int initialCapacity)
    {
        return ObjectIntHashMapWithHashingStrategy.newMapWithInitialCapacity(hashingStrategy, initialCapacity);
    }
}
