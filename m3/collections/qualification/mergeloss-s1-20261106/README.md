<!-- SPDX-License-Identifier: Apache-2.0 -->
# mergeloss-s1-20261106

Case 05 of the M3jdk21 merge-loss audit (superset/retain/ancestry/compiler-accepts merges that dropped already-landed
production code still absent on master). The ancestry merge `e913ab890566` (2026-10-17, "preserve remaining
2026-10-17 M3JDK21 receiver heads", parent 1 lost) dropped what origin `ebbfcbdf93aa` ("receive hash-sealed long-index
path-mask Java/JNI extension") added to `M3IndexPathMasks` and `m3_index_path_masks.c`: `selectLong`,
`selectLongJava`, `selectLongNative`, `validateGeometry`, the shared int/long JNI core `m3_select` and the export
`Java_jdk_m3_collections_M3IndexPathMasks_selectLongNative`. This is why #2517 held `M3IndexPathMasksLongSelfTest` and
`M3IndexPathMasksLongTest` and why census2 P2-057 was NEC.

| file | content |
|---|---|
| `DECISION.tsv` | per leaf: decision, cited commits, evidence (L1 RESTORE, L2 UNHOLD, P2-057 HOLD_CI_PIN) |
| `apply.py` | recipe MERGELOSS_RESTORE v1 (from `census-small-batch-20261106/apply.py`): hash-pinned anchored edits (L1) and byte-for-byte moves (L2); `--leaf` per commit, `--check` fixed point, refuses drift |
| `EDITS.tsv` | file, before/after sha256, leaf, decision, cited commits, rule (written by apply.py) |
| `M3PathMaskRestoreDigest.java` | differential driver: every public operation over seeded valid/invalid geometries, one SHA-256 per operation |
| `native-gate.sh` | Linux native gate: javac -h prototypes, gcc -Werror (+ UBSan build), nm -D, contracts Java/native x default/-Xint under -Xcheck:jni, native = Java digest, master = restored digest on the four int operations, 5 mutants |
| `tools/wsl-profile.sh` | native Maven profile run (`-Pm3-collections-native`, libm3_arrays by CMake as #2577) over a `git archive` of one commit; testcase table |
| `tools/method_diff.py` | per-method `javap -c -p` comparison, constant-pool indices masked |
| `GATES.tsv` | gates with numbers |

Mapping (every hunk MAPPED by member name; master evolved both owners after the merge): the long members reuse
master's `validateShape` through a row-size overload (`validate` -> `validateGeometry(int rowSize, ...)` ->
`validateShape(int rowSize, ...)`; `validateShape(int[] rows, ...)` keeps its null check and delegates), the split the
retained #1294/#1368 twin carries. In C, `m3_select` is master's uncached `selectNative` body generalised over int[] and
long[] rows; the cached stage path (`m3_select_cached`) stays int-only, so long rows take the origin's reference loop.
Existing behaviour: 13 of 15 pre-existing methods bytecode-identical (javap -c, constant-pool indices masked);
`validate` and `validateShape(int[])` changed only by the delegation; the digest of the four int operations is
byte-identical between master and restored classes and libraries.

Replay: `python -I apply.py --check` (exit 0, changed=0). Native gate (Linux, JDK 21, gcc):
`TMPDIR=<tmp> bash native-gate.sh <out> <head m3/collections> [<master m3/collections>]`.

Open item: P2-057 (crate `m3-long-pathmask-bridge-20261018`) now applies cleanly and is behaviour-identical, but
`.github/workflows/m3-long-index-path-jni-20261018.yml` pins `M3LongIndexPathMasks.java` (blob `278f63e3bdc6`) and
compiles it without `M3IndexPathMasks.java`; it needs that workflow re-pinned in the same change (outside this scope).
