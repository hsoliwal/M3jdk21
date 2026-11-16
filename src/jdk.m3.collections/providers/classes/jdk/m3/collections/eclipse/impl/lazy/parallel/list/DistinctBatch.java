/*
 * Copyright (c) 2021 Goldman Sachs.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.impl.lazy.parallel.list;

import jdk.m3.collections.eclipse.api.annotation.Beta;
import jdk.m3.collections.eclipse.api.block.function.Function;
import jdk.m3.collections.eclipse.api.block.predicate.Predicate;
import jdk.m3.collections.eclipse.api.block.procedure.Procedure;
import jdk.m3.collections.eclipse.impl.lazy.parallel.AbstractBatch;
import jdk.m3.collections.eclipse.impl.lazy.parallel.Batch;
import jdk.m3.collections.eclipse.impl.lazy.parallel.bag.CollectUnsortedBagBatch;
import jdk.m3.collections.eclipse.impl.lazy.parallel.bag.FlatCollectUnsortedBagBatch;
import jdk.m3.collections.eclipse.impl.lazy.parallel.bag.UnsortedBagBatch;
import jdk.m3.collections.eclipse.impl.lazy.parallel.set.SelectUnsortedSetBatch;
import jdk.m3.collections.eclipse.impl.lazy.parallel.set.UnsortedSetBatch;
import jdk.m3.collections.eclipse.impl.map.mutable.ConcurrentHashMap;

@Beta
public class DistinctBatch<T> extends AbstractBatch<T> implements UnsortedSetBatch<T>
{
    private final Batch<T> batch;
    private final ConcurrentHashMap<T, Boolean> distinct;

    public DistinctBatch(Batch<T> batch, ConcurrentHashMap<T, Boolean> distinct)
    {
        this.batch = batch;
        this.distinct = distinct;
    }

    @Override
    public void forEach(Procedure<? super T> procedure)
    {
        this.batch.forEach(each -> {
            if (this.distinct.put(each, true) == null)
            {
                procedure.value(each);
            }
        });
    }

    @Override
    public UnsortedSetBatch<T> select(Predicate<? super T> predicate)
    {
        return new SelectUnsortedSetBatch<>(this, predicate);
    }

    @Override
    public <V> UnsortedBagBatch<V> collect(Function<? super T, ? extends V> function)
    {
        return new CollectUnsortedBagBatch<>(this, function);
    }

    @Override
    public <V> UnsortedBagBatch<V> flatCollect(Function<? super T, ? extends Iterable<V>> function)
    {
        return new FlatCollectUnsortedBagBatch<>(this, function);
    }
}
