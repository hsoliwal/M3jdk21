// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import java.util.concurrent.atomic.AtomicInteger;
import static jdk.m3.collections.PolicyAssertions.*;
import jdk.internal.m3.collections.BoundedLongSequence;

//FLLDCIM: RELOCATE - donor blob 1d2af871dbe2 at d5ad5090d87e; recipe m3/collections/qualification/census2-t2-20261106/relocate.py v6
public final class BoundedLongSequenceProof {
    public static void main(String[] args) {
        fails(NullPointerException.class,()->new BoundedLongSequence(null,1));
        fails(IllegalArgumentException.class,
                ()->new BoundedLongSequence(new PolicyProbe(),-1));
        PolicyProbe oversized=new PolicyProbe(1,2);
        fails(IllegalArgumentException.class,()->new BoundedLongSequence(oversized,1));
        values(oversized,1,2);
        PolicyProbe probe=new PolicyProbe(10);
        BoundedLongSequence bound=new BoundedLongSequence(probe,1);
        AtomicInteger predicates=new AtomicInteger();
        check(!bound.addIf(77,v->{predicates.incrementAndGet();return false;}),
                "false predicate at capacity");
        eq(1,predicates.get());
        eq(0,probe.adds);
        fails(NullPointerException.class,()->bound.addIf(3,null));
        RuntimeException fault=new IllegalArgumentException("predicate");
        same(fault,fails(RuntimeException.class,()->bound.addIf(3,v->{throw fault;})));
        fails(IllegalStateException.class,()->bound.addIf(3,v->true));
        fails(IllegalStateException.class,()->bound.add(3));
        fails(IndexOutOfBoundsException.class,()->bound.insert(-1,3));
        fails(IndexOutOfBoundsException.class,()->bound.insert(2,3));
        fails(IllegalStateException.class,()->bound.insert(1,3));
        fails(NullPointerException.class,()->bound.addAll(null));
        fails(IllegalStateException.class,()->bound.addAll(new long[]{3,4}));
        eq(0,probe.bulks);
        bound.addAll(new long[0]);
        eq(1,probe.bulks);
        eq(10,bound.set(0,11));
        bound.trimToSize();
        bound.filterInPlace(v->true);
        values(bound,11);
        bound.removeAt(0);
        bound.add(8);
        values(bound,8);
        bound.clear();
        bound.insert(0,9);
        values(bound,9);
        eq(1,probe.inserts);
        eq(1,probe.adds);
    }
}
