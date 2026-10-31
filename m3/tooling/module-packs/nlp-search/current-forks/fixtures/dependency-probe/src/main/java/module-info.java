// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
/** Separate packaging checks; no test-only dependency enters the public M3 bridge. */
module com.m3.nlpsearch.packchecks {
    requires com.m3.nlpsearch;
    requires com.m3.runtime.capability;
    requires org.apache.lucene.core;
    requires org.apache.lucene.analysis.opennlp;
    requires org.apache.lucene.analysis.morfologik;
    requires org.apache.opennlp.tools;
    requires zlibsvm.core;
    requires libsvm;
    requires morfologik.ukrainian.search;
}
