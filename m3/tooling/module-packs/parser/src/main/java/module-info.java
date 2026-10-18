/* Copyright 2026 Hitesh Soliwal and contributors
 * SPDX-License-Identifier: Apache-2.0
 */
/** Optional source, generic grammar, and existing javac-event parser framework. */
module jdk.m3.parser {
    requires transitive jdk.compiler;
    exports jdk.m3.parser;
    exports jdk.m3.parser.grammar;
}
