// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
/**
 * Owner-scoped primitive interning of M3JDK (package jdk.m3.intern): immutable bytes, UTF-16 atoms, kind-separated
 * long tuples and acyclic graphs in segmented primitive arenas (M3InternSpace), typed structural shapes
 * (M3InternShapes), prefix-sharing lexical paths (M3InternPathTrie) and the bounded SHA-256 archive container
 * (M3InternArchiveGuard). Handles are meaningful only in their originating space; no Java object, class, class
 * loader or native pointer is interned. Relocated by recipe from the first-party donor (recipe/intern-space-20261106).
 * Java 21, java.base only.
 */
module org.m3jdk.intern {
    exports jdk.m3.intern;
}
