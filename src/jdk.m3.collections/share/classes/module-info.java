// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
/**
 * jdk.m3.collections: the M3 collections lane (arrays, collections, algorithms) as one JDK module, moved area by area
 * from the m3/ side tree by m3/tooling/jdk-naming/move_module_area.py (approved layout 2026-11-08). Package
 * names are unchanged (they are API); each area's Maven module becomes a bridge pom over these sources until
 * every consumer has moved.
 */
module jdk.m3.collections {
    // The provider superset (providers/classes) needs these; its exports are in
    // providers/classes/module-info.java.extra.
    // Guava (legal/guava.md) logs through java.util.logging. Its Unsafe fast paths would fall back
    // to plain Java without jdk.unsupported, but Eclipse Collections (legal/eclipse-collections.md)
    // has no fallback: ConcurrentHashMapUnsafe and the parallel iterables' toSet, aggregateBy and
    // aggregateInPlaceBy fail without it. So it is not static.
    requires java.logging;
    requires jdk.unsupported;
    // commons-collections (legal/commons-collections.md): IteratorUtils.getIterator(Object) tests
    // every argument against org.w3c.dom.NodeList, and NodeListIterator has Node and NodeList in
    // its public signatures. Not static: without java.xml that method fails for any argument.
    requires transitive java.xml;

    exports jdk.m3.arrays;
    exports jdk.m3.arrays.packed;
    exports jdk.m3.collections;
    exports jdk.m3.collections.apache;
    exports jdk.m3.collections.bits;
    exports jdk.m3.collections.concurrent;
    exports jdk.m3.collections.graph;
    exports jdk.m3.collections.packed;
    exports jdk.m3.collections.range;
    exports jdk.m3.collections.sketch;
    exports jdk.m3.collections.text;
    exports jdk.m3.algorithm;
    // jdk.m3.paradigm (user decision 2026-11-09): absorbed paradigm code logs through PlaceholderLogger
    exports jdk.internal.m3.collections to jdk.m3.paradigm;
}
