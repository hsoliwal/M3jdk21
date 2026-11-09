// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
/**
 * Compile-time static-use admission of M3JDK (package jdk.m3.compiler.staticuse): M3StaticUsePlan turns
 * compiler-proven facts into DIRECT_PARENT, LAZY_HANDLE, MATERIALIZE or REFUSE (unknown is refused), gives the
 * matching reference mode and the int-induction condition-only loop entry bound, and declares the opt-in @Analyze /
 * @CompilerSummary annotations, and M3StaticFactsProcessor, the opt-in javac annotation processor that reads them and
 * writes one @CompilerSummary companion per @Analyze method plus the META-INF/m3/compiler-facts.tsv ledger. Source use
 * counts are never runtime counts; no bytecode is rewritten and no Java object is freed. The processor runs inside
 * javac only: java.compiler and jdk.compiler are compile-time (static) dependencies, never required at run time.
 * Relocated by recipe from the first-party donor (recipe/static-use-plan-20261107). Java 21.
 */
module org.m3jdk.compiler.staticuse {
    requires static transitive java.compiler;
    requires static jdk.compiler;
    exports jdk.m3.compiler.staticuse;
}
