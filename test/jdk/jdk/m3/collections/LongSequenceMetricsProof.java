// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import static jdk.m3.collections.PolicyAssertions.*;
import jdk.internal.m3.collections.BoundedLongSequence;
import jdk.internal.m3.collections.LongSequenceMetrics;
import jdk.internal.m3.collections.ObservedLongSequence;
import jdk.internal.m3.collections.ValidatingLongSequence;

//FLLDCIM: RELOCATE - donor blob 001194a13d9d at d5ad5090d87e; recipe m3/collections/qualification/census2-t2-20261106/relocate.py v6
public final class LongSequenceMetricsProof {
    public static void main(String[] args) {
        PolicyProbe probe=new PolicyProbe(1,2);
        LongSequence validated=new ValidatingLongSequence(probe,v->v>0);
        LongSequence bounded=new BoundedLongSequence(validated,4);
        LongSequenceMetrics metrics=new LongSequenceMetrics();
        ObservedLongSequence observed=new ObservedLongSequence(bounded,metrics);
        observed.add(3);
        check(!observed.addIf(8,v->false),"false addIf");
        observed.addAll(new long[]{4});
        fails(IllegalStateException.class,()->observed.insert(0,5));
        eq(1,observed.set(0,2));
        eq(2,observed.removeAt(0));
        check(!observed.removeValue(999),"absent");
        observed.filterInPlace(v->v%2==0);
        observed.trimToSize();
        observed.clear();
        check(observed.addIf(8,v->true),"accepted");
        fails(IllegalArgumentException.class,()->observed.add(-5));
        values(probe,8);
        eq(12,metrics.invocations());
        eq(10,metrics.successes());
        eq(2,metrics.failures());
        eq(3,metrics.growth());
        eq(4,metrics.shrink());
        eq(0,metrics.unknownSizes());
        eq(2,probe.adds);
        eq(0,probe.addIfs);
        eq(1,probe.bulks);
        eq(0,probe.inserts);
        eq(1,probe.sets);
        eq(1,probe.removeAts);
        eq(1,probe.removes);
        eq(1,probe.filters);
        eq(1,probe.trims);
        eq(1,probe.clears);
    }
}
