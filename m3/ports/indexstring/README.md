# Isolated IndexString compatibility port

Six source-owner files are retained with their original names and Apache-2.0 notices. `com.m3.text.compat.M3Text` is a partial stock-JVM facade, not java.lang.String. Do not place both original Synexia classes and these compatibility classes in one loader/module path. No Maven/OpenRewrite/application code is a java.base bootstrap dependency.

`M3Text.fromJoined(existing)` and `fromFrozen(...)` retain owners. `fromString(text, admission)` invokes an existing supplied policy once and checks exact UTF-16; it does not establish a new canonical domain. `concat`/`substring` retain payloads, `asString`/encoding/array outputs materialize intentionally. Equality with String is false in both directions; use `contentEquals`. Regex delegates to java.util.regex, not RE2/J or an approximation.

The facade's bounded UTF-16 KMP is an isolated compatibility fallback, not a replacement for MIndexJoinedNativeSearch's byte-offset API. It does not establish fastest search and is pending owner-level consolidation. Charset encoding uses one whole-input encoder with explicit policies. JNI source in native-test is a stock-runtime boundary probe only.

See ../../migration/REPORT.md, ../../migration/evidence/ and ../../docs/name-mapping.json for exact source, limits, mappings, tests and open gates.
