// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.internal.m3.collections;

/** Invocation completion, not a change event; negative size means unavailable. */
//FLLDCIM: RELOCATE - donor blob 801ceba608f7 at d5ad5090d87e; recipe m3/collections/qualification/census2-t2-20261106/relocate.py v6
public record M3LongSequenceCompletion(
        String operation,int beforeSize,int afterSize,boolean succeeded,Throwable failure) {
}
