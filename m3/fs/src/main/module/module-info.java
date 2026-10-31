// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
/**
 * M3JDK21-owned filesystem runtime and the attributed Apache Commons VFS provider core.
 * The m3-vfs-core source profile retains excluded remote providers in source custody.
 */
module jdk.m3fs {
    requires java.xml;
    requires java.logging;
    requires com.m3.file;
    requires org.apache.commons.io;
    requires org.apache.commons.lang3;
    // Original Apache Commons Logging 1.2 Log ABI is exported from this module's
    // own source-built package; TarArchiveEntry retains its current separate boundary.
    requires transitive org.apache.commons.compress;

    exports com.m3.fs;
    exports com.m3.fs.vfs;
    exports com.m3.fs.vfs.auth;
    exports com.m3.fs.vfs.cache;
    exports com.m3.fs.vfs.events;
    exports com.m3.fs.vfs.filter;
    exports com.m3.fs.vfs.function;
    exports com.m3.fs.vfs.impl;
    exports com.m3.fs.vfs.operations;
    exports com.m3.fs.vfs.operations.vcs;
    exports com.m3.fs.vfs.provider;
    exports com.m3.fs.vfs.provider.bzip2;
    exports com.m3.fs.vfs.provider.compressed;
    exports com.m3.fs.vfs.provider.gzip;
    exports com.m3.fs.vfs.provider.jar;
    exports com.m3.fs.vfs.provider.local;
    exports com.m3.fs.vfs.provider.ram;
    exports com.m3.fs.vfs.provider.res;
    exports com.m3.fs.vfs.provider.tar;
    exports com.m3.fs.vfs.provider.temp;
    exports com.m3.fs.vfs.provider.url;
    exports com.m3.fs.vfs.provider.zip;
    exports com.m3.fs.vfs.util;
    exports org.apache.commons.logging;
    exports org.apache.commons.logging.impl;
}
