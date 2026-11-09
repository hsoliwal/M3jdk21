/* Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0 */
/** Qualification-only consumer; this descriptor is not a product API. */
module m3fs.onepass.proof {
    requires jdk.m3fs;
    requires jdk.m3.parser;
    requires jdk.compiler;
    requires java.sql;
    requires org.xerial.sqlitejdbc;
}
