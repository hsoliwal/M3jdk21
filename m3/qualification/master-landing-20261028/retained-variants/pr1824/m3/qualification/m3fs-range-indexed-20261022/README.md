# M3JDK21 M3FS/M3File selected-range and indexed-directory receiving

Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0.

**M3JDK21 is one integrated Java21/JDK/HotSpot/JNI SDK**, never an M3+JDK
wrapper. This reviewable draft is stacked directly on
[product #1823](https://github.com/hsoliwal/M3jdk21/pull/1823), which
already preserves [provider/native SQLite #1814](https://github.com/hsoliwal/M3jdk21/pull/1814)
and [shared-scratch #1817](https://github.com/hsoliwal/M3jdk21/pull/1817).
The existing original M3File, MFileStreams, M3DirectoryTable, native
M3FS SQLite and linked Java source are the only runtime owners.

## Actual use-case selection, not another scanner or SQL engine

1. **FileIndex record/chunk hashes:** M3File.fingerprintRanges operates on
   caller-provided sorted, disjoint, half-open *physical byte offsets*.
   It opens one original SeekableByteChannel, checks metadata before and
   after, and records a detached long[] offset/length/SHA256-word image.
   It retains no per-range Entry/Node objects. It does not establish a
   *full-file* equality proof, ABI equivalence or an atomic snapshot.
   Exact range behavior is reused from original donor #1815.

   Critically, source-qualified M3File.java was **not wholesale copied**
   from #1815: its preimage predates current #1823's
   captureBytes()/CapturedBytes and shared `observe(...scratch...)`
   methods. Only the new EOFException import, range behavioral methods,
   and immutable range-columns class were forward-received into the
   existing original class. All previously admitted methods remain.
2. **ZIP/remote/sequential provider selected transfer:** existing
   MFileStreams receives #1815's transferRanges method. It uses one
   original MFileContent input, bounded 8KiB scratch, consumes all
   gaps through existing stream/progress/shadow accounting, transfers
   only selected bytes, and leaves the caller's destination open.
   No independent background read or automatic failed-write rollback.
   The source preimage matches #1815 exactly. M3File's local
   NOFOLLOW_LINKS seekable API is not falsely extended to ZIPFS;
   ZIP/archive sources use the sequential MFileStreams path instead.
3. **Directory filtering, stable sorting and masks:** existing
   M3DirectoryTable receives original #1793's selected physical int[]
   row indexes, long[] bitmask words and stable primitive-index
   mergesort by 64-bit file size. Backed name payload is the original
   char[] with offset/state/scalar columns; DirEntry records
   materialize on demand from read-only projection views. No second
   retained entry payload, SQLite namespace or C/JNI wire change.
4. **Mandatory full owner tests:** original Java range hash contract,
   MFile range transfer JUnit, and indexed directory projection JUnit
   are received unchanged from their original first-party donors.
   The *existing* strict m3/file/verify.py now accounts for
   **all 13 actual file-module contract sources**, including previously
   added observation, shared-scratch, and resource-lock checks plus the
   range hash contract. No old obligation is disabled or removed.

## Recipe-first source/provenance

Exact canonical recipe:
[Synexia #13012](https://github.com/hsoliwal/com.synexia/pull/13012),
`com.synexia.m3.RangeIndexedFsJava20261022` and
`com.synexia.m3.RangeIndexedFsVerifier20261022`. Both existing
M3HashPinnedJavaSnapshotRecipe and M3HashPinnedTextSnapshotRecipe
are reused, not recreated; exact SHA256 source pre/post images and
ABSENT preconditions on three test files, Maven/OpenRewrite fixed-point
and drift-refusal test definitions, category/donor licensing ledger
and append-only MOP corrections are in the recipe.

This product branch contains **seven source-sealed Java/test/Python
source changes**, received one file at a time. All three modified
production files and the verifier matched canonical preimages.
Every file matched its postimage byte-for-byte at GitHub readback.
Read-only `verify_received_blobs.py` checks exact product Git objects
for drift in a local checkout. Competitive-programming sites are
category references; no LeetCode/HackerRank/GFG submissions were copied.

## Evidence is bounded

- **PASS here:** GitHub source preimages/postimages and
  `verify.py` static 13/13 exact contract census.
- **Historical donor-only narrow work:** #1815 isolated
  1,200 randomized trials / 22,168 assertions and compressed ZIP
  probe, #1793 isolated 520 trials / 3,134 assertions and a
  separate C11 native-row encoding check. Those did **not**
  compile this current receiving source or execute the full product.
- **NOT EXECUTED:** actual Maven/OpenRewrite Recipe.run/fixed
  point/source drift, actual m3/file/verify.py full host and
  linked-JDK run, original m3/fs native SQLite JNI/JUnit and
  ZIP/JRT provider suite, complete source-built M3JDK21 HotSpot/
  JMOD/jtreg/JCK, Windows/macOS/Android coverage and comparative
  CPU, allocated heap and native memory benchmarks.

Preserve the caller-owned M3File input generation and the separate
master-V2 SQLite observed-versus-successful checkpoint. No source-hash
or range-hash equality automatically authorizes compilation or plugin
skipping. The M3 collections physical-array/offset/mask, JNI mask
kernels and filtered/sorted view owners in the parent remain unchanged.

**DRAFT. Do not merge on a passing source receipt alone.**
