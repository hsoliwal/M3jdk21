// Copyright 2026 Hitesh Soliwal and contributors.
// Modified 2026-10-17: target package, progress SPI and JNI symbol adaptation.
// SPDX-License-Identifier: Apache-2.0
package com.m3.collections;
import com.m3.collections.M3ProgressMonitor;
import java.util.*;
import java.util.function.*;
/** Adjacent-run lengths, not global grouping; a repeated key can appear in several outputs. */
public final class M3Runs<K> {
    interface Sink<K> {
        boolean accept(K key,long count);
    }
    interface Plan<K> {
        void run(Sink<K> sink,M3ProgressMonitor m);
    }
    private final Plan<K> plan;
    private final int maximum;
    private M3Runs(Plan<K> plan,int maximum) {
        this.plan=plan;
        this.maximum=maximum;
    }
    private static final class State<K> {
        K key;
        long count;
        boolean exists,running=true;
    }
    static <T,K> M3Runs<K> of(M3Flow<T> flow,boolean needs,M3KeyedFlow.Key<T,K> key) {
        return new M3Runs<>((sink,m)-> {
            State<K> state=new State<>();
            var snap=flow.walk(needs,(i,v)-> {
                K next=key.get(i,v);
                M3Flow.check(m);
                if(!state.exists) {
                    state.exists=true;
                    state.key=next;
                    state.count=1;
                }
                else if(Objects.equals(state.key,next))state.count++;
                else {
                    state.running=sink.accept(state.key,state.count);
                    if(!state.running)return false;
                    state.key=next;
                    state.count=1;
                }
                return true;
            }
            ,m);
            if(state.exists&&state.running) {
                M3Flow.check(m);
                sink.accept(state.key,state.count);
                flow.source.guard(snap.version());
                M3Flow.check(m);
            }
        }
        ,flow.maxRows);
    }
    public void forEach(ObjLongConsumer<? super K> action) {
        forEach(action,null);
    }
    public void forEach(ObjLongConsumer<? super K> action,M3ProgressMonitor m) {
        Objects.requireNonNull(action);
        plan.run((k,n)-> {
            action.accept(k,n);
            return true;
        }
        ,m);
    }
    public M3LongFlow counts() {
        return new M3LongFlow((s,m)->plan.run((k,n)->s.accept(n),m),maximum);
    }
}
