// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
package jdk.m3.collections;

/** Primitive two-long predicate used for allocation-free filtering of packed maps. */
@FunctionalInterface
//FLLDCIM: RELOCATE - donor blob d394f720b82d at commit d5ad5090d87e (repository, path, URL and digests: m3/collections/recipe/four-library-bags-20261106/PROVENANCE.tsv); recipe m3/collections/recipe/four-library-bags-20261106/relocate.py v1
public interface LongLongPredicate {
    boolean test(long first, long second);
}
