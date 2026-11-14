<!-- SPDX-License-Identifier: Apache-2.0 -->
# Recipe durable-extent-store-20261107 (RELOCATE_DURABLE_EXTENT_STORE v1)

Private-repo scout rank 3 (`S:/m3-scratch/eb-conv/scout-private/PROPOSED_LEAVES.tsv`): the durable extent store of
the first-party donor `hsoliwal/com.synexia` `synexia-storage-sdk` (develop `0616288bfa7d`; all five files come from
one commit, `29621ea829fb` 2026-10-31), relocated into the existing module `m3/file` (module `org.m3jdk.file`, exported
package `jdk.m3.file`), recipe-first.

| Owner | Contract |
|---|---|
| `M3ContentExtent` | source-bound byte span (source id and SHA-256, `[byteStart, byteEnd)`, content SHA-256, logical kind); `extentId` = SHA-256 of sorted-key canonical JSON; `validateContent` (exact length, then SHA-256; `ContentTamperedException`); `ContentExtentManifest` (deterministic order, duplicate refusal, root) |
| `M3DurableExtentStore` | single writer (`writer.lock`); `write` (immutable key) / `supersede` (existing key): verify, write + force a content-addressed blob, atomic rename, then write + force a complete sorted index sealed with its SHA-256 and publish it by one atomic rename over `index.m3`; `read` (exact length, SHA-256, fresh array from a READ_ONLY mapping), `metadata`, `keys(prefix, limit)`, `manifest`, `verifyAll` |

**Durability is limited to the atomic rename.** Directory fsync is attempted and its refusal ignored (Windows always
refuses); there is no portable power-loss guarantee and no power-loss qualification. The crash gate below kills the
writer process, it does not cut power.

## Decisions ([DECISION.tsv](DECISION.tsv))

* Owner `m3/file` (D1): it already owns durable file images published by atomic move and atomic per-key storage;
  `m3/optane` excludes a persistence protocol, `m3/fs` owns MIndex custody with another contract, `M3BlockStore` is an
  in-memory viewport cache. No new module, no descriptor or pom change.
* Read backend (D2): the donor reads through its `StorageStore` facade (AUTO resolves to MAPPED_FILE). The facade
  family is HELD (H1, NEC N2 with the PMem region core, whose spans must be non-empty and are not exact-length); the
  default path is restated on `FileChannel` by three `READ_BACKEND` SUBST rows; `open(Path, StorageOptions)` is HELD.
* fs-pmem plan rows 2-5 (pmem-region-core, source-image, gram-index, task-log-replay) are on master (D3); the leaf
  needs none of them, so nothing is HELD for them.
* Schema ids follow JDK naming (D4): `synexia.content-extent.v1` becomes `m3.content-extent.v1` (and the three
  siblings), emitted by the recipe so `jdk_naming apply` is a fixed point. Extent ids differ from the donor's; index
  files and blob names do not. An id bridge is HELD (H3).
* Zero-copy mapped slices are the synexia-mllm oracle's API, not the donor's (D5); HELD (H2).
* NEC (no equivalence decided, both kept): N1 `M3BlockStore`, N2 PMem region core, N3 `M3SnapshotStore`, N4
  `M3FileSystemStore`, N5 `M3FileSourceFactsStore`, N6 `M3PmemTaskLog`, N7 `M3PmemSourceImage`, N8 `M3Hash256` /
  `M3ContentId`, N9 range identities.
* FOSS ([FOSS_REUSE_DECISION.tsv](FOSS_REUSE_DECISION.tsv)): java.nio DIRECT_IMPORT; openclaw/fs-safe, JGit LockFile,
  Lucene FSDirectory are architectural references; MapDB / Chronicle Map / LMDB rejected (own logs, external).

## Files

| File | Role |
|---|---|
| `relocate.py` | RELOCATE_DURABLE_EXTENT_STORE v1 (derived from delta-generator v3: DROP_BLOCK and HELD rows) over `RELOCATION_MAP.tsv`; writes `PROVENANCE.tsv` (blob, sha256, body_match, body_match_renamed); `--check` |
| `apply.py`, `EDITS.tsv` | hash-pinned owner edits: `m3/file/verify.py` (three JUnit classes join `maven_junit_sources`), `m3/file/README.md` (one section) |
| `differential.sh`, `differential/` | dumps (`M3ExtentDump`), crash gate (`M3ExtentCrash`), the relocated standalone proof, javap, mutants (`MUTANTS.tsv`) |
| `ORACLE_CLASSIFICATION.tsv` | 38 synexia-mllm scenarios classified; the MATCH / PARTIAL ones are `M3DurableExtentStoreOracleTest` |
| `verify.sh` | recipe, naming, package and differential gates in one run |
| `GATES.tsv`, `RECIPE_MEMORY.tsv` | gate results and lessons |

## Run

```sh
python -I m3/file/recipe/durable-extent-store-20261107/relocate.py --donor-git <com.synexia clone> \
    --donor-rev 0616288bfa7d91eb0abb754c7a29707f10318752 [--check]
python -I m3/file/recipe/durable-extent-store-20261107/apply.py [--check]
JAVA_HOME=<JDK 21> bash m3/file/recipe/durable-extent-store-20261107/verify.sh <com.synexia clone> \
    0616288bfa7d91eb0abb754c7a29707f10318752 <scratch> <junit-platform-console-standalone jar> [repo [tail]]
```

Gate results: [GATES.tsv](GATES.tsv); lessons: [RECIPE_MEMORY.tsv](RECIPE_MEMORY.tsv).
