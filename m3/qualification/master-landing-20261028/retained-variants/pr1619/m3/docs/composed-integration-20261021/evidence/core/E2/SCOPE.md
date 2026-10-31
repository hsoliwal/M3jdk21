<!-- Copyright 2026 Hitesh Soliwal and contributors; SPDX-License-Identifier: Apache-2.0 -->
# E2 rebuilt-image evidence

The same 28-test jtreg selection completed with **26 passed, 2 failed, and no
timeouts or other errors**. The harness exit was 2 because the two inherited
failures remain. It executed 67 main actions: 65 passed and 2 failed.
`JTREG_E2.json` retains all 166 build/compile/main action records, every recorded
stdout and stderr stream, and the per-test transition from E1. This is an
actual rebuilt-image run; it does not use the isolated runtime class patches
from the E1 receiving experiments.

| Test | E1 | E2 |
| --- | --- | --- |
| Nullable concat reference DAG | Assertion failure | Passed, 23 checks |
| Concat length overflow | 480-second timeout | Passed in both declared compact modes, 111 checks each |
| Pool metadata bounds | Admission-refusal assertion failure | Passed all three native-budget modes, 6660 checks each |
| Canonical DAG coordinate/collision fixture | Passed old fixture | Expanded fixture passed both compact modes, 59 checks each |
| Char-array composition | Missing private method | Same missing private method; unchanged fixture |
| Pool bounds | Accounting assertion failure | Same accounting assertion failure; unchanged fixture |

The char-array and PoolBounds failures retain the dispositions in
`../DIAGNOSTICS.json`. Neither is hidden, excluded from the selected suite, or
weakened to make this checkpoint pass. All 23 tests that passed E1 also passed
E2. The repository's broader String phase checker is separate; the earlier
failure remains recorded, and no phase-completion claim follows from E2.

## Exact source and build scope

`SOURCE55_HASHES.tsv` names all 55 received source paths relative to parent
`a115db5d8d9a53c20bf6f6880286a967a80a8dda`. They comprise the E1 49-file
checkpoint and seven selected receiving paths, one of which updates an
already changed Pool owner. Every row was compared against the actual
receiver bytes used for the rebuild. E2 is bound to these source seals;
the enclosing publication receipt supplies the final target/canonical commit
names after publication. The E1 target and canonical commits are retained as
the preceding epoch, not misrepresented as E2 commits.

Both receiving build logs are preserved under `build/`. The first build
compiled java.base but failed during jlink because the image destination
already existed. Renaming that directory allowed the same make command to
complete. No source change or warning override was used for the retry.
`BUILD_RECEIPT.json` explains the retained stale failure-log excerpts.
`INSTALLED_BYTECODE.json` includes the relevant complete Factory and Pool
method excerpts from the rebuilt image.

## Capture and immutability

The original E1 packet's 13 files remain byte-identical, with manifest SHA-256
`0bc5395622babd5c0cff4ff2770d6359b14c3645e3a42669f5b85c09211f468d`.
E2 is an append-only subpacket. Environment/configuration/rerun dumps and
process identities are omitted; task/tool paths are normalized using the
same tokens documented in E1. `PACKET_MANIFEST.json` binds original input
hashes, source seals, capture counts, and public output hashes. No binaries or
credentials are included, and no numerical performance ratio is inferred
from the timeout-to-completion transition.
