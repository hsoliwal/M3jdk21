// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
/**
 * Mat sortable annotation processor of M3JDK (package jdk.m3.compiler.matsortable): M3MatSortableProcessor, the opt-in
 * JSR-269 processor that locates jdk.m3.collections.M3Sortable, M3SortField, M3SortSignal, M3SortDistance and
 * M3SortDependency by canonical name and writes one deterministic source companion per annotated class or record
 * (schema / order-policy / structural seals, compareExact, exactComparator, primary, signals, distance and the
 * ComparisonEvidence receipt). It never mutates the annotated type and runs inside javac only: java.compiler is a
 * compile-time (static) dependency, never required at run time, and m3-collections is not a dependency at all (the
 * annotations are reached by name). Relocated by recipe from the first-party donor
 * (recipe/mat-sortable-processor-20261107). Java 21.
 */
module org.m3jdk.compiler.matsortable {
    requires static transitive java.compiler;
    exports jdk.m3.compiler.matsortable;
}
