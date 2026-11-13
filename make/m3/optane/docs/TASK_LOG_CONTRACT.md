<!-- Copyright 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/5ecfe052562cdd1a7a6570068654c7f813fd0f47/m3-pmem-sdk/docs/TASK_LOG_CONTRACT.md blob 3a6ea9ec2255; recipe m3/optane/recipe/pmem-task-log-replay-20261106/relocate.py v3 -->
# PMEMSDK source-task receipt log — additive Java/JNI companion

`PmemTaskLog` uses the existing `PmemRegion` JNI mapping, read, write and
`persist` ABI. It adds no native symbols, independent scheduler, file crawler,
AST parser, model runner or automatic source edit authority.

Canonical consumers and owners remain:

- `synexia-fileindex`: repository inventory and file identity catalogue.
- `AiXSourceSnapshot`: strict UTF-8 per-file source sealing.
- `ChromeMlTaskService` / `Chrome2ApiMethodSplitPlanner`: bounded candidate
  tasks and method-level split proposals; the actual Chrome session context
  budget is checked by the browser adapter before invocation.
- `synexia-openrewrite-recipes`: the sole source-changing recipe authority.
- Quality plugins and compiler/JUnit logs: external diagnostics, never a
  substitute for source bytes or compile/runtime proof.

## Record layout and durability

One 144-byte fixed record holds `M3TL`, version 1, an outcome byte,
task/source/result SHA-256 digests, a SHA-256 integrity digest and an
`M3COMMIT` marker. Candidate output bytes live outside the log in the existing
content store. Each task should bind its repository/file/atom identity, prompt
contract, diagnostics, model identity and context parameters into its task
digest; `PmemTaskLog` validates exact SHA-256 syntax, not those upstream claims.

An append writes and persists a zero-marked body, then writes and persists the
commit marker. Reopen scans in ordinal order and verifies each committed body;
the next nonempty slot with a zero marker is an incomplete tail available for
replay. A committed checksum, header, status or marker mismatch refuses the
journal. A gap followed by any nonempty slot also refuses the journal: committed
entries may never be silently skipped. Any error during body/marker writing or
persistence poisons that log instance for further appends, because the durability
outcome is unknown; the caller must reopen, verify and only then retry.

The caller pre-sizes a dedicated mapping and grants **one log owner** exclusive
write authority. The class serializes operations on that owner; it cannot
coordinate independently created wrappers around the same region. Mapping
closure, replacement and lifetime remain with `PmemRegion` and its existing
Java/JNI leases. Never treat a candidate report as an admitted rewrite or
interpret successful POSIX `msync` as actual Optane power-loss proof.

20,000 candidate receipts occupy exactly **2,880,000 bytes** (excluding the
existing filesystem and mapping overhead). On a 32 GB DRAM machine, keep the
source corpus and hot index in RAM under a separately measured budget; reserve
PMEM for durable task identities, output hashes and recovery. Files above the
model's context window are atomized using the existing JDT/OpenRewrite owners.
The number of model calls is workload-dependent and is not necessarily equal
to the number of files.

## Qualification

`bash m3-pmem-sdk/scripts/test-task-log.sh` runs the true Java interfaces with
a deterministic test mapping. It checks 20,000 ordered appends, two persist
operations per receipt, reopen integrity, an interrupted write, committed
corruption refusal, poisoning on unknown persistence outcome, orphan record
refusal, cancellation, and long offsets above 2 GiB. This is a
Java contract test, **not** a native JNI execution or physical PMEM durability
test. The accompanying Synexia Maven/OpenRewrite leaf must prove absent-before
source generation, fixed point and drift refusal against exact source SHA-256.

Hardware promotion still requires source-built Java 21/JNI, JMOD/jlink,
PMDK/FSDAX/DEV DAX, actual power-loss recovery, source and quality-plugin
integration, a representative 20,000-file Chrome AI run and benchmark receipts.
