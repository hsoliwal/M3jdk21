<!-- Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 / Synexia use-case range refinement — 2026-10-22

**One SDK:** M3JDK21 owns its existing com.m3.file and jdk.m3fs Java 21/JNI runtime classes. The reusable Maven/OpenRewrite recipe and donor studies remain in Synexia; no cross-repository runtime layer, wrapper service, or second payload store.

## Use cases grounded in actual Synexia code
- ByteOffsetIndex builds physical half-open spans and reads records positionally; FileIndex FastCDC gives content-defined chunk boundaries, and AllocationTracker can identify affected chunks when authoritative allocation information exists.
- Maven/Gradle consumers need raw-byte receipts for source/class files, while M3FileDeltaLedger remains responsible for complete-file and input-generation discovery.
- M3FS provides MFileContent and MFileStreams read-through, shadows, progress and capture lifetimes over registered local/ZIP/archive/DB/remote providers. A sequential provider does not necessarily offer NIO random access.

## Serial in-place behavioral atom additions
1. `M3File.java`: `fingerprintRanges(long[] offsets, long[] lengths, long maxEnd[,M3StreamProgress])`. Read one original provider `SeekableByteChannel`; strict NOFOLLOW regular-file checks, source size/mtime/key before and after; sorted disjoint half-open byte coordinates, explicit upper bound and checked EOF; four SHA-256 `long` words per selected range. Snapshot inputs before I/O. `RangeFingerprints` stores only primitive long[] lanes and one FileTime, materializes preexisting `Fingerprint` on demand. Neither range digest nor metadata equality is full-file identity or atomic snapshot.
2. `M3FileRangeContract.java` (new) and original `m3/file/verify.py` (updated) make the use-case executable on host and installed linked Java21 image through existing admission checks.
3. `MFileStreams.java`: `transferRanges(MFileContent, OutputStream, long[] offsets, long[] lengths, long maxEnd, M3FsProgress)`. Preserve one provider stream and all existing shadow/capture/progress contracts. Consume prefix gaps through the same observed Input, write only selected windows to borrowed destination; no flush/close, no automatic retry, no atomic success claim. Empty selection opens nothing, validation precedes I/O.
4. `MFileRangeTransferTest.java` (new): local provider stub and real original stream observation; randomized windows, partial capture exclusion, progress accounting, cancellation, geometry errors, EOF and borrowed output.
5. Separate source-sealed per-module problem-category catalogues and receipt. No changes to native ABI or public Java SE classes.

## Proof status
- **Executed isolated Java21**: `javac --release 21 -Xlint:all -Werror` and 1,200 seeded randomized file+stream region trials, **22,168 assertions PASS**; real local NIO paths, SHA-256 digests, EOF/masks/symlink/cancellation and borrowed destination tests in an isolated test harness. These are algorithm-congruent stand-ins, not compilation of exact M3JDK21 source or JUnit suite.
- **Not yet executed**: actual Synexia Maven/OpenRewrite Recipe.run/fixed-point/refusal, full m3/file verify.py linked-image contract, full m3/fs Maven/JUnit/SQLite/CTest and -Xcheck:jni, full installed M3JDK21 OpenJDK build/jtreg, platform-specific provider matrix, real FastCDC/AllocationTracker integration, CPU/heap/native crossover benchmarks. Local Maven was not present; Java21 and GCC were present.
- The preceding data confirms selected-range hashing/stream transport contracts only. It is not permission to skip builds or infer identical content elsewhere in the file.

## Corrected provider test assumption

The original M3File implementation intentionally insists on a NOFOLLOW_LINKS read channel; Java21 jdk.zipfs currently rejects that channel option with IllegalArgumentException. The first range-contract fixture incorrectly assumed ZIPFS would support the seekable path. The canonical recipe corrected the fixture before target publication, preserving the original security contract instead of stripping NOFOLLOW_LINKS. ZIP archive bytes are handled by the separate M3FS sequential fallback; real ZIP entry data was exercised in a bounded standalone Java21 probe, while the added product JUnit test remains unexecuted.
