# Resume the MIndex-to-M3 migration

> **Whole-JDK scope note:** this file is the retained bounded MIndex/prefix tranche resume and preserves its dated evidence. For the current whole-JDK programme, start with [../../docs/whole-jdk-resume.md](../../docs/whole-jdk-resume.md), [../../docs/whole-jdk-work-packets.md](../../docs/whole-jdk-work-packets.md), and [../../docs/whole-jdk-subsystem-matrix.md](../../docs/whole-jdk-subsystem-matrix.md). Do not read the historical checklist below as whole-JDK completion.

**Status: INCOMPLETE. One algorithm specialization is implemented and locally tested. The whole MIndex family has not been inventoried or migrated. No complete JDK image was built in this tranche.**

Read `m3/docs/name-mapping.json`, this document, `COVERAGE.md`, and `ACCEPTANCE.md` before changing code. The existing naming map is the sole capability authority; reports are generated projections. Never infer implementation from a merged documentation PR or a placeholder mapping.

## Exact starting points

| Role | Revision | Meaning |
|---|---|---|
| Source develop baseline | `75fb1abaecb56969bea2914520bbe819f130632b` | Pinned before work; private repository |
| Source observed later tip | `ea855a6008ecbfbdc31b9cb3a9aaf828839e1a86` | Drift to reconcile, not silently repinned |
| Prefix donor PR #7526 | `73978088621dc70f5021befafa443f7e622b517e` | Source body actually inspected |
| Target master baseline | `d6390ea3bb348f0c22afdba0819ca4ec0e97970f` | Public repository; no PR #6 runtime code on this baseline |
| Prefix implementation commit | `3a8e21dcea32b97e869f3bdbb5817e1f9927ca4f` | Exact algorithm, tests and reviewed postimage |
| Tooling implementation commit | `35c2787e2222e9ce894f6255ca9a9c1c44c5a6c0` | Recipe, schema, validator, tests and local runner |

Target master was observed at `aceb243be751f327c8d59f6d2ebcbeb126db531b` when PR #16 was created. The candidate remains pinned to d639. Reconcile that drift before merge; do not overwrite newer work.

Publication branch: `m3/migration-pass1-prefix-evidence-20261002`, based on **master**, not the runtime experiment. Draft PR: https://github.com/hsoliwal/M3jdk21/pull/16. Final PR metadata/evidence commits descend from these code commits. Evidence is bound to exact file hashes; it is not a synthetic merge-revision or whole-tree build.

## What actually changed

`com.m3.algorithm.M3PrefixZ` projects exact UTF-16 prefix facts onto the existing sealed `M3StringPiece`. No new text storage, interner, flattening cache, JNI layer or compiler was introduced. The result retains only an int array and primitive facts. The budget is primitive array payload, not total retained heap. The existing P0 arena is explicitly **not an interner**; this port does not upgrade it to canonical shared/local storage.

`ExactFileRecipe` is a dependency-free Java implementation with a separate Maven descriptor. Reviewed source/postimage hashes, five target guards, bounded reads, path/symlink refusal, all-destination preflight, idempotence, cooperative locking and receipt-owned rollback are tested. It is not a cross-file transaction: crashes and noncooperating writes can require manual reconciliation. No source-transforming OpenRewrite/compiler recipe is claimed.

The naming map has 25 observed or pending records. One has exact local algorithm-test evidence. The remaining entries are explicit pending work; they are not an exhaustive capability count. The validator checks schema, artifact hashes, evidence inputs, lineage and review-only three-way classifications. It consumes existing Synexia `InventoryWriter` TSV rather than introducing another source scanner. Symbol resolution, complete dependency closure and continuous cross-repository enforcement remain open.

## Local evidence

`m3/migration/evidence/local-20261002-final/receipt.json` records the actual environment, commands, input hashes and output hashes. It covers five actual target dependency classes on the classpath, not a complete source checkout. Java 21 normal and interpreter prefix suites each passed 77,735 cases / 1,564,255 assertions. Focused view/regex/code-point suites each passed 68,106 assertions. The exact-file recipe passed 45 checks. Python schema/mapping tests passed 26 tests. No benchmark or retained-memory result was measured.

