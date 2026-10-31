<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Native byte-mask window selection in M3JDK21 libjava — 2026-10-20

## Ownership, not an adapter
The product is the actual **M3JDK21** Java / native / HotSpot tree.
`jdk.internal.m3.M3IndexMaskWindow` resides inside `java.base`,
with JNI symbols compiled into its own `libjava`. There is no runtime
dependency on Synexia, a Maven plugin, or the application library
`m3_collections_mask`. Public Java SE arrays and existing
`java.util` contracts remain unchanged.

Canonical recipe owner: [Synexia #12558](https://github.com/hsoliwal/com.synexia/pull/12558).
The exact reviewed donor files from the current product's convergence module
were reused, not reimplemented:

| Convergence donor | JDK product receiver | Generated SHA-256 |
| --- | --- | --- |
| `m3/collections/src/main/java/com/m3/collections/M3IndexMaskWindow.java` | `src/java.base/share/classes/jdk/internal/m3/M3IndexMaskWindow.java` | `f42473470989a103062696f1a3f4aa348e524308391edcaffff3df311d35eacf` |
| `m3/collections/src/main/native/collections/m3_index_mask_window.c` | `src/java.base/share/native/libjava/M3IndexMaskWindow.c` | `80ef8aa795825efe9940f0230ba20802084b7d164450668660297516a1091301` |
| `m3/collections/src/test/java/com/m3/collections/M3MaskSpansContract.java` | `test/jdk/jdk/internal/m3/M3IndexMaskWindowJniTest.java` | `2490b6752d453daa7f7c4eb7d565ae2894a1236fd0ebaecac637f3230ffb5d8c` |

## Behavioral invariants
- The user-visible data structures are dynamic arrays and primitive
  index coordinates, not one object/node/entry allocated per row.
- Selection is in existing source encounter order; duplicate row IDs,
  signed int payloads and physical offset distinctions survive.
- Aligned `int[]` row IDs and `byte[]` unsigned state masks use
  `(state & required) == required && (state & excluded) == 0`.
- Output windows of at most 4096 rows and JNI chunks of 128 entries;
  no persistent Java array pins, JNI callbacks, or native heap owner.
- The packed return value contains the next exclusive input index
  (upper 32 bits) and written row count (lower 32 bits).
- Output/src alias and all requested offsets/masks/geometry are checked
  before writes. Explicit native requests do not fall back to Java.
- Java predicates and comparators remain in Java. This is a primitive
  mask kernel, not a replacement for arbitrary filtered collection semantics.

## File-by-file algorithm category
| Problem category | Selected reusable atom | Donor/problem discovery | Complexity |
| --- | --- | --- | --- |
| Mask/filter over ordered rows | One pass with 8-bit required/excluded masks | Existing M3IndexMaskWindow source and JNI | O(n), constant native scratch |
| Dynamic array offset windows | Bounded scan with caller-owned result array | HackerRank Dynamic Array / Java index range contracts | O(window scanned) |
| In-place / stable output compaction | Write selected row IDs in original order | LeetCode 27 two-pointer/compaction category only | O(n), O(1) scalar scratch |
| JNI region-copy minimization | Determine admitted span before GetIntArrayRegion | Existing M3 JNI 128-element-span implementation | O(n), bounded JNI transfer |
| Sorted wrappers | Keep comparator/predicate effects outside JNI | Java NavigableSet contracts; existing M3 collections wrappers | NOT migrated by this atom |

References:
- https://leetcode.com/problems/remove-element/
- https://www.hackerrank.com/challenges/dynamic-array/problem
- https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/NavigableSet.html

Algorithm challenge pages are *category discovery only*. No problem
statement or contributed third-party solution source was copied.

## Acceptance gates (still PENDING)
1. Synexia Maven/OpenRewrite exact-source replay, refusal on drift and
   no-write fixed point from the canonical #12558 Java+text crates.
2. Full linked M3JDK21 `make images` build and native `libjava`
   JNI entrypoint symbol verification on Linux/Windows/macOS.
3. `make test TEST="jtreg:test/jdk/jdk/internal/m3/M3IndexMaskWindowJniTest.java"`
   with interpreter and mixed execution; C17 compile,
   sanitizer and native memory/GC safety.
4. Existing M3IndexedArray / M3IndexedView contract suites,
   invalid/mutating input/refusal matrices and cross-platform ABI.
5. CPU time, bytes copied, retained heap, and native process memory
   measured separately for Java reference vs JNI execution.

**Status:** source-received review candidate only. Source and class
interfaces are additive. Existing `com.m3.collections` donor files,
other M3 native libraries and their tests are unchanged. This is not
completion of the whole Java/JNI integration or permission to relax
the staged String -> arrays -> collections -> AST/compiler qualification.
First-party Apache-2.0 and OpenJDK per-file rights must be admitted
explicitly before merging into a released JDK image.
