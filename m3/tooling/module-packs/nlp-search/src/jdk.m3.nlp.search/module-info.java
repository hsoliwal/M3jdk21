/* Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
/** Optional source analysis/search over the existing M3 owners and complete donor modules. */
module jdk.m3.nlp.search {
    requires transitive com.m3.text;
    requires transitive jdk.m3.compiletime;
    requires transitive org.apache.lucene.core;
    requires transitive org.apache.opennlp.tools;
    exports jdk.m3.nlp.search;
}
