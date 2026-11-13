<!-- SPDX-License-Identifier: Apache-2.0 -->
# mergeloss2-50-20261107

Merge-loss phase-2 leaf 50 (`CLEAN_0E.tsv` n=50, `ATOM_VERDICTS` rows 128, 129, 130 and 183). Origin `e47eeda7c05`
("m3: consolidate internal JNI exception atoms and safe exact-address mapping", 2026-10-20, PR #1553) replaced the
local `FindClass`/`ThrowNew`/`DeleteLocalRef` helpers of four m3/collections C owners with `m3_jni_throw_by_name`
from `src/java.base/share/native/libjava/m3_jni_util.h`. The retain merge `04fe6419c8b9` (2026-10-28, "Superset:
retain PR #1553") kept the older side of the four files. The first pass held the leaf (NEC/HOLD) because the header
was absent; java.base leaf 79 (#2677, `a180724a87ea`) has since restored it. This is the re-test.

Decision: **KEEP_LOCAL**, no source edit. The origin reached the header with a source-relative include,
`#include "../../../../../../src/java.base/share/native/libjava/m3_jni_util.h"`, not an `-I` path. No
m3/collections build has an include path into `src/java.base`, and 8 of the 18 workflow jobs that compile a leaf-50
file check out a sparse closure without it (7 of the 16 for `m3_index_mask_window.c`, including the 3 m3jdk21
workflows that build it with `m3_index_mask_compactor.c`; 1 of 2 for `m3_long_index_path_masks_jni.c`). Another 3
jobs seal the current bytes (2 blob pins on `m3_index_mask_window.c`, 1 recipe template on
`m3_long_index_path_masks_jni.c`). The candidate builds clean in the full tree, with the same exports and the same
exception behaviour. In the sparse closure it fails with `m3_jni_util.h: No such file or directory`. The local
helpers are cold-path exception atoms in the header's own JNI operation order, not duplicated fast paths. Evidence
is in `DECISION.tsv` and the numbers are in `GATES.tsv`.

| file | content |
|---|---|
| `DECISION.tsv` | per file: decision, cited commits, evidence, re-run condition |
| `apply.py` | recipe MERGELOSS_RESTORE v1 (from `mergeloss-20-20261107/apply.py`): the pinned CANDIDATE, origin hunks byte-for-byte plus one `//FLLDCIM: MRL` anchor per file; before/after sha256 pins, refuses drift, idempotent, `--check`; refuses to write into this repository while `DECISION.tsv` holds the leaf (use `--root <copy>`) |
| `EDITS.tsv` | file, before/after sha256, leaf, origin commit, dropping merge, rule (written by apply.py) |
| `closures.py` | which workflow jobs compile a leaf-50 file, their checkout, `-I` list, whether the header is in the checkout and whether they pin the file's bytes (`git show`, no checkout) |
| `CLOSURES.tsv` | closures.py output at master `a180724a87ea` |
| `native-gate.sh` | WSL gcc 13.3 / JDK 21 gate: 20 compile/link combinations x {incumbent, candidate} x {full tree, sparse closure}, `nm -D`, javac -h prototypes, -Xcheck:jni throw differential, existing self-tests and contracts |
| `oracle/` | `M3Mergeloss50JniThrowDifferential`: every JNI export of the four owners over a seeded argument lattice, one transcript line per call (return value or exception class and message, post-call array hashes) |
| `GATES.tsv` | gates with numbers |

Re-run condition: every closure that compiles one of the four files carries
`src/java.base/share/native/libjava/m3_jni_util.h` (or the module gains a declared include path to it), and the 2
blob pins plus the long-path template are re-sealed in the same change. Then
`python -I apply.py` is the landing. Moving the helpers onto the in-closure atom
`m3/native/include/m3_jni_core.h` `m3_jni_throw_new` instead would add a pending-exception check that the origin
helper does not have. That is a separate leaf with its own behaviour review, not this restore.
