<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Forward reconciliation before publication — 2026-10-18

The original recipe and proof-01/02/03 remain unchanged historical artifacts. New work is isolated under recipe-current and proof-current-*.

Latest inspected product base: f29b0afe7ad253cc0562bc8dfdd48fc1acec710e. Current Java blob 2053896cfc74192d7561478f6aca249eae87ca66; native blob aaa74686949de59b44b363741ef1c414fe3e95d0. These add selectPositions and two JNI exports; they must be preserved byte-for-byte outside the already selected counting atom. No wholesale replacement with earlier outputs is authorized.

Adapt only the existing source-bound recipe preimages and the native replacement anchor so it cannot touch the newly added positions body. Extend the frozen probe to cover selectPositions, including missing-native and legacy-binary fallback. New public descriptors and all seven current exports must match before/after. The unchanged M3LongIndexProjection consumer is blob 09c10e2b4874f70ece8c1e4392a574db652945f7.

Qualify each source stage in order: patch, lint, compile, tests, runtime. Keep failures and old receipts; source drift must fail before mutation. Synexia owns the reusable recipe, M3JDK21 owns runtime output. No default-branch writes, rebase, deletion or phase advancement.

## 2026-10-21 current-source continuation


Event: `6c370040922eb26b5be64f1d2d43d93fdf7a6044d48915fbcef1246e9de57a41`. Canonical owner remains the existing Synexia crate and PR #12260.
Receiver is existing M3JDK21 PR #1280. The original 35-file source snapshot at
`a0f22da4f2bd953a116f124adbe7784f9e5e419d` and its historical receipts remain in Git;
the original `evidence/` receipts are not rewritten.

The original recipe refuses current master rather than overwriting its required/excluded
logical-position API and nine JNI exports. The existing long-prefix compaction atom is
already present, so the author removes only the two obsolete scratch-removal operations
and updates the prefix-loop match. The existing source-seal engine is unchanged. Java
derivation is unchanged apart from admitted preimage hashes. Current consumer fixtures
are real source owners, not API doubles. No production file is manually patched.

The original serial verifier passed 105 commands on the current preimages. The current
supplement passed 12 commands: 133,140 Java checks, 512 actual JNI cases, and three
seven-check current/legacy/missing-library capability modes on both original and generated
images. Twenty-one retained method bodies and generated JNI headers are byte-identical.
Java/JNI traces match at all serial stages; eight partial states, seven refusal cases,
two detected mutants, three binary pairings, and no-write fixed points passed.

The 1,024-row native probe retains its two state scans while payload Region reads change
from four to one for a single retained chunk, and four to zero when none match, for int
and long. This is a deterministic operation count, not a throughput or memory benchmark.

Maven was available and attempted. Its lifecycle could not resolve jar-plugin 3.3.0:
the offline cache lacked it, and the online attempt failed Maven Central DNS before any
build. Full module/JDK/image/jtreg/platform and resource qualification remain unrun.
The String -> arrays -> collections -> AST/compiler receiving sequence does not advance.

Reproduce from this existing crate using JDK21 and GCC with fatal UBSan:

```sh
python3 author.py --check
python3 verify.py --out /absolute/fresh/serial-proof
python3 reconciliation-20261021/verify_current.py --proof /absolute/fresh/serial-proof --out /absolute/fresh/current-proof
```

Before publishing the receiver, update the existing canonical Synexia branch, pin that
new commit and mirror its exact crate bytes into the existing receiver custody path.
Apply the generated resources with the existing SealReplay to the exact admitted current
preimages. A moving master must be rechecked by owner blobs; a source mismatch refuses.
See `reconciliation-20261021/evidence/` for actual raw logs and command exits.

## Canonical publication amendment

The original source PR #12260 is merged/closed. Preserve its historical crate. The
current-source successor is the distinct `current-master-20261021` dataset under that
same canonical owner, prepared for the already-open source PR #12712 at
`bad47f27aa06d011d297841eba11d935e0258be2`. No merged branch is resurrected and no new
PR is created. Receiver PR #1280 mirrors this exact dataset in its existing custody
path after the canonical source commit exists. This is a local prepared publication
plan; no remote write has been performed by the qualification worker.

Publication amendment event: `ec3240ca0c51dada1851704898e11a0c51e619af8ab228cb3a25ed3e37d38c21`.

## Final checked source epoch:55b44776

The final source comparison caught newly merged PR#1638's additive long required/excluded
selector. The earlier1e5056b proof above remains historical; it is not the publication
preimage. The current Java seal is `394211113f2e6c875521553700e9060a30906a9aaab4c755405402042832d12e`, Git blob
`d246ed575d109627d5e3c8397aa9ab388ff190ed`. C is unchanged. The same existing author was regenerated with this
new exact preimage and preserves the new method byte-for-byte.

Fresh serial qualification again passed105 commands. The current supplement passed14
commands:133,140 existing Java contract checks,512 actual JNI cases,136,397 independent
long-overload checks, and current/legacy/missing native capability cases on both before
and generated sources. All22 retained method bodies and JNI headers match exactly.
Current evidence is under `reconciliation-20261021/evidence/serial-v2` and `current-v2`.
Event:`f5e733441b3801e0e5f0b1aa73a7ef450beb9ffefb3709efc93c3e1148beef85`. Previous receipts and events remain immutable. No global phase advances.
