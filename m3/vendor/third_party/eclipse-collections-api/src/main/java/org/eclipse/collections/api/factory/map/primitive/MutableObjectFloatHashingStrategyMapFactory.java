/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.api.factory.map.primitive;

import org.eclipse.collections.api.block.HashingStrategy;
import org.eclipse.collections.api.map.primitive.MutableObjectFloatMap;

/**
 * A factory which creates instances of type {@link MutableObjectFloatMap}.
 * This file was automatically generated from template file mutableObjectPrimitiveHashingStrategyMapFactory.stg.
 *
 * @since 11.1.
 */
public interface MutableObjectFloatHashingStrategyMapFactory
{
    <K> MutableObjectFloatMap<K> empty(HashingStrategy<? super K> hashingStrategy);

    /**
     * Same as {@link #empty()}.
     */
    <K> MutableObjectFloatMap<K> of(HashingStrategy<? super K> hashingStrategy);

    /**
     * Same as {@link #empty()}.
     */
    <K> MutableObjectFloatMap<K> with(HashingStrategy<? super K> hashingStrategy);

    /**
     * Same as {@link #empty()} but takes in an initial capacity
     */
    <K> MutableObjectFloatMap<K> withInitialCapacity(HashingStrategy<? super K> hashingStrategy, int initialCapacity);
}
