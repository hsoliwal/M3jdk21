// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
/** Source-owned text ingress for the original Lucene and OpenNLP modules. */
module jdk.m3.search {
    requires transitive java.compiler;
    requires transitive org.apache.lucene.core;
    requires transitive org.apache.opennlp.tools;
    exports jdk.m3.search;
}
