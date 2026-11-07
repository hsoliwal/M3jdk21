// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import java.util.ArrayList;
import java.util.List;
import static jdk.m3.collections.M3PolicyAssertions.*;
import jdk.internal.m3.collections.M3BoundedLongSequence;
import jdk.internal.m3.collections.M3ForwardingLongSequence;
import jdk.internal.m3.collections.M3LongSequenceCompletion;
import jdk.internal.m3.collections.M3LongSequenceMetrics;
import jdk.internal.m3.collections.M3ObservedLongSequence;

//FLLDCIM: RELOCATE - donor blob b8202ade2a5e at d5ad5090d87e; recipe m3/collections/qualification/census2-t2-20261106/relocate.py v6
public final class M3ObservedLongSequenceProof {
    public static void main(String[] args) {
        M3PolicyProbe probe=new M3PolicyProbe(10,20,30);
        List<M3LongSequenceCompletion> events=new ArrayList<>();
        M3ObservedLongSequence observed=new M3ObservedLongSequence(probe,events::add);
        RuntimeException fault=new IllegalStateException("filter");
        same(fault,fails(RuntimeException.class,()->observed.filterInPlace(v->{
            if(v==10)return false;
            if(v==20)throw fault;
            return true;
        })));
        values(probe,20,30);
        eq(1,probe.filters);
        eq(1,events.size());
        M3LongSequenceCompletion partial=events.get(0);
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
            M3LongSequenceCompletion e=events.get(i+1);
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
        M3LongSequenceMetrics rejected=new M3LongSequenceMetrics();
        M3LongSequence constrained=new M3BoundedLongSequence(new M3PolicyProbe(1),1);
        M3ObservedLongSequence outer=new M3ObservedLongSequence(constrained,rejected);
        fails(IllegalStateException.class,()->outer.add(2));
        eq(1,rejected.invocations());
        eq(1,rejected.failures());
        RuntimeException original=new IllegalArgumentException("backend");
        M3LongSequence hostile=new M3ForwardingLongSequence() {
            final M3LongArrayList storage=new M3LongArrayList();
            @Override protected M3LongSequence delegate(){return storage;}
            @Override public int size(){throw new IllegalStateException("size");}
            @Override public boolean add(long v){throw original;}
        };
        M3LongSequenceMetrics unknown=new M3LongSequenceMetrics();
        same(original,fails(RuntimeException.class,
                ()->new M3ObservedLongSequence(hostile,unknown).add(7)));
        eq(1,unknown.unknownSizes());
    }
}
