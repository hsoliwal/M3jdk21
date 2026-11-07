// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
/**
 * Compile-time static-use admission of M3JDK (package jdk.m3.compiler.staticuse): M3StaticUsePlan turns
 * compiler-proven facts into DIRECT_PARENT, LAZY_HANDLE, MATERIALIZE or REFUSE (unknown is refused), gives the
 * matching reference mode and the int-induction condition-only loop entry bound, and declares the opt-in @Analyze /
 * @CompilerSummary annotations its javac processor reads. Source use counts are never runtime counts; no bytecode is
 * rewritten and no Java object is freed. Relocated by recipe from the first-party donor
 * (recipe/static-use-plan-20261107). Java 21, java.base only.
 */
module org.m3jdk.compiler.staticuse {
    exports jdk.m3.compiler.staticuse;
}
