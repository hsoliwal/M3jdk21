// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project
// SPDX-License-Identifier: Apache-2.0
/** Optional JDK 21 API for a caller-operated loopback ChromeML-compatible server. */
module com.synexia.m3.chrome.chat {
    requires java.net.http;
    exports com.synexia.m3.chrome.chat;
}
