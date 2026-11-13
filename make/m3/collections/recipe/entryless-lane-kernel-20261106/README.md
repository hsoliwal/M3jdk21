<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- Copyright 2026 Hitesh Soliwal and contributors -->
# entryless-lane-kernel-20261106: the DOOPS key/value signed-range lane kernel joins `jdk.m3.collections.M3IndexMaskCompactor` (recipe RELOCATE_ENTRYLESS_LANE_KERNEL v3)

Third leaf of the native/compiler/OpenJDK convergence plan (`C:/m3-superset/conv/plans/native-compiler-openjdk.tsv`,
row 3 `doops-entryless-keyvalue-lane-kernel`, decision ABSORB). The position-page half of the donor lane (byte-state
position pages, sorted bulk slots) is ALREADY in M3JDK since #2342 (`14d5d27e1d7`, 2026-11-06: `selectPositionsPage` /
`selectPositionsPageNative`, `M3IndexMaskPositionPage{Proof,Test}`, `M3EntrylessSortedBulk{Proof,Test}`); this leaf ports
the other half: the key/value signed-range select / selectInto / count / first / paged scan over caller-owned
`long[] keys`, `long[] values`, `byte[] state` lanes, with its C17 JNI kernel and its full-span region-read elision.

## What it is

The donor `EntrylessLaneKernel` (com.synexia crate `m3-doops-entryless-20261102`, 335 lines) selects the occupied slots
(`state[i] & LIVE`) whose key and value fall into inclusive signed ranges, over three parallel lanes the caller owns.
Nothing is boxed, moved or retained: results are ascending physical slot indices (`select`, exact-size `int[]`),
counts into a caller-owned output lane (`selectInto`: capacity refused before any write, `ConcurrentModificationException`
when the lanes move between the two passes), `countMatching`, `firstMatching` (-1 when none), and one-pass bounded
pages (`scanPageInto`: written in the high 32 bits, next absolute position in the low 32; `pageWritten` / `pageNext`).
Every operation has a `...Java` oracle twin that never calls JNI and a `...NativeRequired` twin that refuses
(`UnsatisfiedLinkError`) instead of falling back; the plain form uses the native kernel when the module library carries
the lane ABI and the Java body otherwise (`laneKernelNativeAvailable()`).

The C17 kernel (`entryless_lane_kernel_jni.c`, 279 lines) reads 256-element `GetLongArrayRegion` / `GetByteArrayRegion`
batches (no critical pins, no retained pointer), validates its own JNI boundary (`m3_validate`), counts then emits
(`SetIntArrayRegion`, drift check) and skips the key or value region read entirely when that lane's inclusive signed
range is the full span (`filter_keys = key_min != INT64_MIN || key_max != INT64_MAX`): the "full-span region-read
elision" of commits `c796689c668` / `b1b4f801a94`.

## Where it landed (member insertion, by recipe)

