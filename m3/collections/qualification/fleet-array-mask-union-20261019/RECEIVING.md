# M3JDK21 — original-owner fleet union: integer map and mask JNI (2026-10-19)

Product parent is **M3JDK21 #1391** (commit 06c56658975e90aca82b241fe1f4fe26f2b6b62c),
which already includes #1354 sorting, #1365 reentrant filtered views, #1377 utilities,
and #1391 immutable indexed-filter/sort + full-width physical masks.
**This is not an M3 layer on an otherwise unmodified JDK.**

Serial single-file atom delivery:
1. Existing `M3IntIntHashMap.java` source exactly receives first-party #1388
   `addTo` single-discovered-probe insertion atom. No new map owner or Entry.
2. Existing `m3_index_mask_compactor.c` exactly receives #1389 native int/long
   mask-window stable prefix compaction. Only two pre-existing window functions
   differ from parent: remove `accepted[256]` and compact in `values[256]`.
   Preexisting second-state-pass empty-output verification is retained.
3. Add independent Java 21 fixture, JUnit wrapper and source-sealed Java/C JNI CI.
   Do not alter M3IndexedListSnapshot, M3FilteredViews or M3IndexPathMasks.

The native comparison was source-proven mechanically: deleting two scratch
declarations, writing accepted values into the already-read prefix and changing
two SetArrayRegion pointers exactly yields the canonical #1389 source,
apart from two explanatory comments. All exported JNI symbols match.
`row_ids[write] = row_ids[i]` and `values[write] = values[i]` are safe
because accepted-prefix `write <= i`; source Java arrays are never mutated.

Synexia has the only build-time OpenRewrite recipe/candidate catalogue.
OpenRewrite Java LST, native-text and YAML recipe sources are sealed to the
parent's exact before SHA-256 and the reviewed after content.
No runtime Synexia dependency or new JNI symbol.
The category catalogue classifies LeetCode/HackerRank/GFG donor problems
and rejects XOR when mathematical frequency/neighbor contracts differ.

**Admission**: GitHub PR draft, not merged. Existing first-party donor
#1388 reported its own 260,388 Java/260,407 JNI differential checks; these
are historical evidence, NOT an execution of this combined successor.
Must execute this branch's source seals, new Java/JNI tests, inherited
M3PackedFilterTest, all owning module tests, JNI UBSan/ASan, ABI comparison,
Maven/OpenRewrite fixed point and broader modified JDK/HotSpot validation
before promotion. Never discard other fleet PR atoms; #1389's
M3FilteredViews / M3IndexPathMasks changes require separate per-atom analysis.

## Executed receiving iteration (M3JDK21 run 39182463854)

Original owner hash seal PASS. Strict Java21 compiler and independent map/mask
differential: `M3_FLEET_ARRAY_MASK_UNION_PASS backend=java checks=185549`.
Existing C17 JNI compiled with -Wconversion/-Wshadow/-Werror and
`-Xcheck:jni` passed the **same 185549 checks**.

The inherited JUnit Maven stage was NOT RUN: the parent owning-module POM
depends on `org.junit.jupiter:junit-jupiter:5.10.2-m3-source-1` (not
published to Maven Central). Do **not** reinterpret dependency-resolution
failure as a behavioral test failure or use a fabricated JUnit artifact.
Source-sealed workflow successor invokes the actual existing
`M3PackedFilterProof` with direct javac and existing checked-JNI library
so original-owner sorted/filtered regression can execute independently.
The full custom Maven dependency issue remains a separate build gate.

## Historical zero-slot receipt compatibility

The older zero-key selected-source CI refused the new M3IntIntHashMap SHA
even after the canonical, byte-identical, behavior-qualified #1388 receipt.
Do **not** overwrite the historical `SOURCE_MANIFEST.tsv`. The separately
source-sealed `SOURCE_SUCCESSORS.tsv` admits exactly one current reviewed
M3IntIntHashMap postimage, bound to its historical hash, Synexia #12402
and exact product Git blob. The original `verify.py` source is modified
only to recognize that additional exact successor. Any other changed
bytes or an invalid/foreign successor manifest still refuse admission.

The older zero-key workflow's distinct module-JUnit job calls a missing
`./mvnw`; this source-admission repair does not claim to solve that
runner/build dependency or make JUnit pass.

The strict historical-source gate now also SHA-256-pins the entire
`SOURCE_SUCCESSORS.tsv` receipt; a changed successor row cannot silently
authorize arbitrary map bytes without updating the canonical source-first
recipe and source checker together. The predecessor manifest stays immutable.

## Validated exact product successor — 2026-10-19

Source-hash checks, strict Java 21 compilation and actual JNI C17
`-Xcheck:jni` completed successfully in
[M3JDK21 GitHub Actions run 39182744227](https://github.com/hsoliwal/M3jdk21/actions/runs/39182744227):

| Verified product route | Assertions | Status |
|---|---:|---|
| Fleet primitive map and int/long masked arrays — Java | 185,549 | PASS |
| Fleet primitive map and int/long masked arrays — JNI | 185,549 | PASS |
| Inherited packed filtered/sorted snapshots — Java | 351,161 | PASS |
| Inherited packed filtered/sorted snapshots — native | 11,879,132 | PASS |

**12,601,391 checks** across four reported routes.
The first stage on run 39182463854 also passed Java/C/JNI, then stopped
before JUnit because the parent Maven POM requires a custom JUnit artifact
not available from Maven Central; the validated successor directly executes
the existing production `M3PackedFilterProof` via `javac` and JNI.
This is focused functional/native qualification, **not** the full Maven
reactor or a built modified HotSpot image. The earlier zero-key source
checker was separately source-sealed with one fixed SHA-256 successor;
historical Maven wrapper and unavailable custom dependency stay pending.
