// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
/** M3 CPU tools, sharing the existing text DAG, file source and javac owners. */
module com.m3.tools.cpu {
    requires transitive jdk.m3.text;
    requires transitive jdk.m3.io;
    requires transitive jdk.m3.structured;
    requires transitive jdk.compiler;
    requires transitive java.compiler;
    requires transitive com.m3.tools.text.normalize;
    requires transitive com.m3.tools.text.tokenize;
    requires transitive com.m3.tools.text.regex;
    requires transitive com.m3.tools.text.search;
    requires transitive com.m3.tools.text.ngram;
    requires transitive com.m3.tools.text.frequency;
    requires transitive com.m3.tools.text.similarity;
    requires transitive com.m3.tools.text.diff;
    requires transitive com.m3.tools.text.index;
    requires transitive com.m3.tools.graph.topology;
    requires transitive com.m3.tools.graph.paths;
    requires transitive com.m3.tools.stats.streaming;
    requires transitive com.m3.tools.constraint.interval;
    requires transitive com.m3.tools.codec.varint;
    requires transitive com.m3.tools.verify.sha256;
    requires transitive com.m3.tools.plan.priority;
    exports com.m3.tools.cpu;
}
