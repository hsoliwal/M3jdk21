# M3JDK21 original provider-range borrowed scratch receiver

2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0.

M3JDK21 is one integrated Java21/HotSpot/JNI SDK, not an M3+JDK
wrapper. This PR follows [original manager-routed M3FS bulk #1841]
(https://github.com/hsoliwal/M3jdk21/pull/1841), which includes
the upstream NIO ZIP/JRT/m3db SQLite and physically indexed
directory-masks/range-transfer owners. No separate filesystem,
SQL driver, Java object graph, JNI runtime or new payload store
is introduced.

## Use cases

1. The existing six-argument `MFileStreams.transferRanges` remains
   available with the same 8 KiB scratch per nonempty call, one
   original MFileContent provider input, read-through gaps, original
   cancellation, shadow/progress and borrowed output behavior.
2. For repeated Maven/Gradle/IDE/project-index or remote/archive
   provider selections, callers may use the **additive overload**
   `transferRanges(source, dest, offsets, lengths, maxEnd,
   byte[] scratch, progress)`. It borrows a 1..1048576-byte
   caller-owned array that is not retained and may be reused
   serially across calls. Scratch validation precedes provider IO.
3. Both public entrypoints dispatch to **one private original
   range-transfer behavioral atom**. The original loop's source
   input, selection-column copies, strict window preflight,
   checked EOF, read-through gap progress/shadows, confirmed
   destination writes and cleanup are shared without duplicate
   logic or altered JNI symbols.
4. The source array is borrowed, exclusive during the operation,
   and must not be mutated/retained by concurrent user or shadow
   callbacks. Source and destination lifetimes and partial-write
   failure semantics are unchanged.
5. The original M3File.captureBytes and borrowed scratch are
   preserved in the parent, and the master V2 successful-vs-
   observed build-state protocol is untouched. A source hash
   alone never authorizes a Maven plugin or javac task skip.

## Recipe-first and algorithm donor custody

Canonical [Synexia recipe](https://github.com/hsoliwal/com.synexia/pull/13018)
uses the existing M3HashPinnedJavaSnapshotRecipe, original
`MFileStreams.java` and original `MFileRangeTransferTest.java`
before/after SHA256, exact two-cycle fixed-point/source-drift-refusal
**test definitions**, category/donor/complexity/license decisions,
and append-only correction memory. This is *not* a Synexia
runtime layer or another Java/JNI SDK.

Both files were applied serially after exact source preimage
comparison and GitHub readback matched the reviewed postimages.
The read-only `verify_received_blobs.py` checks those identities
from a target checkout. It is source-integrity proof only.

## Actual bounded proof and required full gates

An isolated Linux Java21 `javac --release 21 -Xlint:all,-serial
-Werror` source-equivalent kernel mirror passed:

`M3_MFILE_RANGE_SCRATCH_SCOPED_PASS checks=4333`

This used 720 seeded file/range cases over 1, 3, 64, 8192,
65536 and 1048576-byte buffers, independent output and progress
oracles, invalid scratch/no provider IO, EOF, destination failure,
and a compressed ZIP entry selection. The mirror used minimal
MFileContent Input/Progress API shapes. It **did not compile or
execute the entire original M3FS source** nor exercise SQLite
JNI/HotSpot.

The actual existing owning JUnit source has the original range
tests plus two new bounded-buffer methods. **NOT RUN**: full
Synexia Maven/OpenRewrite `Recipe.run`, real `mvn -f m3/fs/pom.xml
verify` with native M3FS SQLite/ZIP/JRT/m3db tests, C17/JNI,
source-built M3JDK21 HotSpot/JMOD/jtreg, Windows/Android providers,
Maven/Gradle ABI/plugin admission and comparative CPU/heap/native
memory allocations. No speedup/heap-saving measurement is claimed.

**DRAFT/HOLD.** The optimization targets aggregate repeated
scratch allocation traffic, not guaranteed live heap or latency.
