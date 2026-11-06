// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

import java.util.Objects;

/** Single-threaded completed calls and terminal size deltas, not changed elements. */
//FLLDCIM: RELOCATE - donor blob a5c2d45140b9 at d5ad5090d87e; recipe m3/collections/qualification/census2-t2-20261106/relocate.py v6
public final class M3LongSequenceMetrics implements M3LongSequenceListener {
    public M3LongSequenceMetrics() { }
    private long invocations,successes,failures,growth,shrink,unknownSizes;
    @Override public void onCompletion(M3LongSequenceCompletion event) {
        Objects.requireNonNull(event,"event");
        invocations++;
        if(event.succeeded()) successes++;
        else failures++;
        if(event.beforeSize()<0||event.afterSize()<0) {unknownSizes++;return;}
        long delta=(long)event.afterSize()-event.beforeSize();
        if(delta>0) growth+=delta;
        else shrink-=delta;
    }
    public long invocations(){return invocations;}
    public long successes(){return successes;}
    public long failures(){return failures;}
    public long growth(){return growth;}
    public long shrink(){return shrink;}
    public long unknownSizes(){return unknownSizes;}
}
