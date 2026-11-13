// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections;

@FunctionalInterface
//FLLDCIM: RELOCATE - donor blob 4c36dde16b8f at d5ad5090d87e; recipe make/m3/collections/qualification/census2-t2-20261106/relocate.py v6
public interface LongSequenceErrorHandler {
    void onListenerFailure(LongSequenceCompletion event,Throwable listenerFailure);
}
