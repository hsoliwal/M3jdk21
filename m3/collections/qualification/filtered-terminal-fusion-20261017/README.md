# M3 primitive filtered/sorted terminal cursor fusion (2026-10-17)

Canonical Synexia convergence recipe for the M3JDK21 target. **Correct current preimage** is Git blob 51588ee54029a6b534c1d1a5268bcf2be33f5e72 (SHA256 f2a4d038413e8ddd294be088f8bff9ab127ee63706df1f9e16ef57f1e4b44355). The new source is Git blob 07fdbb60e1a7272272c1f462e5102435392bacef (SHA256 345f3a25aff7c18cd01643a87976570231870949b95ae4a758d486cf22087e3d).

## Java behavioral atoms
M3LongFilteredView size/isEmpty/forEachLong now use the single authoritative M3LongCollection primitive cursor and its existing predicate, **without allocating the extra filtered iterator**. Public types and methods, source encounter order, last-value duplication, Java predicate callback count and exception boundaries are unchanged. Empty view remains live; isEmpty terminates on first match; arbitrary stateful/reentrant/concurrent owner mutation is not admitted. M3LongSortedViews reuses this owner and therefore benefits indirectly.

The existing balanced M3LongPredicates.and (Git blob 2367a2e5f7437ffe340d800a1a5c949cef54adc4) remains unchanged, preserving deep nesting and custom and override behavior. No array payload copying or new index/mask/state lane is introduced; this is an optimization of terminal **control/iterator state** above M3's array/index/mask substrate.

## Mandatory recipe-first
Named com.synexia.m3.FilteredTerminalFusion20261017 reuses the exact existing Maven/OpenRewrite M3HashPinnedJavaSnapshotRecipe (class Git blob d58ca50dfaeb60f581b64f43545e688aca5c6530, explicit PlainText source custody mode). Manifest contains one **preimage→postimage** Java owner and one **ABSENT→new test**. Actual SourceFile Result bytes were compiled/tested, not hand-patched. Four partial states under two input orders, fixed point, source missing/drift/duplicate/late modification refusals. This source-custody mode is NOT semantic-attributed OpenRewrite rewriting; Java compiler and behavioral checks supply separate proof.

## Local actual gate
On Linux x86-64 OpenJDK21.0.12.1 Maven3.9.9 OpenRewrite8.17.1 JUnit5.10.2: real SDK 4/4 Maven/JUnit PASS, no failures/skips; strict javac --release 21 -proc:none -Xlint:all -Werror passed for baseline and candidate; **same 642 assertions / 679300-character source, predicate, consumer, exception trace** and public javap -s descriptors for both variants. SHA256 a482d01252a7a235c73734ad9319b0b9c43c34b3bbb603437b23498e6322c008.

Local test-only ThreadMXBean with disabled escape analysis showed 7168→3072 thread-allocated bytes over 128 warmed calls for EACH of size/isEmpty/forEachLong, a 32-byte/call difference. JIT may already scalar-replace old cursor; no universal throughput, JMH or GC claim. Parent compiled using pinned selected M3 sources, NOT full current JDK image/reactor; existing C17/JNI unchanged and not executed locally for this atom.

To reproduce, supply a matching M3JDK21 receiver checkout and complete Maven cache, then run the crate Maven POM with explicit -Dm3.engine.sources=<M3JDK21>/m3/tooling/migration-recipes/src/main/java and -Dm3.receiver.source=<M3JDK21>. The recipe/test sources stay together and all donor licenses preserved.

See CATEGORY_REVIEW.tsv. LeetCode/HackerRank/GeeksforGeeks are algorithm taxonomy only; no submission/editorial implementation copied. Apache-2.0 attribution retained, Synexia develop remains append-only without rebasing. STRING→arrays→collections progression and installed-JDK admission remain independent.
