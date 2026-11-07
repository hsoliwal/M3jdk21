// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
/**
 * Parent-owned annotation processor of M3JDK (package jdk.m3.compiler.parentowned): M3ParentOwnedProcessor, the opt-in
 * JSR-269 processor of jdk.m3.collections.M3ParentOwned that writes the deterministic, conservative
 * META-INF/m3/parent-owned-v1.tsv sidecar (owner, flags and element kind of every annotated class, field and method;
 * static reference counts and escape proofs are never fabricated). It never mutates the annotated type and runs inside
 * javac only: java.compiler is a compile-time (static) dependency, and jdk.m3.collections is static too (the processor
 * reads M3ParentOwned.class; the code under compilation already carries that module). Opt-in through -processor; no
 * service registration. Relocated by recipe from the first-party donor (recipe/parent-owned-processor-20261107). Java 21.
 */
module org.m3jdk.compiler.parentowned {
    requires static transitive java.compiler;
    requires static jdk.m3.collections;
    exports jdk.m3.compiler.parentowned;
}
