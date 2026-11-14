// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import static jdk.m3.collections.PolicyAssertions.*;
import jdk.internal.m3.collections.ForwardingLongSequence;
import jdk.internal.m3.collections.LongSequenceCompletion;
import jdk.internal.m3.collections.LongSequenceMetrics;
import jdk.internal.m3.collections.ObservedLongSequence;

//FLLDCIM: RELOCATE - donor blob acc0f167caa7 at d5ad5090d87e; recipe m3/collections/qualification/census2-t2-20261106/relocate.py v6
public final class ObserverIsolationProof {
    public static void main(String[] args) {
        PolicyProbe probe=new PolicyProbe();
        AssertionError listenerError=new AssertionError("listener");
        RuntimeException handlerError=new IllegalStateException("handler");
        int[] calls=new int[2];
        ObservedLongSequence good=new ObservedLongSequence(probe,e->{
            calls[0]++;
            throw listenerError;
        },(e,failure)->{
            same(listenerError,failure);
            calls[1]++;
            throw handlerError;
        });
        good.add(1);
        values(probe,1);
        eq(1,calls[0]);
        eq(1,calls[1]);
        RuntimeException original=new IllegalArgumentException("delegate");
        RuntimeException listenerFailure=new IllegalStateException("callback");
        LongSequence throwing=new ForwardingLongSequence() {
            @Override protected LongSequence delegate(){return probe;}
            @Override public boolean add(long v){throw original;}
        };
        int[] failures=new int[2];
        ObservedLongSequence failed=new ObservedLongSequence(throwing,e->{
            failures[0]++;
            same(original,e.failure());
            throw listenerFailure;
        },(e,error)->{
            failures[1]++;
            same(listenerFailure,error);
            throw new AssertionError("handler");
        });
        same(original,fails(RuntimeException.class,()->failed.add(5)));
        eq(1,failures[0]);
        eq(1,failures[1]);
        AssertionError delegateError=new AssertionError("delegate");
        LongSequence errored=new ForwardingLongSequence() {
            @Override protected LongSequence delegate(){return probe;}
            @Override public void trimToSize(){throw delegateError;}
        };
        LongSequenceCompletion[] captured=new LongSequenceCompletion[1];
        ObservedLongSequence obs=new ObservedLongSequence(errored,e->captured[0]=e);
        same(delegateError,fails(AssertionError.class,obs::trimToSize));
        same(delegateError,captured[0].failure());
        PolicyProbe nested=new PolicyProbe();
        LongSequenceMetrics metrics=new LongSequenceMetrics();
        ObservedLongSequence[] ref=new ObservedLongSequence[1];
        LongSequenceCompletion[] event=new LongSequenceCompletion[1];
        ref[0]=new ObservedLongSequence(nested,e->{
            metrics.onCompletion(e);
            event[0]=e;
            ref[0].add(42);
        });
        ref[0].add(7);
        values(nested,7,42);
        eq(2,nested.adds);
        eq(1,metrics.invocations());
        eq(0,event[0].beforeSize());
        eq(1,event[0].afterSize());
        PolicyProbe handlerProbe=new PolicyProbe();
        ObservedLongSequence[] handlerRef=new ObservedLongSequence[1];
        int[] notifications=new int[1];
        handlerRef[0]=new ObservedLongSequence(handlerProbe,e->{
            notifications[0]++;
            throw listenerError;
        },(e,failure)->{
            handlerRef[0].add(9);
            throw handlerError;
        });
        handlerRef[0].add(8);
        values(handlerProbe,8,9);
        eq(1,notifications[0]);
    }
}
