/*
 * Copyright (c) 2024 Goldman Sachs and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v. 1.0 which accompany this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package jdk.m3.collections.eclipse.impl.set.immutable;

import jdk.m3.collections.eclipse.api.bag.MutableBag;
import jdk.m3.collections.eclipse.api.factory.Bags;
import jdk.m3.collections.eclipse.api.factory.Sets;
import jdk.m3.collections.eclipse.api.set.ImmutableSet;
import jdk.m3.collections.eclipse.api.set.MutableSet;
import jdk.m3.collections.eclipse.impl.list.Interval;
import jdk.m3.collections.eclipse.impl.list.mutable.FastList;
import jdk.m3.collections.eclipse.impl.list.primitive.IntInterval;
import jdk.m3.collections.eclipse.impl.set.mutable.UnifiedSet;
import jdk.m3.collections.eclipse.impl.test.Verify;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

public class ImmutableEmptySetTest extends AbstractImmutableEmptySetTestCase
{
    @Override
    protected ImmutableSet<Integer> classUnderTest()
    {
        return Sets.immutable.of();
    }

    @Override
    @Test
    public void newWithout()
    {
        assertSame(Sets.immutable.of(), Sets.immutable.of().newWithout(1));
        assertSame(Sets.immutable.of(), Sets.immutable.of().newWithoutAll(Interval.oneTo(3)));
    }

    @Override
    @Test
    public void equalsAndHashCode()
    {
        ImmutableSet<Integer> immutable = this.classUnderTest();
        MutableSet<Integer> mutable = UnifiedSet.newSet(immutable);
        Verify.assertEqualsAndHashCode(mutable, immutable);
        Verify.assertPostSerializedIdentity(immutable);
        assertNotEquals(FastList.newList(mutable), immutable);
    }

    @Override
    @Test
    public void countByEach()
    {
        assertEquals(Bags.immutable.empty(), this.classUnderTest().countByEach(each -> IntInterval.oneTo(5).collect(i -> each + i)));
    }

    @Test
    public void countByEach_target()
    {
        MutableBag<Integer> target = Bags.mutable.empty();
        assertEquals(target, this.classUnderTest().countByEach(each -> IntInterval.oneTo(5).collect(i -> each + i), target));
    }
}
