<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 rope BigText / M3Index proof — 2026-10-18

Status: **DRAFT**. This adds only an independently built M3 core index, NOT an installed java.base/HotSpot String replacement.

## Source, donor and recipe provenance

- Canonical Synexia source packet: https://github.com/hsoliwal/com.synexia/pull/12344
- Exact source templates: synexia-openrewrite-recipes/src/main/resources/com/synexia/rewrite/hash-pinned-java/m3jdk21-rope-bigtext-index-20261018/
- Apple donor pin: apple/swift-collections@b905c889ff75059b8ef0bde480fe55948147b1e6, Sources/RopeModule/Rope and Sources/RopeModule/BigString.
- Separate regex donor: swiftlang/swift-experimental-string-processing. BigString/RopeModule does not itself contain a regex engine.
- Alternate rope reference: cessen/ropey. No Apple, Rust or Swift implementation code was copied. Apple donor sources bear Apache-2.0 WITH Swift-exception; this Java implementation is original first-party code.

Target postimage SHA-256:
- m3/core/src/com/m3/text/M3RopeTextIndex.java = ff2495c01b8df68a01167e29daa4ea4a6af25650e59c5cc705cc2fdc7cfa0141
- m3/core/test/M3RopeTextIndexTest.java = 1374a5b14a32a58324f50f980731a94e5e938235fbfdf27c2e679f0ea1681c92

## Scope

Immutable optional 16-way tree over existing M3StringPiece leaves, with primitive int[] child offsets/counts and state masks, long[] UTF-16 subtree counts and M3StringPiece[] references. No new canonical text store, per-node Entry, String API change, retained JNI pointer or second interner.

- utf16Length(): long coordinates, including logical text larger than Integer.MAX_VALUE.
- charAt(long): exact UTF-16 code-unit random access.
- findLiteral(CharSequence,long): cross-leaf streaming KMP, bounded pattern plan.
- window(long,long): no-copy CharSequence projection with int length, matcher-local single-threaded cursor.
- matcher(Pattern,long,long): full Java Pattern execution only **within one requested window**. Captures and anchors are window-relative; matches crossing two windows are not supported.

The actual M3JDK21 owner java.lang.M3String/M3StringTuple and existing Synexia segmented-text, leaf-cursor, and regex-seam recipes remain authoritative. This index is a candidate metadata accelerator, not a replacement implementation.

## Run the target proof

From the repository root, with a Java 21 JDK:

    M3_JDK=/path/to/jdk21 bash m3/run-rope-proof.sh

This script compiles the current m3/core Java module and runs the rope fixture under the normal JIT, interpreter, noncompact Strings, and C2-focused modes.

The separate exact-source Java21 prototype compiled with -Xlint and passed **83,941 assertions in each mode**. Tests cover random Unicode/unpaired surrogates, split surrogate pairs, window and java.util.regex.Pattern capture parity, cross-segment KMP, nested pieces and a 2,200,000,000-code-unit composition sharing 2,000 references to one 1,100,000-unit leaf.

These were standalone fixture runs, not GitHub CI/complete JDK tests.

## Admission gates still open

1. Synexia Maven/OpenRewrite source-sealed recipe replay and fixed-point/drift-refusal tests — NOT_EXECUTED (no Maven in available runtime).
2. Full Synexia/M3 reactor and actual target branch CI proof script — NOT_EXECUTED.
3. HotSpot, JNI, jtreg/JCK, CDS, GC, native memory and installed-image qualification — NOT_EXECUTED.
4. Before/after performance and memory A/B against current M3StringTuple/JoinedM3StringPiece, including index build cost — NOT_EXECUTED.
5. M3Index/M3File training corpus document boundaries, tokenizer coordinates, stateful full-corpus streaming regex, and pressure-evictable sidecar residency — FUTURE, not implemented by this PR.

Keep draft, preserve String -> arrays -> collections -> AST receiving order, and do not confuse a source proof with runtime promotion.

## Important regex limitation

Standard Java Pattern/Matcher accepts int-sized CharSequence positions. A long-addressed corpus does not make regex itself long-addressed. Window searches have their own anchors, boundaries, lookarounds and capture coordinates. Correct matching across arbitrary window seams needs a different stateful engine with matching semantics proven against JDK Pattern/Matcher; simply repeating chunk-local regex is not equivalent.
