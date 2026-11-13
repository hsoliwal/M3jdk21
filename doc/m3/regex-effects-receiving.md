<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 String regex-effect receiver — 2026-10-14

This is a target-owned, partial receiving candidate, not a complete regex migration.
Source: hsoliwal/com.synexia@8200bd6a92b5e4f81522c177ed0e668bf03664aa.
Target base: hsoliwal/M3jdk21@d5abed865bd47c4dd5ad78e95704348fe2095ed2.
Canonical recipe: com.synexia.rewrite.M3Jdk21RegexEffectsRecipe in Synexia.

## Names and structure

MIndexString remains the donor/reference family; the public API remains java.lang.String,
and the target canonical value remains java.lang.M3String. The new
jdk.internal.mindex.M3RegexDpPlan is primitive regex metadata, NOT a second String owner.
Read name-mapping-regex-effects-20261014.json together with name-mapping.json.
The supplement preserves the original naming history and records partial/unchanged/pending lanes.

MIndexRegexProgram contributes prepared finite-state mechanics; MIndexRegexAtomAlgebra contributes
all-entry-state fragment transformations; MIndexStateRelation contributes ordered Boolean
composition and selected-bit iteration. These are target-owned algorithm adaptations, not
blind prefix renames or direct Apache source copies into GPL files. Existing JDK target files
and new target-owned Java/C/tests retain GPLv2 + Classpath headers. Synexia recipe/control-plane
source remains Apache-2.0; its templates retain the target license. No challenge solution body,
RE2 library, Synexia service, Maven dependency, spelling pool, or public String method is imported.

## Runtime selection

The existing M3StringRegexDp/String.matches path gains a narrow optional effect cache.
The selected candidate cost cohort is a complete canonical tuple whose exact equal-child repetition
spine represents at least 256 occurrences. Source remains capped at 8192 UTF-16 units; the existing
128-pattern-unit and DP-work caps remain. Kernel plans admit at most 63 tokens/64 states;
larger admitted patterns use the unchanged rolling-row implementation.

Every other input keeps the old rolling-DP/Pattern path. The raw cold bit-frontier scanner was
measured slower than the small-pattern two-row reference and was NOT selected for ordinary inputs.
This is a cost decision, not a change to matching semantics or programmer-facing configuration.

Plans precompute literal/dot masks and six nullable-star propagation masks. Effects record the
transformation of every incoming frontier, compose in input order and support repeated powers.
Effects preserve matches across leaf boundaries. They are Boolean whole-input summaries only;
no captures, offsets, find-history, lookarounds or anchors are approximated. Surrogates, flags,
escapes and unsupported patterns retain the existing JDK fallback. Default dot excludes all five
JDK line terminator code points. The JDK remains authoritative for syntax errors and full regex.

JNI composes two bounded primitive effect rows in libjava. It reads no canonical text address,
retains no heap pointer, validates dimensions and every bit, and falls back to Java when the symbol
is unavailable. No user budget or native backend toggle is required. The native symbol is
Java_jdk_internal_mindex_M3RegexDpPlan_compose0; javac -h supplies its target header.

## Ownership and bounds

M3String and M3StringOwner receive no new instance fields. The existing regex helper owns 64 plan
slots and 128 effect slots. Entries weakly key the actual canonical owner and exact coordinate;
effects additionally require the identical plan object. Hash collisions are misses, never matches.
No entry retains String, M3String, text cursors or payload arrays. A plan/effect retains metadata only.

A conservative array-payload bound is (64+128)*134*8 + 128*64*8 = 271360 bytes, excluding scalar
fields, bounded headers/references and transient work. This is a derived bound, not a measured
whole-runtime retained-heap result. Preparation has a 262144 primitive-work budget and depth cap48;
eviction, allocation refusal or budget exhaustion changes only optimization selection. Caches are
allocated lazily without a fallible static holder initializer. They are direct-mapped bounded
caches, not a newly claimed OS-pressure/LRU or persistent database service.

## Evidence and outstanding gates

Executed locally: actual target plan patched into java.base; real JNI library with -Xcheck:jni;
90,342 JDK/DP cases; 460,314 fragment split checks; 2,654,316 arbitrary frontiers; width64 nullable
runs, large repetition, concurrent calls, invalid inputs, cancellation checkpoints; 6,400 direct
JNI matrix comparisons; 8,192 native composition cases with three alias variants under ASan/UBSan
and portable bit iteration. Missing-library Java fallback was executed separately.

The wrapper control-flow/cache fixture changes only its package and the stock-JDK-unavailable
String.m3 accessor. Its owner/tuple/cursor classes are explicitly TEST_ONLY. Its 64 checks, 256
concurrent calls and warm zero-source-read result are NOT actual M3String/HotSpot storage proof.
The real target jtreg test is supplied and stock-compiler checked, but M3-enabled/-disabled and
CompactStrings product runs are pending. Full JDK build/libjava linking, Maven/OpenRewrite SDK,
reactor, Windows/MSVC, retained heap/native memory profiling and end-to-end benchmarks are pending.

Persistent images, full MIndexRegexProgram parsing, capture/search summaries, compile-time lowering,
complete donor-family intake, general memory-pressure spilling and DOOP/SWT/Swing are not delivered
by this receiver. Preserve those rows as pending; no whole-estate completion or phase advancement.
