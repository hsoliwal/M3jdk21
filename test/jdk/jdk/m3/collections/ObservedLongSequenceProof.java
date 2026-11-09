// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import java.util.ArrayList;
import java.util.List;
import static jdk.m3.collections.PolicyAssertions.*;
import jdk.internal.m3.collections.BoundedLongSequence;
import jdk.internal.m3.collections.ForwardingLongSequence;
import jdk.internal.m3.collections.LongSequenceCompletion;
import jdk.internal.m3.collections.LongSequenceMetrics;
import jdk.internal.m3.collections.ObservedLongSequence;

//FLLDCIM: RELOCATE - donor blob b8202ade2a5e at d5ad5090d87e; recipe m3/collections/qualification/census2-t2-20261106/relocate.py v6
public final class ObservedLongSequenceProof {
    public static void main(String[] args) {
        PolicyProbe probe=new PolicyProbe(10,20,30);
        List<LongSequenceCompletion> events=new ArrayList<>();
        ObservedLongSequence observed=new ObservedLongSequence(probe,events::add);
        RuntimeException fault=new IllegalStateException("filter");
        same(fault,fails(RuntimeException.class,()->observed.filterInPlace(v->{
            if(v==10)return false;
            if(v==20)throw fault;
            return true;
        })));
        values(probe,20,30);
        eq(1,probe.filters);
        eq(1,events.size());
        LongSequenceCompletion partial=events.get(0);
        check(partial.operation().equals("filterInPlace"),"filter completion");
        check(!partial.succeeded(),"failure completion");
        same(fault,partial.failure());
        eq(3,partial.beforeSize());
        eq(2,partial.afterSize());
        check(!observed.addIf(9,v->false),"false addIf");
        observed.addAll(new long[0]);
        check(!observed.removeValue(100),"absent removal");
        eq(20,observed.set(0,20));
        observed.trimToSize();
        String[] expected={"addIf","addAll","removeValue","set","trimToSize"};
        for(int i=0;i<expected.length;i++) {
            LongSequenceCompletion e=events.get(i+1);
            check(e.operation().equals(expected[i]),"noop operation "+i);
            check(e.succeeded()&&e.failure()==null,"noop success");
            eq(2,e.beforeSize());
            eq(2,e.afterSize());
        }
        observed.clear();
        observed.clear();
        eq(8,events.size());
        eq(2,events.get(6).beforeSize());
        eq(0,events.get(7).afterSize());
        LongSequenceMetrics rejected=new LongSequenceMetrics();
        LongSequence constrained=new BoundedLongSequence(new PolicyProbe(1),1);
        ObservedLongSequence outer=new ObservedLongSequence(constrained,rejected);
        fails(IllegalStateException.class,()->outer.add(2));
        eq(1,rejected.invocations());
        eq(1,rejected.failures());
        RuntimeException original=new IllegalArgumentException("backend");
        LongSequence hostile=new ForwardingLongSequence() {
            final LongArrayList storage=new LongArrayList();
            @Override protected LongSequence delegate(){return storage;}
            @Override public int size(){throw new IllegalStateException("size");}
            @Override public boolean add(long v){throw original;}
        };
        LongSequenceMetrics unknown=new LongSequenceMetrics();
        same(original,fails(RuntimeException.class,
                ()->new ObservedLongSequence(hostile,unknown).add(7)));
        eq(1,unknown.unknownSizes());
    }
}
