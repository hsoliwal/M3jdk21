// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import static jdk.m3.collections.M3PolicyAssertions.*;
import jdk.internal.m3.collections.BoundedLongSequence;
import jdk.internal.m3.collections.LongSequenceMetrics;
import jdk.internal.m3.collections.ObservedLongSequence;
import jdk.internal.m3.collections.ValidatingLongSequence;

//FLLDCIM: RELOCATE - donor blob 0d1db5959be3 at d5ad5090d87e; recipe m3/collections/qualification/census2-t2-20261106/relocate.py v6
public final class M3PolicyForwardingProof {
    public static void main(String[] args) {
        M3PolicyProbe probe=new M3PolicyProbe(2);
        LongSequenceMetrics metrics=new LongSequenceMetrics();
        M3LongSequence sequence=new ObservedLongSequence(
                new BoundedLongSequence(new ValidatingLongSequence(probe,v->v>0),8),metrics);
        long[] caller={3,4};
        sequence.addAll(caller);
        values(sequence,2,3,4);
        eq(1,probe.bulks);
        check(probe.lastBulk!=caller,"cloned bulk");
        sequence.addAll(new long[0]);
        eq(2,probe.bulks);
        long[] sum=new long[1];
        sequence.forEachLong(v->sum[0]+=v);
        eq(9,sum[0]);
        eq(1,probe.eaches);
        long[] split=new long[1];
        sequence.longSpliterator().forEachRemaining((long v)->split[0]+=v);
        eq(9,split[0]);
        eq(1,probe.spliterators);
        eq(9,sequence.longStream().sum());
        eq(1,probe.streams);
        eq(9,sequence.parallelLongStream().sum());
        eq(1,probe.parallelStreams);
        eq(2,metrics.invocations());
        eq(2,metrics.successes());
    }
}
