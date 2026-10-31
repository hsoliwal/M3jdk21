# M3 regex closure receiver

Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0.

SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project

This target-owned proof layer checks the actual current M3StringRegexDp.java and complete M3StringRegexDp.c against source-sealed historical baselines. It has no Synexia runtime dependency. Reusable recipe source remains in the existing Synexia Maven/OpenRewrite owner; these files are received through that same qualified source-sealed recipe.

The Java closure identity is B | (S ^ (S + (B & S))). Star bits occupy at most 0..31, so addition cannot overflow long/uint64 and bit32 remains the accepting state. The local zero-frontier fast path is retained explicitly. Existing grammar, bounds, refusal order, tuple/effect/cache behavior and rolling Java are retained. The complete native source is an unchanged pinned input after its arithmetic-validation candidate was rejected by measured selection. source-seals.json binds the exact accepted source versions and fixtures.

Run stages in order on Linux with Java21, a C17 compiler, nm and UBSan available:

    python3 run_target_java21.py --stage static --jdk /path/to/jdk21
    python3 run_target_java21.py --stage compile --jdk /path/to/jdk21
    python3 run_target_java21.py --stage test --jdk /path/to/jdk21
    python3 run_target_java21.py --stage benchmark --jdk /path/to/jdk21

The baseline commit must be in the checkout. Alternatively, --baseline points to immutable before images named M3StringRegexDp.java.txt and M3StringRegexDp.c.txt; their hashes must match. --repo selects the actual target checkout. No runner downloads a dependency or rewrites target source. Output defaults to evidence/local, with ordered PASS/FAIL receipts and retained failure logs.

The fixture compiles exact extracted before/after private Java methods without storage stubs. It generates the actual JNI declaration header, then compiles complete production C and a test-only forwarding/buffer adapter. Separate JVMs run before/after normal and UBSan libraries with -Xcheck:jni. Exhaustive graph/frontier validation, malformed packet order, width/endianness, Unicode refusal and chunk composition are checked. Java fixture source is under src/test/java and native test binding is under src/test/native.

This proves an isolated actual Java leaf and actual target-C JNI entry on a host JVM. A declaration-only class used for header generation is never loaded as java.base. The new test/jdk/java/lang/String/M3StringRegexClosureTest.java invokes the actual helper in a built M3JDK image; host compilation is not jtreg execution. Full build, M3 String dispatch, canonical backing/GC/CDS/HotSpot lifetime, platform ABI and broader product tests remain authoritative.

Benchmarks report three forks, alternating version order, first-operation diagnostic and warm samples. Closure measurements exclude preparation/traversal; JNI measurements include fixture buffer borrowing and actual production validation/scan. A fresh Python observer records child user/system CPU seconds and Linux peak RSS in KiB in .resource.json receipts. The observer has one direct child and is excluded from its resource observations; process peak RSS is separate from retained Java heap. No universal speedup, native threshold crossover or product acceptance follows from this finite fixture.

M3StringRegexEffectsTest is intentionally unchanged: its restored soft-root/128-slot expectation conflicts with current tables/256-slot source. Restoration commit bcbf6bf2cd0b428441db3cf73028a3ef35ad939a left reconciliation pending. Do not weaken that test or report a green full-target gate on the strength of this isolated proof.
