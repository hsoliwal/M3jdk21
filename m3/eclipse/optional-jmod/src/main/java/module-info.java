// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
/** Optional, narrowly scoped M3 substrate for Eclipse API-compatible adapters. */
module com.m3.eclipse.core {
    requires transitive com.m3.text;
    requires transitive com.m3.arrays;
    requires transitive com.m3.collections;
    requires transitive com.m3.ast;
    requires transitive com.m3.file;
    exports com.m3.eclipse.core;
    provides com.m3.eclipse.core.M3EclipseCore with
            com.m3.eclipse.core.impl.M3EclipseCoreProvider;
}