The donor type's members join the existing `M3IndexMaskCompactor` (plan: additive ABI, one owner), the five natives join
`m3_index_mask_compactor.c` with their symbols rewritten to `Java_jdk_m3_collections_M3IndexMaskCompactor_*`, and the
module library `libm3_collections_mask.so` gains six exports (the five natives and the `laneKernelAbiNative` probe).
`relocate.py` v3 does the insertion (row kinds `MERGE` and `APPEND`: the donor body between two
`//FLLDCIM: RELOCATE-MERGE begin/end` anchors before the host's closing brace, resp. after the host's last line;
`import java.util.Arrays` added to the host's import block) and keeps every donor body line byte for byte except the
declared rows of `RELOCATION_MAP.tsv`:

| Row | What | Why |
|---|---|---|
| `TYPE EntrylessLaneKernel -> M3IndexMaskCompactor` | the oracles' `EntrylessLaneKernel.<member>` calls | one owner |
| `MEMBER nativeLoaded -> laneKernelNativeAvailable` | the flag's name (1 declaration + 4 oracle calls) | the host already has `nativeAvailable()` (library loaded); the lane flag means library loaded AND lane ABI present |
| `DROP_LINE` NATIVE_LOADED field, `DROP_METHOD loadOptionalLibrary`, `DROP_METHOD` constructor (11 lines) | the donor's opt-in `System.load(property)` harness and its duplicate private constructor | the module has one loader (`NativeLibrary`, `System.loadLibrary("m3_collections_mask")`) |
| `REWRITE \bNATIVE_LOADED\b -> NativeLaneKernel.AVAILABLE` (11 lines) | every native/Java choice and every NativeRequired refusal keep their shape | the host's lazy ABI-probe pattern |
| `INSERT_BLOCK` (Java 15 lines, C 9 lines) | the `NativeLaneKernel` holder (`NativeLibrary.AVAILABLE && laneKernelAbiNative() == 1`) and its C probe | the host's own pattern, 6th instance (`NativeRange`, `NativePositions`, `NativePositionMasks`, `NativeWindowMasks`, `NativePositionPage`) |
| `JNI Java_com_synexia_m3_doops_EntrylessLaneKernel_ -> Java_jdk_m3_collections_M3IndexMaskCompactor_` | the five exported symbols | host class |

`PROVENANCE.tsv` pins donor commit, blob, sha256 and URL, target sha256, the host pin (`host_sha256` = target minus
block minus the recipe's imports = the `BASE` row, `base_match=TRUE`), the counts and `body_match=TRUE` (read back from
disk: the lines between the anchors are exactly the donor body after the declared rows). The recipe is idempotent
(`--check` after apply: `changed=0 provenance=UNCHANGED`) and `javap -public` shows the 19 donor public members (names
mapped) inside the host's 36. The three oracles are relocated as new files (`FILE` rows): `M3IndexMaskLaneKernelProof`
(the donor's standalone differential main), `M3IndexMaskLaneKernelTest` (JUnit wrapper) and
`M3IndexMaskLaneLazyResultTest`. `M3IndexMaskLaneKernelDump` is the reflective dump driver of the differential gate
(test scope, not a test).

Donor lineage: develop `ac5feec00a6d6206194b3ba954c6f738aae07126` main-path blobs are the newest commits' blobs
(Java `04bebdfbf3b` lazy results, C `b1b4f801a94` shared-scanner full-span elision, proof `e6638372643`); the 2026-11-06
align commit `87be6d7965b` resolved the two-sided C path to that blob, so unlike the posting kernel of PR #2512 there is
no regression; the three retained variants in the tree are older (`pr15403` pre-full-span, fails the develop proof;
`pr15593`/`pr16154` the m3scan-convergence comment variant, same results). See the `SKIP` rows.

## Not unified (NEC rows, decided later, not here)

- Page packing: the donor `scanPageInto` packs written high / next low; the host's `selectPositionsPage` packs next high /
  written low (the plan row's "identical packing" note is wrong). The donor body is kept; `pageWritten` / `pageNext`
  disambiguate.
- `LIVE = 1` (bit 0) versus the host's caller-supplied required/excluded masks.
- Full-span elision on the host's existing reads: the host `.c` has 14 `Get*ArrayRegion` reads (not 12): 10 byte-state
  reads already skipped when the mask is 0, 2 int and 2 long payload reads that copy the selected rows into the result
  and must happen. The donor guard is a predicate-lane elision and has no mechanical counterpart there, so no `RECIPE`
  rule is declared; the guard arrives only inside the appended kernel. Follow-up: none required.

## Gates (2026-11-06, see GATES.tsv)

| Gate | Result |
|---|---|
| 1 `mvn -f m3/collections/pom.xml -DskipTests package` (`-Xlint:all,-module -Werror`) | BUILD SUCCESS, 0 compiler warnings; `jdk.m3.collections` jar carries `M3IndexMaskCompactor$NativeLaneKernel` |
| 2 oracle | `M3IndexMaskLaneKernelProof`: `PASS cases=4034 native=false` (Java path; 4029 with JNI, the five UnsatisfiedLinkError cases are skipped); module tests 1631 run / 5 failures / 49 errors / 2 skipped vs base 1627 / 5 / 49 / 2, failing set identical (54, the Windows JNI-library baseline; `TEST_BASELINE_DIFF.txt`) |
| 3 differential (Java) | donor compiled from the com.synexia tree vs relocated: proof output IDENTICAL; dump over 100,000 seeded lanes (5,982,905 slots, 1,143,903 matches, 297,490 pages) IDENTICAL, digest `94a42bcf3a5360f471d03658c216f52ecd0ca0c257ef103da923eb578fa2f7b9` |
| 4 native (WSL: gcc 13.3, Temurin 21.0.13) | javac -h lane prototypes identical modulo class name; donor kernel and `libm3_collections_mask.so` (compactor + window sources) compiled with the donor's `-std=c17 -O2 -Wall -Wextra -Wpedantic -Werror` (also c11; 0 `-Wconversion -Wshadow` warnings); proof under `-Xcheck:jni` in default / `-Xint` / C1 against the donor library and the module library: 6 executions, all `PASS cases=4029 native=true`, 0 JNI warnings; native dump 20,000 ops identical on both sides and equal to the Java-path digest; cross-check `M3IndexMaskPositionPageProof` 67,980 (Java) / 67,988 (JNI) and `M3IndexMaskCompactorSelfTest` 57,344 checks JNI=true against the merged library |
| 5 scope | only `m3/collections/**` changes |
| naming | `jdk_naming.py apply --only m3/collections/`: 0 moves, 0 rewrites, 0 refusals |

Run everything: `JAVA_HOME=<JDK 21> MAVEN_REPO_LOCAL=<m2> FULL_TESTS=1 bash make/m3/collections/recipe/entryless-lane-kernel-20261106/verify.sh <com.synexia clone> ac5feec00a6d6206194b3ba954c6f738aae07126 <scratch>`;
on a Linux host `M3_LANE_NATIVE_GATE=1` adds gate 4 (`native-gate.sh <scratch>/differential <scratch>/native`).

## Follow-ups (not in this PR)

- `make/m3/collections/qualification/doops-entryless-receiver.tsv` pins `post_sha256` values of `M3IndexMaskCompactor.java`
  and `m3_index_mask_compactor.c` that master no longer has (the JDK naming v2 pass changed both before this leaf), so
  the `doops-entryless-receiver` workflow is red before and after this PR; its custody lane owns the reseal.
- Unifying the two page packings and parameterising the lane kernel's live bit are API decisions for a later leaf.
- The module has no CMake for `libm3_collections_mask`; the qualification scripts build it from the two sources. A
  `native-linux`-style build (as m3/arrays has) would let the module tests run the lane kernel's JNI branch in the test
  phase.

## Files

| File | Purpose |
|---|---|
| `m3/collections/src/main/java/jdk/m3/collections/M3IndexMaskCompactor.java` | host owner + the merged lane kernel members (anchored block, 324 lines) + `import java.util.Arrays` |
| `m3/collections/src/main/native/collections/m3_index_mask_compactor.c` | host kernel + the appended lane kernel (anchored block: 5 natives + probe) |
| `m3/collections/src/test/java/jdk/m3/collections/M3IndexMaskLaneKernelProof.java`, `M3IndexMaskLaneKernelTest.java`, `M3IndexMaskLaneLazyResultTest.java` | relocated oracles and JUnit wrappers |
| `m3/collections/src/test/java/jdk/m3/collections/M3IndexMaskLaneKernelDump.java` | reflective dump driver of the differential gate |
| `relocate.py`, `RELOCATION_MAP.tsv`, `PROVENANCE.tsv` | the relocation recipe, its mapping table and its pins |
| `differential.sh`, `native-gate.sh`, `verify.sh`, `GATES.tsv`, `RECIPE_MEMORY.tsv`, `TEST_BASELINE_DIFF.txt` | gates and their record |
