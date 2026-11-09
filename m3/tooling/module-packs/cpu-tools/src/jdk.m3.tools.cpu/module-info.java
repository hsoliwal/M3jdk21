// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
/** M3 CPU tools, sharing the existing text DAG, file source and javac owners. */
module jdk.m3.tools.cpu {
    requires transitive jdk.m3.text;
    requires transitive jdk.m3.io;
    requires transitive jdk.m3.structured;
    requires transitive jdk.compiler;
    requires transitive java.compiler;
    requires transitive jdk.m3.tools.text.normalize;
    requires transitive jdk.m3.tools.text.tokenize;
    requires transitive jdk.m3.tools.text.regex;
    requires transitive jdk.m3.tools.text.search;
    requires transitive jdk.m3.tools.text.ngram;
    requires transitive jdk.m3.tools.text.frequency;
    requires transitive jdk.m3.tools.text.similarity;
    requires transitive jdk.m3.tools.text.diff;
    requires transitive jdk.m3.tools.text.index;
    requires transitive jdk.m3.tools.graph.topology;
    requires transitive jdk.m3.tools.graph.paths;
    requires transitive jdk.m3.tools.stats.streaming;
    requires transitive jdk.m3.tools.constraint.interval;
    requires transitive jdk.m3.tools.codec.varint;
    requires transitive jdk.m3.tools.verify.sha256;
    requires transitive jdk.m3.tools.plan.priority;
    exports jdk.m3.tools.cpu;
}
