<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Native byte-mask window selection in M3JDK21 libjava — 2026-10-20

<<<<<<< 72b3b8a43682e5d51dae5f17d1e77eb1fa3be8ca
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
=======
## Receiving outcome

The reviewed receiver executes from `java.base`, with its two native methods
compiled into the product's `libjava`. The native and collection-view owners
passed their combined acceptance workloads in installed images containing the
actual compiled product String/M3 closure and a matching source-built process
helper. HotSpot and the remaining module binaries came from the recorded
Temurin 21 boot JDK. This is a qualified receiver closure on Linux x64; a complete
source-built HotSpot/`make images` product build remains pending.

The concrete correction is native capability detection. The original PR #1557
owner considered `System.loadLibrary("java")` sufficient. A normal JDK already
has that library, even when it has neither mask-window method. That made
`nativeAvailable()` return true and let four zero-work `NATIVE_REQUIRED` cases
return successfully without the required ABI.

The corrected owner makes valid, nonaliasing, zero-work calls to **both existing
JNI methods** during its lazy capability check. It admits native execution only
after both symbols resolve and return the expected zero cursor. No capability
export, application library, callback bridge, or second runtime owner was added.
The C receiver includes its generated JNI header and identifies its actual
`libjava` owner. Its existing mask, alias, bounded-range and sparse-span
implementation is preserved.

## Exact provenance and rights

