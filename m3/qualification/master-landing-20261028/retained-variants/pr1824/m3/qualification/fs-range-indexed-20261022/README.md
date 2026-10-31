# Original M3JDK21 FS/file: range fingerprints, sequential providers and indexed directory views

Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0.

**M3JDK21 is one Java21/HotSpot/JNI SDK.** Source and runtime owners stay
in the original M3File/M3FS/JDK classes. This PR is stacked on
[M3FS provider and capture/scratch superset #1823](https://github.com/hsoliwal/M3jdk21/pull/1823);
it retains original NIO ZIP/JRT provider identity, `m3db:` native SQLite
WAL/JNI state and the original M3 collections arrays/offsets/state masks.

## First-party use cases and implementation ownership

1. **Known byte ranges in a seekable local source/class/index file:**
   `M3File.fingerprintRanges` uses one original checked NIO
   `SeekableByteChannel`, verified sorted/nonoverlapping positions,
   bounded 8KiB scratch and `long[]` hash-word lanes. A
   `RangeFingerprints.fingerprint(i)` record is created on access.
   It is not a full-file fingerprint, offline dirty-file discovery
   guarantee or compiler ABI proof. The file's checked
   `captureBytes`, `observe`, borrowed scratch and existing
   progress APIs remain intact; no older donor M3File.java is copied.
2. **Selected byte transfer through a sequential/archive/database
   provider:** `MFileStreams.transferRanges` reads one original
   `MFileContent` input. It drains gaps through the existing
   shadow/progress mechanism and writes only selected ranges to the
   borrowed output. Failure may leave a partial output prefix; this
   is not an atomic destination commit, retry, close or flush.
3. **Filter and stable-sort an existing SQLite-backed directory:**
   `M3DirectoryTable` keeps original `char[]` names,
   `int[]` offsets and state/size/time primitive lanes from JNI.
   New selected rows and masks operate on physical indexes;
   bottom-up stable sorting manipulates only primitive `int[]`
   row order, not retained copies of source names/entries.
   Read-only list projections materialize `DirEntry` on access
   with validated, detached indexes. The SQLite/JNI row encoding
   and existing encounter order are unchanged.

## Source-first exact custody

Canonical [Synexia #13012](https://github.com/hsoliwal/com.synexia/pull/13012)
contains `RangeIndexedFsJava20261022` and
`RangeIndexedFsVerifier20261022`, reusing existing exact
Maven/OpenRewrite Java and text recipes; each original preimage,
candidate SHA256 and new-source ABSENT row are sealed. The target
receives six Java/test files and one strict Python verifier serially.
All seven source postimages were checked byte-for-byte against the
canonical recipe after receiving.

The original strict `m3/file/verify.py` was extended with the
already-existing observation, shared-scratch and resource-lock
contracts and the new `M3FileRangeContract`. The guard requiring
an *exact* registered host+linked image test inventory remains
in place. The 13 test file stems match the 13 exact verifier keys.
`RECEIVED_DONORS.tsv` contains all seven original Git blob IDs and
`verify_received_blobs.py` is a read-only local reproducibility
gate; the latter does not qualify behavior by itself.

## Canonical problem categories and donor decisions

Catalogue lives in [Synexia #13012](https://github.com/hsoliwal/com.synexia/pull/13012):
known-region hashing vs full-source hashing; sequential skip/read;
parent-owned physical indexes and state masks; primitive stable
mergesort; immutable projection views; source-blessed Maven/AST
build facts; strict host+linked image test census. LeetCode,
HackerRank and GeeksforGeeks contribute comparison categories only,
not copied contest implementations. Preserve existing Apache-2.0
first-party and any Apache Commons VFS upstream notices.

## Evidence and withheld gates

**Verified in this receiving step:** canonical exact pre/post
source readback 7/7; strict source census matching 13/13.

**Historical donor proof only:** #1815 ran isolated equivalent Java21
range tests (1,200 trials/22,168 assertions, plus 28 selected ZIP
bytes); #1793 ran isolated equivalent directory selectors (520
trials/3,134 assertions and standalone C11/Java wire-layout check).
Those were not full original target native builds and have not
been rerun on this combined source.

**NOT EXECUTED:** canonical Maven/OpenRewrite Recipe.run/fixed point;
complete m3/file/verify.py host+linked-JDK image; native M3FS SQLite
JNI/JUnit/CTest across providers, full HotSpot/JMOD/jtreg/JCK and
Android/Windows/macOS matrix; Maven/Gradle ABI/plugin impacts;
five-library JDK/Guava/Eclipse/Commons/Spring collection parity;
comparative CPU/heap/native-memory throughput and allocation
measurements. Do not promote an observed file state to successful
build status based on source hashes, or assert speedup.

Keep **DRAFT** until real source-built SDK gates pass.
