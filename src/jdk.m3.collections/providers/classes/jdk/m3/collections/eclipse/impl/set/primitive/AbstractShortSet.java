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

import jdk.m3.collections.eclipse.api.set.primitive.ShortSet;
import jdk.m3.collections.eclipse.impl.primitive.AbstractShortIterable;
import jdk.m3.collections.eclipse.api.LazyIterable;
import jdk.m3.collections.eclipse.api.tuple.primitive.ShortShortPair;
import jdk.m3.collections.eclipse.impl.factory.primitive.ShortSets;

/**
 * This file was automatically generated from template file abstractPrimitiveSet.stg.
 *
 * @since 6.0.
 */
@SuppressWarnings("missing-explicit-ctor")
public abstract class AbstractShortSet extends AbstractShortIterable implements ShortSet
{
    @Override
    public LazyIterable<ShortShortPair> cartesianProduct(ShortSet set)
    {
        return ShortSets.cartesianProduct(this, set);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }

        if (!(obj instanceof ShortSet))
        {
            return false;
        }

        ShortSet other = (ShortSet) obj;
        return this.size() == other.size() && this.containsAll(other.toArray());
    }

    @SuppressWarnings("AbstractMethodOverridesAbstractMethod")
    @Override
    public abstract int hashCode();
}
