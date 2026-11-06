<!-- SPDX-License-Identifier: Apache-2.0 -->
# m3/collections lane kernel JNI exports restored (2026-11-06)

Census pass-2 group T1 (#2611, `qualification/census2-t1-20261106`) relocated the three lane contracts and recorded the
native half HOLD_NATIVE: the Java owners declare four natives of library `m3_collections_address` that master's
`src/main/native/collections/m3_long_address_kernel.c` does not export, so the contracts ran the Java paths and asserted
the required-native refusal. This directory restores the four exports and qualifies them.

| owner (Java) | native | contract |
|---|---|---|
| `M3LongAddressKernel.gatherSelectedInto(..., NATIVE_REQUIRED)` | `gatherSelectedNative` | `M3MaskedGatherContract` |
| `M3LaneAlgorithms.slidingMaximumIndicesInto(..., true)` | `slidingMaximumIndices0` | `M3LaneSlidingMaximumContract` |
| `M3LaneAlgorithms.partition3Prepared(long[], ..., true)` | `partition3Long0` | `M3LanePartition3PreparedContract` |
| `M3LaneAlgorithms.partition3Prepared(int[], ..., true)` | `partition3Int0` | `M3LanePartition3PreparedContract` |

## 3-way, then ADD rows

`THREE_WAY.tsv`: base `821876dfb56d` (the owner at 65456ef1e36; the retained variant's pre-rename blob is this blob plus
293 lines, 0 removed), ours `11657a88959a` (master), theirs `055eee1f1194` (the uncompiled retained variant pr1882 under
`m3/qualification/master-landing-20261028/retained-variants/pr1882/`). `git merge-file --diff3` conflicts in one hunk:
both sides insert includes after `#include <jni.h>` (ours `stdio.h`, theirs `stdlib.h` + `stdint.h`). The conflict is not
resolved by hand; the owner is master's file plus the declared rows of `ADD_ROWS.tsv` (two includes, the helpers and the
four exports as byte-for-byte slices of theirs, three CNPCDA anchor lines). `add_rows.py` (RESTORE_ADD_ROWS v1) re-runs the
3-way, checks the slice hashes, proves "result minus ADD rows == ours" and that the exports are ours + the four; `--check`
reports `changed=0` on the committed owner. Master's `gatherNative` (with its 2026-10-19 diagnostics) is untouched.

Registration is the static JNI naming the Java side already uses (`System.loadLibrary("m3_collections_address")`,
`Java_jdk_m3_collections_<class>_<method>`); the four `javac -h` prototypes equal the C definitions and gcc compiles the
owner through `-include` of both headers. The library has no CMake (it is built by gcc in
`full-java-jni-20261018/verify.py` and the workflows), so no CMake or pom change is needed.

## Gates

`native-gate.sh` (Linux) and `GATES.tsv`. Java opt-in is unchanged: `-Dm3.collections.lane.native=true` loads the lane
natives of `M3LaneAlgorithms`; `-Dm3.test.native.required=true` / `-Dm3.collections.partition3.native.required=true`
make the contracts require them. In native-required mode the contracts run additional native parity checks and drop the
refusal checks, so their counts are the donor crates' native counts (980,599 / 1,950,158 / 10,224), while the Java path
keeps the census counts (978,782 / 976,328 / 10,215).

## Open items

* `.github/workflows/long-index-gather-contract.yml` pins the owner blob (`git hash-object` = `11657a88959a`) and runs
  on changes of this file: it needs a re-pin to the restored blob (outside this leaf's scope: C file + this directory).
* m3/collections stays -Werror red on master (`M3PrimitiveRoaringIntSet.java:[35,14]`, #2561); the module gates ran
  with an uncommitted pom copy without `<arg>-Werror</arg>`.
