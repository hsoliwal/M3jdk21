// Copyright 2026 Hitesh Soliwal and contributors
// SPDX-License-Identifier: Apache-2.0
/** M3JDK21-owned filesystem runtime; directly consumes the original M3File SDK module. */
module jdk.m3fs {
    requires com.m3.file;
    exports com.m3.fs;
}
