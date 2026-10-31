// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
module com.m3.nlpsearch {
    requires transitive com.m3.runtime.capability;
    requires transitive org.apache.lucene.core;
    requires transitive org.apache.opennlp.tools;
    exports com.m3.nlpsearch;
}
