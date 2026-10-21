// SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
// Modified 2026: expose the existing collection backend for scoped module resources.
/** Target-owned NIO file access and explicitly scoped module resource boundary. */
module com.m3.file {
    requires transitive com.m3.collections;
    exports com.m3.file;
}
