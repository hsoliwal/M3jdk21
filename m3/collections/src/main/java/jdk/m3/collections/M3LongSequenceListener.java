// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

@FunctionalInterface
//FLLDCIM: RELOCATE - donor blob caca481a2cac at d5ad5090d87e; recipe m3/collections/qualification/census2-t2-20261106/relocate.py v6
public interface M3LongSequenceListener {
    void onCompletion(M3LongSequenceCompletion event);
}
