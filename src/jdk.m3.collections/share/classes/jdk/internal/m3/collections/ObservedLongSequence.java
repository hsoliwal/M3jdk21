// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections;

import java.util.Objects;
import java.util.function.LongPredicate;
import java.util.function.LongSupplier;
import jdk.m3.collections.LongSequence;

/** Single-threaded; callback reentry is permitted but observation is suppressed. */
//FLLDCIM: RELOCATE - donor blob 7338965418c4 at d5ad5090d87e; recipe make/m3/collections/qualification/census2-t2-20261106/relocate.py v6
public final class ObservedLongSequence extends ForwardingLongSequence {
    private final LongSequence backend;
    private final LongSequenceListener listener;
    private final LongSequenceErrorHandler handler;
    private boolean notifying;
    public ObservedLongSequence(LongSequence backend,LongSequenceListener listener) {
        this(backend,listener,(event,failure)->{});
    }
    public ObservedLongSequence(LongSequence backend,LongSequenceListener listener,
                                LongSequenceErrorHandler handler) {
        this.backend=Objects.requireNonNull(backend,"backend");
        this.listener=Objects.requireNonNull(listener,"listener");
        this.handler=Objects.requireNonNull(handler,"handler");
    }
    @Override protected LongSequence delegate(){return backend;}
    private int safeSize() {
        try {
            int size=backend.size();
            return size<0?-1:size;
        } catch(RuntimeException|Error ignored) {return -1;}
    }
    private void complete(String operation,int before,Throwable failure) {
        try {
            int after=safeSize();
            LongSequenceCompletion event=new LongSequenceCompletion(
                    operation,before,after,failure==null,failure);
            notifying=true;
            try {
                listener.onCompletion(event);
            } catch(RuntimeException|Error listenerError) {
                try {
                    handler.onListenerFailure(event,listenerError);
                } catch(RuntimeException|Error ignored) {}
            } finally {
                notifying=false;
            }
        } catch(RuntimeException|Error ignored) {
            notifying=false;
        }
    }
    private long observe(String operation,LongSupplier call) {
        if(notifying) return call.getAsLong();
        int before=safeSize();
        Throwable failure=null;
        try {
            return call.getAsLong();
        } catch(RuntimeException|Error original) {
            failure=original;
            throw original;
        } finally {
            complete(operation,before,failure);
        }
    }
    @Override public boolean add(long v) {
        return observe("add",()->backend.add(v)?1L:0L)!=0L;
    }
    @Override public boolean addIf(long v,LongPredicate p) {
        return observe("addIf",()->backend.addIf(v,p)?1L:0L)!=0L;
    }
    @Override public boolean addAll(long[] v) {
        return observe("addAll",()->backend.addAll(v)?1L:0L)!=0L;
    }
    @Override public void insert(int i,long v) {
        observe("insert",()->{backend.insert(i,v);return 0L;});
    }
    @Override public long set(int i,long v) {
        return observe("set",()->backend.set(i,v));
    }
    @Override public long removeAt(int i) {
        return observe("removeAt",()->backend.removeAt(i));
    }
    @Override public boolean removeValue(long v) {
        return observe("removeValue",()->backend.removeValue(v)?1L:0L)!=0L;
    }
    @Override public int filterInPlace(LongPredicate p) {
        return (int)observe("filterInPlace",()->backend.filterInPlace(p));
    }
    @Override public void clear() {
        observe("clear",()->{backend.clear();return 0L;});
    }
    @Override public void trimToSize() {
        observe("trimToSize",()->{backend.trimToSize();return 0L;});
    }
}
