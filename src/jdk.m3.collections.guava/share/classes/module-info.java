/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
 */

/**
 * Guava as M3JDK carries it: all main sources of Guava 33.4.8 and of its failureaccess artifact under the
 * JDK's own package names (Apache-2.0; see legal/guava.md). The exported packages are the ones Guava's own
 * module descriptor exports.
 *
 * @moduleGraph
 */
module jdk.m3.collections.guava {
    requires java.logging;
    // optional at run time: the Unsafe-based fast paths fall back to plain Java when the module is absent
    requires static jdk.unsupported;
    // the nullness annotations of the public signatures
    requires static transitive jdk.m3.annotations;

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
}
