// SPDX-License-Identifier: Apache-2.0
// Copyright 2026 Hitesh Soliwal and contributors
/** Optional Jackson integration over the existing M3 text and received file/AST atoms. */
module jdk.m3.jackson {
    requires transitive com.fasterxml.jackson.databind;
    exports jdk.m3.jackson;
    provides com.fasterxml.jackson.databind.Module with jdk.m3.jackson.M3JacksonModule;
}
