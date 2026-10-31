/* Copyright 2026 Hitesh Soliwal and contributors.
 * SPDX-License-Identifier: Apache-2.0
 */
module jdk.m3.jackson {
    requires transitive com.fasterxml.jackson.databind;
    exports jdk.m3.jackson;
    provides com.fasterxml.jackson.databind.Module with jdk.m3.jackson.M3JacksonModule;
}
