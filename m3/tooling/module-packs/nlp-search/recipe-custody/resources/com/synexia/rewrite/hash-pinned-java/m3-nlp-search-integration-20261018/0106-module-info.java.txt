/* Copyright 2026 Hitesh Soliwal and Contributors to the Synexia Project.
 * SPDX-License-Identifier: Apache-2.0 */
module org.m3jdk.tools.search {
    requires transitive jdk.compiler;
    requires transitive org.m3jdk.file;
    requires transitive org.apache.lucene.core;
    requires org.apache.lucene.analysis.common;
    requires transitive org.apache.lucene.queryparser;
    requires org.apache.opennlp.tools;
    exports org.m3jdk.tools.search;
}
