/*
 * Copyright (c) 2022 Goldman Sachs.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.eclipse.collections.impl.set.primitive;

import org.eclipse.collections.api.set.primitive.ByteSet;
import org.eclipse.collections.impl.primitive.AbstractByteIterable;
import org.eclipse.collections.api.LazyIterable;
import org.eclipse.collections.api.tuple.primitive.ByteBytePair;
import org.eclipse.collections.impl.factory.primitive.ByteSets;

/**
 * This file was automatically generated from template file abstractPrimitiveSet.stg.
 *
 * @since 6.0.
 */
public abstract class AbstractByteSet extends AbstractByteIterable implements ByteSet
{
    @Override
    public LazyIterable<ByteBytePair> cartesianProduct(ByteSet set)
    {
        return ByteSets.cartesianProduct(this, set);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }

        if (!(obj instanceof ByteSet))
        {
            return false;
        }

        ByteSet other = (ByteSet) obj;
        return this.size() == other.size() && this.containsAll(other.toArray());
    }

    @SuppressWarnings("AbstractMethodOverridesAbstractMethod")
    @Override
    public abstract int hashCode();
}
