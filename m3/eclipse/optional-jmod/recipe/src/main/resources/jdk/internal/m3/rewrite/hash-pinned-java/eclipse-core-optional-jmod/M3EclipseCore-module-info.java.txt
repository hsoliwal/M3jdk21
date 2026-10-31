// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
/** Optional, narrowly scoped M3 substrate for Eclipse API-compatible adapters. */
module jdk.m3.eclipse.core {
    requires transitive jdk.m3.text;
    requires transitive jdk.m3.arrays;
    requires transitive jdk.m3.collections;
    requires transitive jdk.m3.ast;
    requires transitive jdk.m3.file;
    exports jdk.m3.eclipse.core;
    provides jdk.m3.eclipse.core.M3EclipseCore with
            jdk.m3.eclipse.core.impl.M3EclipseCoreProvider;
}
