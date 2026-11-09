<!-- Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 native directory-array index/mask views (2026-10-21)

**Owner:** original `jdk.m3fs` `com.m3.fs.M3DirectoryTable`; M3JDK21 is one integrated Java 21/HotSpot/JNI SDK. **Canonical source recipe:** `com.synexia.m3.M3FsDirectoryIndexViews20261021`. Synexia is authoring/proof owner only, not a JDK runtime dependency.

## Serial, source-sealed behavioral atoms
1. Original immutable `M3DirectoryTable` retains **exactly** its owned `char[] names`, `int[] offsets`, `int[] state`, `long[] scalar` storage. Add direct native-kind mask projection `selectedRows` and `selectedMaskWords` supporting half-open parent-physical windows and exact detached arrays. No Node/Entry/Cell retained per source element; unselected parent values are not copied.
2. Add stable primitive `rowsBySize` using bottom-up mergesort on physical int[] ordinals. Signed 64-bit size comparison, descending and original tie order; zero mutation to the directory's name/metadata array owner.
3. `entriesByRows` defensively clones and validates caller ordinals, permits duplicates, wraps only one `AbstractList/RandomAccess` projection with strict JDK21 unmodifiable boundary, materializes `DirEntry`/`Stat` on public access. `entriesOfKind` and `entriesBySize` adopt only their internally created primitive index array (no double full index clone).
4. New source-sealed `M3DirectoryTableProjectionTest` covers bit boundaries, stable ties and Long.MAX_VALUE, immutable/reversed projections, invalid geometry and identity, native UTF-8 row codec, and **520 deterministic randomized** comparisons across masks/windows/stable sorts.

## Java/JNI ABI and data correctness boundary
Original `m3fs_jni.c::list_collect` serializes name byte length, UTF-8 name, 4 little-endian uint32 state values (kind/mode/links/block size), 4 little-endian uint64 scalars (inode/size/generation/mtime). Existing `M3DirectoryTable.decode` is the authoritative Java decoder, preserving this exact structure. Neither its native payload nor JNI exports change. Kind flags describe source's 1=file/2=directory/3=symlink **state lane**, not the distinct status bits used in `M3FsBulk.Result` or pending `M3FsStatBatch` (#1740).

## Bounded executed proof, not full product admission
- Java 21 `javac --release 21 -Xlint:all -Werror` and executable stand-in using the same private selection/sort/view atoms: **PASS** with 520 seeded trials and **3,134 independent assertions**, including invalid windows and immutability. The external Java 21 harness used a minimal `M3FS` record source; it did **not** compile this whole original M3DirectoryTable source or execute the new JUnit class.
- Native payload writer and decoder field order were inspected from exact current C/Java sources. In an independent isolated C11/Java21 codec repro, strict `gcc -std=c11 -Wall -Wextra -Werror -Wconversion -Wshadow -Wpedantic` built a 2-row UTF-8 payload and Java21 decoded all fields: **PASS** (`NATIVE_ROW_WIRE_PASS bytes=110`, `JAVA21_NATIVE_ROW_LAYOUT_PASS records=2`). Canonical reusable repro sources are under the recipe crate `repro/`. This is a **wire-format probe**, not the real JNI library/native SQLite/JDK image.

**DRAFT/HOLD — NOT EXECUTED:** exact Maven/OpenRewrite `Recipe.run` fixed-point, absent/missing/drift refusal, original `mvn -B -ntp -f m3/fs/pom.xml verify` provider/JUnit tests, native SQLite CTest/`-Xcheck:jni`, linked JMOD/JDK image, jtreg/GC/HotSpot, C/native ABI, Eclipse/OSGi, cross-platform and comparative CPU/heap/native-memory benchmarks. No speedup or broad all-provider parity claim. No phase advancement from String/arrays/collections policy.

All published target sources and tests must match the canonical SHA-256-pinned source templates before release or merge.
