// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
/**
 * jdk.m3.collections: the M3 collections lane (arrays, collections, algorithms) as one JDK module, moved area by area
 * from the m3/ side tree by m3/tooling/jdk-naming/move_module_area.py (approved layout 2026-11-08). Package
 * names are unchanged (they are API); each area's Maven module becomes a bridge pom over these sources until
 * every consumer has moved.
 */
module jdk.m3.collections {
    // Guava (jdk.m3.collections.guava, see legal/guava.md): its logging, its optional use of Unsafe, and the
    // nullness annotations of its public signatures
    requires java.logging;
    requires static jdk.unsupported;
    requires static transitive jdk.m3.annotations;
    exports jdk.m3.arrays;
    exports jdk.m3.arrays.packed;
    exports jdk.m3.collections;
    exports jdk.m3.collections.apache;
    exports jdk.m3.collections.bits;
    exports jdk.m3.collections.concurrent;
    exports jdk.m3.collections.graph;
    exports jdk.m3.collections.guava.annotations;
    exports jdk.m3.collections.guava.base;
    exports jdk.m3.collections.guava.cache;
    exports jdk.m3.collections.guava.collect;
    exports jdk.m3.collections.guava.escape;
    exports jdk.m3.collections.guava.eventbus;
    exports jdk.m3.collections.guava.graph;
    exports jdk.m3.collections.guava.hash;
    exports jdk.m3.collections.guava.html;
    exports jdk.m3.collections.guava.io;
    exports jdk.m3.collections.guava.math;
    exports jdk.m3.collections.guava.net;
    exports jdk.m3.collections.guava.primitives;
    exports jdk.m3.collections.guava.reflect;
    exports jdk.m3.collections.guava.util.concurrent;
    exports jdk.m3.collections.guava.util.concurrent.internal;
    exports jdk.m3.collections.guava.xml;
    exports jdk.m3.collections.packed;
    exports jdk.m3.collections.range;
    exports jdk.m3.collections.sketch;
    exports jdk.m3.collections.text;
    exports jdk.m3.algorithm;
    // jdk.m3.paradigm (user decision 2026-11-09): absorbed paradigm code logs through PlaceholderLogger
    exports jdk.internal.m3.collections to jdk.m3.paradigm;
}
