// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

/** Behaviour when a bounded strong value pool has no remaining canonical identity slots. */
//FLLDCIM: RELOCATE - donor blob dfde939c2b6c at commit 3385d79a2968 (repository, path, URL and digests: m3/collections/recipe/immutable-pools-20261106/PROVENANCE.tsv); recipe m3/collections/recipe/immutable-pools-20261106/relocate.py v2
public enum M3StrongPoolOverflowPolicy {
    RETURN_INPUT,
    REJECT
}
