// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
/**
 * Owner-scoped primitive interning of M3JDK (package jdk.internal.m3.intern): immutable bytes, UTF-16 atoms, kind-separated
 * long tuples and acyclic graphs in segmented primitive arenas (M3InternSpace), typed structural shapes
 * (M3InternShapes), prefix-sharing lexical paths (M3InternPathTrie) and the bounded SHA-256 archive container
 * (M3InternArchiveGuard), and the typed, bit-exact canonical literal pool over one space (M3CanonicalLiteralPool: a
 * pool only, never a JVM constant-pool or ldc replacement), and the class-space family: the owner-scoped, budgeted,
 * failure-atomic exact-byte store of immutable classfile-byte candidates (M3ClassSpace), its immutable SHA-256-framed
 * archive (M3ClassSpaceArchive) and the read-only constant-pool scanner (M3ClassFileAtoms); never a class loader,
 * Klass table or CDS archive. Handles are meaningful only in their originating space; no Java object, class, class
 * loader or native pointer is interned. Relocated by recipe from the first-party donor (recipe/intern-space-20261106,
 * recipe/canonical-literals-20261106, recipe/class-space-20261107). Java 21, java.base only.
 */
module org.m3jdk.intern {
    exports jdk.internal.m3.intern;
}
