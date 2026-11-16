/*
 * Copyright (c) 2026 Goldman Sachs.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.impl.set.primitive;

import jdk.m3.collections.eclipse.api.set.primitive.LongSet;
import jdk.m3.collections.eclipse.impl.primitive.AbstractLongIterable;
import jdk.m3.collections.eclipse.api.LazyIterable;
import jdk.m3.collections.eclipse.api.tuple.primitive.LongLongPair;
import jdk.m3.collections.eclipse.impl.factory.primitive.LongSets;

/**
 * This file was automatically generated from template file abstractPrimitiveSet.stg.
 *
 * @since 6.0.
 */
@SuppressWarnings("missing-explicit-ctor")
public abstract class AbstractLongSet extends AbstractLongIterable implements LongSet
{
    @Override
    public LazyIterable<LongLongPair> cartesianProduct(LongSet set)
    {
        return LongSets.cartesianProduct(this, set);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }

        if (!(obj instanceof LongSet))
        {
            return false;
        }

        LongSet other = (LongSet) obj;
        return this.size() == other.size() && this.containsAll(other.toArray());
    }

    @SuppressWarnings("AbstractMethodOverridesAbstractMethod")
    @Override
    public abstract int hashCode();
}
