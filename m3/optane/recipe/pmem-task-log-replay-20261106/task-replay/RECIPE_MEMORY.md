<!-- Copyright 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/5ecfe052562cdd1a7a6570068654c7f813fd0f47/synexia-openrewrite-recipes/recipe-crates/m3-pmem-task-replay-20261102/RECIPE_MEMORY.md blob 95755c4f32ff; recipe m3/optane/recipe/pmem-task-log-replay-20261106/relocate.py v3 -->
# M3 correction memory — exact source and candidate-bound PMEM replay

- Date/parent: 2026-11-02, draft PR #15292 head
  `0b39214577e58b5ae7639344141cf74aff531e8f`.
- Exact target: seven ABSENT-before source atoms emitted from resource templates
  by `M3PmemTaskReplay20261102Recipe`; partial images and changed hashes refuse.
- Existing `M3PmemTaskLog`, `M3PmemRegion`, `AiXSerialAtomPass`, Maven module POMs,
  JNI API and contract/interface rules stay source-exact. The separate smoke
  is explicitly source-compiled across modules, not a product dependency.
- Crash nuance: append persist failure is **ambiguous**, so reopen the log and
  reverify the committed contiguous prefix. Never assume a failed write was
  rolled back, and never infer again for an already committed receipt.
- Receipt is only an integrity pin; replay additionally requires the exact
  result bytes in an immutable durable candidate store. Failed/skipped statuses
  also require content-addressed result bodies. The complete reviewed task
  catalogue, including source/contract/recipe identities, is authoritative.
- Local proof: Java17 strict compilation, 20k fake PMEM persist/reopen records,
  20k actual tiny filesystem source files/serial fake Chrome callbacks,
  20k receipt checks, tamper and drift failure oracles, >2 GiB offset arithmetic.
- 2026-11-03 follow-up: added **qualification only**, reusing the canonical
  `AiXExplicitSourceImage`, `AiXSerialCandidatePass`, `M3PmemTaskLog` and
  `M3PmemTaskReplay` with 20,000 physical 50-KiB synthetic Java files, 1,024,000,000
  resident source bytes, one fake candidate call per file, 40,000 fake persist
  fences, and exact 20,000 receipt recovery. Three full Java17 `-Werror`
  runs PASS. The stable image root is
  `8bc238a6fdb107b0507571546a26f1586a19104648062d84117e03d6ee99612a`
  and stable reviewed-sequence root is
  `6e78b3eb73f51caee1be44a5c7d99a47970fb1620528f0f72e886de793b5d25f`.
  The third test run asserts both. The final qualifier SHA-256 is
  `e4a0303cdc51ae026bc22d121c55e9995c3018fd46f5cc6dff3b3bafc2afa4c4`;
  the opt-in script SHA-256 is
  `e0e2ff99d9c4c364ff3c8de4ca5a870d6c8d3f1f5526a2ef3faea0a8c7a924b1`.
  Evidence: `qualification/RESIDENT_FILE_BRIDGE_20261103.tsv`. No existing
  seven source-sealed product postimages, Maven POM, JNI ABI or interfaces changed.
- Boundary correction: no auto-resume scheduler, atom replacement, Chrome
  calls, JDT source transformation or candidate promotion in the replay API.
- Remaining: Java21 Maven/OpenRewrite SDK, PMDK hardware power-fail behavior,
  actual model, plugin quality evidence, **real project** source images,
  context-size admission and full M3 CI. Synthetic 1.024GB source residency
  and file-level fake-call recovery are now locally tested, not product-qualified.

On a failure correct the Maven recipe/source postimage and retain failure
receipts. Do not hand-fix individual emitted source files or merge on the
strength of in-memory test doubles alone.
