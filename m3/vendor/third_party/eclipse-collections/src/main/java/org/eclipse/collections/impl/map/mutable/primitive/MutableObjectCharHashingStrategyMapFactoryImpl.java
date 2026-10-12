/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.map.mutable.primitive;

import org.eclipse.collections.api.block.HashingStrategy;
import org.eclipse.collections.api.factory.map.primitive.MutableObjectCharHashingStrategyMapFactory;
import org.eclipse.collections.api.map.primitive.MutableObjectCharMap;

/**
 * MutableObjectCharHashingStrategyMapFactoryImpl is a factory implementation which creates instances of type {@link MutableObjectCharMap}.
 * This file was automatically generated from template file mutableObjectPrimitiveHashingStrategyMapFactoryImpl.stg.
 *
 * @since 11.1.
 */
public class MutableObjectCharHashingStrategyMapFactoryImpl implements MutableObjectCharHashingStrategyMapFactory
{
    public static final MutableObjectCharHashingStrategyMapFactory INSTANCE = new MutableObjectCharHashingStrategyMapFactoryImpl();

    @Override
    public <K> MutableObjectCharMap<K> empty(HashingStrategy<? super K> hashingStrategy)
    {
        return ObjectCharHashMapWithHashingStrategy.newMap(hashingStrategy);
    }

    @Override
    public <K> MutableObjectCharMap<K> of(HashingStrategy<? super K> hashingStrategy)
    {
        return this.empty(hashingStrategy);
    }

    @Override
    public <K> MutableObjectCharMap<K> with(HashingStrategy<? super K> hashingStrategy)
    {
        return this.empty(hashingStrategy);
    }

    @Override
    public <K> MutableObjectCharMap<K> withInitialCapacity(HashingStrategy<? super K> hashingStrategy, int initialCapacity)
    {
        return ObjectCharHashMapWithHashingStrategy.newMapWithInitialCapacity(hashingStrategy, initialCapacity);
    }
}
