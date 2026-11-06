# Isolated IndexString compatibility port

Six source-owner files are retained with their original names and Apache-2.0 notices. `com.m3.text.compat.M3Text` is a partial stock-JVM facade, not java.lang.String. Do not place both original Synexia classes and these compatibility classes in one loader/module path. No Maven/OpenRewrite/application code is a java.base bootstrap dependency.

`M3Text.fromJoined(existing)` and `fromFrozen(...)` retain owners. `fromString(text, admission)` invokes an existing supplied policy once and checks exact UTF-16; it does not establish a new canonical domain. `concat`/`substring` retain payloads, `asString`/encoding/array outputs materialize intentionally. Equality with String is false in both directions; use `contentEquals`. Regex delegates to java.util.regex, not RE2/J or an approximation.

The facade's bounded UTF-16 KMP is an isolated compatibility fallback, not a replacement for MIndexJoinedNativeSearch's byte-offset API. It does not establish fastest search and is pending owner-level consolidation. Charset encoding uses one whole-input encoder with explicit policies. JNI source in native-test is a stock-runtime boundary probe only.

See ../../migration/REPORT.md, ../../migration/evidence/ and ../../docs/name-mapping.json for exact source, limits, mappings, tests and open gates.

`jdk.internal.m3.indexstring.M3IndexCharacterFlags` joins the module through `recipe/character-flags-20261106/` (RELOCATE_INDEX_CHARACTER_FLAGS v1): the donor owner `IndexCharacterFlags` with the recovered package-private `combine(int,int)` (exact flag composition of adjacent UTF-16 segments without a rescan, PR #7532 lineage) and its executable smoke oracle `M3IndexCharacterFlagsCombineSmoke` (test phase, 256 pairs / 4,096 triples / 8 rejections). `jdk.m3.file.M3CharacterFlags` in `m3/ports/file` is the pre-combine lineage of the same donor and is not touched by that leaf; `java.lang.M3StringFacts` REGEX_* bits are a different vocabulary, bit layouts are not merged.
