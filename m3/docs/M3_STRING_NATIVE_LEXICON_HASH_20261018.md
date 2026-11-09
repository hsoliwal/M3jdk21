# M3JDK21 native java.lang.M3StringPool lexicon hash admission
<!-- SPDX-License-Identifier: GPL-2.0-with-classpath-exception -->

## Direct Java runtime — no external M3 execution layer

This is a bounded **existing owner** algorithm update inside
`src/java.base/share/classes/java/lang/M3StringPool.java`. The unchanged
`java.lang.M3String` identity is its canonical native/mapped atom owner
plus packed coordinate; String's public methods, HotSpot contracts and JNI
symbols are not replaced by a Synexia runtime dependency.

Canonical source-first Maven/OpenRewrite recipe and full postimages:
[hsoliwal/com.synexia PR #12389](https://github.com/hsoliwal/com.synexia/pull/12389).
Named recipe `com.synexia.m3.M3Jdk21NativeLexiconHash20261018`,
using existing `M3HashPinnedJavaSnapshotRecipe`. Product source is exactly
Git blob `09750ae9a656cfeb0a82c4979fb33e4022c49942`.
Product test is exactly blob `3b12412c7a47b17425a0ea36b01a596ead8bd986`.

## Source-bounded atom changes

The existing `M3StringPool.Lexicon` already independently validates Java
String hashes against every mapped canonical UTF-16 spelling. New optional
`int[] hashSlots` reuses those `javaHashes[]`, `lengths[]` and existing
mapped-unit readers. Zero means empty; row+1 is the slot payload, not a
per-string object, `Map.Entry`, heap char copy or another lexicon.

An immutable integer-mixed hash directory is built once with at most 50%
load. A per-probe chain >=64 or an image outside the 64..1,048,576 row
admission band rejects the optional table, not the lexicon. The same is
true for inadequate heap headroom or allocation failure. A miss/long
chain falls back to the original UTF-16 lexical binary search.
Public interning/array signatures and source-specific Latin1/UTF16
semantics are preserved; the Latin1 lookup reuses the existing compact
bytes implementation instead of owning a duplicate implementation.

Full content is always checked at the existing read-only mapped address:
hash equality is merely a candidate, never a semantic equivalence
or an intern identity proof. Native atom payload storage, row IDs,
`M3LEX001` formats v1/v2 and `SYNARR01`, OS mappings, native lifetime,
String compatibility-shadow boundaries, and Java 21 String.intern
semantics remain unchanged.

## JTreg fixture and outstanding acceptance

A new `test/jdk/java/lang/String/M3StringLexiconHashIndexTest.java`
constructs a deterministic synthetic `M3LEX001` v2 UTF-16LE image
including Java-hash collision keys (Aa/BB), Unicode, NUL and isolated
surrogates; checks original array/codepoint/Latin1/compact entry points
and single-character lookups, 2048 negative probes and three-word
binary fallback through the actual private JDK owner.

The test is a **candidate**, not a hosted passed jtreg result yet.
Required before merge: canonical OpenRewrite fixed point and drift
refusal, matched modified M3JDK21 Java21 image, String.intern/substring/
UTF-16/regex jtreg in JIT and -Xint, stress under heap/GC pressure,
CDS/JNI and explicit OS-native lookup comparisons, and measured
setup-inclusive A/B throughput/RSS/cold/warm latency.

## Continuation

This is the String runtime's frozen **spelling** index, not semantic
X-spine identity. The larger UnicodeX concept, language hierarchy,
grammar and direct fixed-address concept descriptor integration must
be generation/source-qualified and proven separately. M3 collections
already own dynamic arrays, offsets, bit masks, filtered/sorted wrappers,
and JNI selectors under `m3/collections`; improve those owners in-place
via a serial per-file catalogue and recipe rather than layering another
collection implementation over JDK.