Product base:
[`2779aba3f98f31c324be43f731be7914e1a05935`](https://github.com/hsoliwal/M3jdk21/commit/2779aba3f98f31c324be43f731be7914e1a05935).

Existing Java/C/test receiver:
[M3JDK21 #1557](https://github.com/hsoliwal/M3jdk21/pull/1557), head
[`900a723619fb7021e41b6e2c48a773fd41407224`](https://github.com/hsoliwal/M3jdk21/commit/900a723619fb7021e41b6e2c48a773fd41407224),
branch `m3/java-base-jni-mask-window-20261020`.
Its historical canonical recipe owner is
[Synexia #12558](https://github.com/hsoliwal/com.synexia/pull/12558).
The current convergence packet records canonical application, refusal and replay
separately from the runtime evidence here.

| Receiving source | Original #1557 SHA-256 | Reviewed candidate SHA-256 |
| --- | --- | --- |
| `src/java.base/share/classes/jdk/internal/m3/M3IndexMaskWindow.java` | `f42473470989a103062696f1a3f4aa348e524308391edcaffff3df311d35eacf` | `3133ddda330e1c71d1ecd481ac70d3cade3954cbc1fe2492a5bb49e3fd60b90f` |
| `src/java.base/share/native/libjava/M3IndexMaskWindow.c` | `80ef8aa795825efe9940f0230ba20802084b7d164450668660297516a1091301` | `aba190de61ea6e55e076ec3d175d451b5fea975ed621124b4f084410ef739323` |
| `test/jdk/jdk/internal/m3/M3IndexMaskWindowJniTest.java` | `2490b6752d453daa7f7c4eb7d565ae2894a1236fd0ebaecac637f3230ffb5d8c` | Same bytes; the independent original oracle is retained. |
| `test/jdk/jdk/internal/m3/M3IndexMaskWindowIntegrationTest.java` | New regression and receiving test | `0490a6c7523f5fd1e49c30d3f89c504647b13ea119408fb23bb646f2237cd992` |

The original receiver files carry **Apache-2.0**, not GPL, headers. Those notices
are preserved. The intake record is `EXISTING_DIRECT_COPY_REVIEW`, with a
target-owned correction to the existing receiver. No source is silently
relicensed. The pinned OpenJDK C, process-helper and build files retain their
original rights and notices. Receiving and distribution decisions continue to
follow `m3/docs/m3jdk21-porting-invariant.md` and the product name map.

## Stronger existing behavior preserved

The current collection proving-ground mask-window C owner already contains the
same 128-slot sparse-span atom as #1557, apart from the receiver JNI names.
The following related work was read before receiving this owner:

| Related donor | Pinned revision | Applicable decision |
| --- | --- | --- |
| [#1538](https://github.com/hsoliwal/M3jdk21/pull/1538), compactor row spans | `a02f0d912ff9da60a5dbc0a7832d7f4b56260fa0` | Preserve admitted relative ordinals and omit row reads for rejected chunks. The window already implements this with its existing 128-slot geometry. |
| [#1549](https://github.com/hsoliwal/M3jdk21/pull/1549), current compactor window work | `087e9bca5bd38a3606ac2ce1ced73bfb38916564` | Keep unsigned required/excluded masks and subtraction-based bounds. Independent row/state offsets and allocated int/long compactor results remain their existing separate API contracts. |
| [#1574](https://github.com/hsoliwal/M3jdk21/pull/1574), composed span/offset work | `5faad5bc5683d052d0d265ca2181547117f1bd5e` | Keep the narrowed first-to-last admitted row span. Do not replace the resumable caller-output window with a two-pass allocating compactor or copy its nine-export ABI into this two-method receiver. |

The inspected donor path for these three comparisons is
`m3/collections/src/main/native/collections/m3_index_mask_compactor.c`.
Its source identities and the receiving input identities are retained in the
proof receipt. The wider optional-module compactor and sorted-wrapper histories
are not overwritten by this native receiver.

## Behavioral contract

The caller supplies already ordered primitive row IDs and byte states aligned by
source ordinal. The receiver preserves encounter order, duplicate row IDs and
signed int payloads. Matching is exactly:

```text
(state & requiredMask) == requiredMask && (state & excludedMask) == 0
```

Both masks must fit an unsigned byte. Overlapping masks select nothing.
`scan` examines the remaining source; `scanBounded` admits at most
`maxExamined` source slots. At most 4,096 row IDs are written into a caller-owned
output window. The high half of the packed result is the next exclusive source
ordinal; the low half is the written count. An empty bounded result may still
have a nonterminal continuation.

All masks, references, aliases and requested geometry are validated before
writes. Source/output aliasing is rejected, and the output guards and input
arrays are unchanged after rejected calls. The raw JNI validators already did
this in #1557; the new tests qualify their existing behavior directly, bypassing
the Java validator. They do not turn that existing protection into a claimed
new defect.

Zero output limits, empty source budgets, terminal source positions and
contradictory masks still honor `NATIVE_REQUIRED`. A missing method refuses
instead of silently falling back. The Java backend remains an explicit
independent choice.

The native kernel uses four fixed 128-element scratch arrays: 1,280 bytes of
declared primitive array storage before scalar locals, compiler padding and
JNI/VM frames. It allocates no native heap, retains no array pointer, and invokes
no Java predicate or comparator. Java capability discovery creates only three
empty, nonretained arrays once during lazy initialization. There is no retained
per-entry object store. The caller must keep its input arrays stable during a
call and revalidate the parent generation between calls.

## Actual source build and installed image closure

The proof compiles all **75 pinned Linux libjava C translation units plus the
receiver**, with no stubs, omitted product sources or substituted native
implementations. The receiver compiles against its generated header with strict
warnings, and the shared library links with `-Wl,-z,defs`.

Original and corrected receiver headers are byte-identical, SHA-256
`8a7c78ff33b2a174e819ab04cf66e97722193ac6af30db60b77eed82ddf70393`.
The linked receiver exports exactly:

```text
Java_jdk_internal_m3_M3IndexMaskWindow_scanNative
Java_jdk_internal_m3_M3IndexMaskWindow_scanBoundedNative
```

Two real failures identified additional required native-image inputs:

1. Pinned `jni_util.c` expects `String.m3`. An image with stock String classes
   failed at VM initialization with `NoSuchFieldError`. Compiling and installing
   the actual pinned String/M3 class closure resolved that dependency.
2. Pinned `ProcessImpl_md.c` and the newer stock `jspawnhelper` disagreed about
   their argument protocol. A real `ProcessBuilder` launch failed before any
   Maven tests ran. The pinned `jspawnhelper.c`, linked with the same
   `childproc.o` used by libjava as prescribed by `Launcher.gmk`, resolved it.
   Both a shell and child JVM then executed through explicit `POSIX_SPAWN`.
   The proof does not force a fork fallback.

The positive image uses those actual source-built Java/native/helper inputs
alongside the supplied Temurin HotSpot and other module binaries. Negative
String, helper and partial-ABI images are retained separately. Library, helper,
VM, module and class hashes are not mixed across those image variants.

The compiler-capable image contains the actual new `java.base` classes in its
`jrt` image. It supports normal forked Maven/Surefire execution and the platform
type lookup used by the inspected OpenRewrite parser. This closes the required
receiving-tool execution path; it does not qualify the full compiler or VM.

## Executed gates

The portable proof's compact receipt and full reproducible driver live at:

- [Native proof instructions](../collections/algorithm-catalog/wrapper-convergence-20261020/native-proof/README.md)
- [Portable driver](../collections/algorithm-catalog/wrapper-convergence-20261020/native-proof/qualify.py)
- [Recorded evidence](../collections/algorithm-catalog/wrapper-convergence-20261020/native-proof/receipt.json)

| Gate | Recorded outcome |
| --- | --- |
| Unchanged donor on the absent-ABI JDK | RED: false availability plus four zero-work native-required successes. Nonempty missing-symbol calls already refused. |
| Corrected owner on the absent-ABI JDK | GREEN: 9 assertions, including guarded output and all required refusal cases. |
| Real source-built library with only the bounded method hidden | GREEN: the same 9 assertions; both existing methods must resolve before capability is admitted. |
| Installed native integration | 141,780 assertions per interpreter/mixed run, including scalar parity, offset/cursor/output guards, direct JNI validation, bootstrap/module/jrt origin and the loaded image libjava path. |
| Unchanged original oracle | 12,000 seeded cases for each Java/native and interpreter/mixed combination. |
| Combined CollectionViews bootstrap contract | 77 checks in each execution mode. |
| Combined CollectionViews core contract | 10 methods, 14,694 checks in each execution mode. |
| Combined CollectionViews COW contract | 120 checks in each execution mode. |
| Combined CollectionViews identity contract | Passed in each execution mode. |
| Combined matrix | 14 fresh-JVM workloads passed; no `-Xcheck:jni` warnings. |
| Matching process helper | Required POSIX_SPAWN shell and child JVM passed in both the combined and compiler images. |
| Compiler image | Actual image `javac` and child-process launch passed. Canonical SDK/OpenRewrite qualification is recorded by its separate receiving packet. |

The matrix invokes every unsigned required/excluded mask pair through both scan
APIs and covers sparse 128-slot edges, large source windows, zero work and maximum
output limits. Counts describe assertions or seeded cases within the named
oracle. Repeated execution modes are not additional unique cases.

The receipt records elapsed/user/system CPU time and maximum child RSS for the
qualification workload. GNU `time -v` was unavailable in the recorded container,
so Linux `wait4` supplied those process measurements. They are not a benchmark
comparison, speedup claim or retained-memory budget.

The complete runtime run preceded a metadata-only driver addition: hashes of
ten supplied-JDK files. A full rerun of that revision reached module extraction
and stopped with `ENOSPC`. Its partial logs remain separate. The metadata seal
control executed the exact added code, verified every nominated product/helper
source hash and retained positive image seal, and reran the 14 combined runtime
workloads plus four refusal/process controls.

The final proof layout places the unchanged default-package helpers at
`native-proof/src/test/java/HeaderFromInstalledClasses.java` and
`native-proof/src/test/java/ProcessBuilderProbe.java`, beneath the qualification
directory linked above. This admits the real Java sources through the unchanged
canonical Java engine. A separate layout control checks old-lookup refusal,
new-path lookup, unchanged source and compiled-class bytes, identical generated
headers, and fresh shell/child-JVM probes on the retained matched images. The
receipt preserves the earlier run and metadata-control hashes under their
original layout. It makes no full-rerun claim for this path-only change.

## Remaining receiving gates

The complete configured `make images` and source-built HotSpot gate is
**NOT_RUN**. Windows/macOS ABI runs, sanitizers, broader GC/concurrency and
lifetime stress, and comparative CPU/native-memory budgets remain **PENDING**.
Other existing M3 native owners and String behavior were included as required
build inputs; their whole behavior is not qualified by these mask-window tests.
Eclipse call-site adoption and canonical recipe replay retain their own evidence.

The product remains M3JDK21 itself. The proof adds no runtime Synexia package,
external application JNI library or per-entry storage owner, and it does not
advance the global String-to-arrays-to-collections qualification sequence.
>>>>>>> 0cd0944136c1a87b1bb8e15e8052ed1fbdfb74c5
