/*
 * Copyright (c) 2022 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.stream.primitive;

import java.util.Spliterator;
import java.util.function.IntConsumer;

/**
 * IntSingletonSpliterator is a specialized spliterator of type int.
 * This file was automatically generated from template file primitiveSingletonSpliterator.stg.
 *
 * @since 10.0
 */
public final class IntSingletonSpliterator implements Spliterator.OfInt
{
    private final int elem;
    private boolean visited = false;

    public IntSingletonSpliterator(int elem)
    {
        this.elem = elem;
    }

    @Override
    public OfInt trySplit()
    {
        return null;
    }

    @Override
    public long estimateSize()
    {
        if (this.visited)
        {
            return 0;
        }
        return 1;
    }

    @Override
    public int characteristics()
    {
        return Spliterator.SIZED | Spliterator.SUBSIZED | Spliterator.IMMUTABLE | Spliterator.DISTINCT | Spliterator.ORDERED;
    }

    @Override
    public boolean tryAdvance(IntConsumer action)
    {
        if (this.visited)
        {
            return false;
        }

        visited = true;
        action.accept(elem);
        return true;
    }
}