Maven is absent from the local execution environment; its lifecycle is unexecuted. Full repository/JDK/JPMS/native/Windows builds, complete source-owner reactor tests and independent review are unexecuted. GitHub status must be read separately; local receipts do not establish hosted success.

## Reproduce without replacing a JDK

From a checkout of this draft, using an existing stock Java 21:

```sh
python -m pip install -r m3/migration/requirements.txt
python m3/migration/run-tests.py --out m3/migration/evidence/NEW-UNUSED-RECEIPT-DIRECTORY
python m3/migration/migration.py validate .
python m3/migration/migration.py complete .
```

`validate` currently succeeds with `completion=INCOMPLETE`; `complete` must currently exit **2**. Do not suppress that failure or describe it as migration acceptance. The runner is for local execution and labels receipts accordingly. A future hosted wrapper must explicitly record the actual checked-out PR head and execution environment instead of reusing that label unmodified.

Maven execution, when a legitimate Maven installation is available:

```sh
mvn -f m3/migration/pom.xml verify
```

Exact source-bound prefix replay requires an authorized source checkout at the pinned donor revision and the guarded target files. Paths below are placeholders for those real checkouts, not network fetches:

```sh
java m3/migration/src/com/m3/migration/ExactFileRecipe.java check SOURCE_ROOT TARGET_ROOT m3/migration/recipes/prefix-z-v1.tsv
java m3/migration/src/com/m3/migration/ExactFileRecipe.java apply SOURCE_ROOT TARGET_ROOT m3/migration/recipes/prefix-z-v1.tsv
java m3/migration/src/com/m3/migration/ExactFileRecipe.java rollback TARGET_ROOT m3/migration/recipes/prefix-z-v1.tsv
```

Rollback deletes only equal additions owned by that plan receipt. A preexisting equal file is not acquired for deletion. Missing or modified receipt-owned files cause refusal. Review stale locks after a crash; do not automatically delete another invocation's lock.

The naming extension is replayed from the retained public baseline and the canonical current manifest, without a second migration registry:

```sh
python m3/migration/migration.py extend --base m3/migration/recipes/baselines/name-mapping.v1.json --extension m3/docs/name-mapping.json --expected-sha256 a28301f443314e6ece0e918d7d3af655fe127578380f15c9860a0db2702a5601 --output NEW-REVIEW-OUTPUT.json
```

This creates a review output and refuses modified existing output. Install only a reviewed diff; preserve target adaptations. The foundation recipe manifest's naming-file hash must stay synchronized.

## Next dependency-ready work

Run the existing `com.synexia.m3.inventory.M3InventoryMain` / `InventoryWriter` on authorized full source checkouts at the baseline and each relevant unmerged PR head. Preserve every row, including non-MIndex dependencies. Record producer revision, repository, commit, tracking ref, TSV SHA-256 and truthful scope completeness. Do not move a private checkout into the public repository to repair CI downloads.

Feed those receipts to `migration.py reconcile --inventory ... --inventory-receipt ...`. Reconcile every unmapped row; an empty result from partial search is not absence. The tool emits review decisions, not permission to mutate or a CI completeness pass. Next inspect the actual structural owner bodies, bridge adapters and the two conflicting `SubMIndexString` implementations. Preserve each public contract and identify the Java String UTF-16 route separately.

For each enhancement: compare against the last synchronized source baseline; identify mapping/dependency closure; classify API/format/semantic/performance changes; compare target against its recorded hash; preserve target-only adaptations; replay a reviewed recipe; test the exact candidate; update the mapping and evidence together. Retain stable IDs and tombstones for moves/splits/merges/deletions. Reverse ports require separate reviewed proposals.

PR #6 historical evidence is not this candidate's evidence. Its enabled interpreter suite still had two StringJoiner OOME expectation failures. JIT, JNI/JVMTI, GC, CDS, deduplication, serviceability and complete-image acceptance remain separate gates. Do not merge or install an experimental JDK without separate authorization.
