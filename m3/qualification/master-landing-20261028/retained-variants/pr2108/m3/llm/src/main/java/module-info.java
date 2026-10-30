// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
/** M3JDK-owned text summarization and explicit Chrome inference capabilities. */
module com.m3.llm {
    requires transitive jdk.httpserver;
    exports com.m3.llm.chrome;
    exports com.m3.llm.chrome.aix;
    exports com.m3.llm.evidence;
    exports com.m3.llm.extract;
    exports com.m3.llm.summary;
    exports com.m3.llm.projection;
}
