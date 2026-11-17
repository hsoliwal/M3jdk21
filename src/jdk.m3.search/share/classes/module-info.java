/*
 * SPDX-License-Identifier: Apache-2.0 OR EPL-2.0
 * Copyright (c) 2026 Synexia, Hitesh Soliwal (hsoliwal@gmail.com) and others
 */

/**
 * Search and natural-language libraries carried with their upstream packages: Apache Lucene 9.10.0
 * (core, queries, sandbox, queryparser, analysis-common with the Snowball stemmers, analysis-opennlp)
 * and Apache OpenNLP Tools 2.4.0, which logs through the SLF4J API 2.0.12 relocated into a concealed
 * package. Their sources are in providers/classes, their exports and services in
 * providers/classes/module-info.java.extra. The module is not resolved by default
 * (make/CreateJmods.gmk), so it never shadows an application's own copies of these libraries on the
 * class path; add it with {@code --add-modules jdk.m3.search} or require it.
 */
module jdk.m3.search {
    requires java.logging;
    requires java.xml;
    requires jdk.management;
    requires jdk.unsupported;
}
