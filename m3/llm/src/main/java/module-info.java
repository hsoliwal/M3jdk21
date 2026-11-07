// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
/** M3JDK-owned text summarization and explicit Chrome inference capabilities. */
module jdk.m3.llm {
    requires transitive jdk.httpserver;
    exports jdk.m3.llm.chrome;
    exports jdk.m3.llm.chrome.aix;
    exports jdk.m3.llm.evidence;
    exports jdk.m3.llm.extract;
    exports jdk.m3.llm.summary;
    exports jdk.m3.llm.projection;
}
