// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
/**
 * M3JDK21-owned filesystem runtime and the attributed Apache Commons VFS provider core.
 * The m3-vfs-core source profile retains excluded remote providers in source custody.
 */
module jdk.m3fs {
    requires java.xml;
    requires java.logging;
    requires jdk.m3.file;
    requires org.apache.commons.io;
    requires org.apache.commons.lang3;
    // Original Apache Commons Logging 1.2 Log ABI is exported from this module's
    // own source-built package; TarArchiveEntry retains its current separate boundary.
    requires transitive org.apache.commons.compress;

    exports jdk.m3.fs;
    exports jdk.m3.fs.vfs;
    exports jdk.m3.fs.vfs.auth;
    exports jdk.m3.fs.vfs.cache;
    exports jdk.m3.fs.vfs.events;
    exports jdk.m3.fs.vfs.filter;
    exports jdk.m3.fs.vfs.function;
    exports jdk.m3.fs.vfs.impl;
    exports jdk.m3.fs.vfs.operations;
    exports jdk.m3.fs.vfs.operations.vcs;
    exports jdk.m3.fs.vfs.provider;
    exports jdk.m3.fs.vfs.provider.bzip2;
    exports jdk.m3.fs.vfs.provider.compressed;
    exports jdk.m3.fs.vfs.provider.gzip;
    exports jdk.m3.fs.vfs.provider.jar;
    exports jdk.m3.fs.vfs.provider.local;
    exports jdk.m3.fs.vfs.provider.ram;
    exports jdk.m3.fs.vfs.provider.res;
    exports jdk.m3.fs.vfs.provider.tar;
    exports jdk.m3.fs.vfs.provider.temp;
    exports jdk.m3.fs.vfs.provider.url;
    exports jdk.m3.fs.vfs.provider.zip;
    exports jdk.m3.fs.vfs.util;
    exports org.apache.commons.logging;
    exports org.apache.commons.logging.impl;
}
