<!-- Copyright 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/5ecfe052562cdd1a7a6570068654c7f813fd0f47/m3-pmem-sdk/docs/TASK_REPLAY_CONTRACT.md blob e49a0c9bd83a; recipe m3/optane/recipe/pmem-task-log-replay-20261106/relocate.py v3 -->
# M3 PMEM task receipt replay — source and candidate reconciliation

`M3PmemTaskReplay` adds a **read-only Java** recovery verifier to the existing
`M3PmemTaskLog`/`M3PmemRegion` owners. It does not change JNI symbols or region
ownership, introduce a direct dependency on ChromeML, run a model, or edit a
source file. Promotion is always unauthorized by the returned record.

## Recovery preconditions and algorithm

1. Persist every model candidate in an immutable content-addressed store,
   keyed by SHA-256. Then append its receipt through `M3PmemTaskLog.append`.
   Each receipt already uses the two-persist, commit-marker ordering in the
   existing task journal. Keep a durable, separately reviewed catalogue of
   every task's contiguous ordinal, exact task SHA and exact source SHA.
2. **After an interrupted/ambiguous append, reopen** the existing PMEM log
   through `M3PmemTaskLog.open`; do not reuse a stale in-memory committed count.
   The existing log scanner recovers only the contiguous committed prefix and
   exposes an incomplete tail. PMEM ownership and durability remain the
   responsibility of its caller and the selected region backend.
3. Call `M3PmemTaskReplay.reconcile` on that reopened log with the complete
   immutable reviewed sequence, a `ContentReader` for persisted result bytes,
   and an existing cancellation/progress monitor adapter. The method validates
   each task SHA and source SHA against the reviewed catalogue and each
   content byte sequence against the recorded result SHA, then returns the
   exact next uncommitted ordinal. All result statuses, including FAILED and
   SKIPPED, require corresponding integrity-checked content bytes.
4. A separate catalogue/recipe authority may decide if/how to resume from
   `nextOrdinal`. This verifier **does not** itself schedule remaining Chrome
   calls; it never skips reviewed tasks automatically. Resume must also obey
   recipe, donor, behavioral-contract and interface preconditions from the
   `AiXSerialAtomPass` owner. Do not promote any candidate without independent
   compiler/JUnit and contract-preserving OpenRewrite/JDT verification.

The complete reviewed sequence is checked before any receipts. It must be
nonempty, contiguous, unique by task SHA, and within the journal capacity.
The result includes a domain-separated `reviewedSequenceSha256` over ordered
ordinals, task hashes and source hashes, plus status counts, `incompleteTail`
and `promotionAuthorized=false`. The SHA pin strings are lowercase 64-digit
hex; candidate content is capped at 64 KiB. A changed contract, source atom,
task sequence, missing/corrupt content, excess committed receipt or cancelled
progress monitor fails closed. A trusted store must persist result bytes before
the log receipt becomes committed; a result hash is not the result body.

The receipt has a fixed size of **144 bytes**: 20,000 entries require 2,880,000
bytes of region capacity, excluding application content storage, its metadata
and the in-RAM reviewed catalogue. A fake sparse region separately checks PMEM
offset arithmetic above 2 GiB. This is **not** proof of a real 1 GiB source
image, a 32 GiB workstation workload, or physical PMDK power-loss behavior.

## Module boundary and verification

The PMEM SDK remains its own `pom`/JNI owner and ChromeML retains its own
module/classpath. The independent test at
`m3-pmem-sdk/integrations/chromeml` explicitly compiles first-party Java
sources from both modules. No reciprocal Maven module dependency is added.
It instantiates 20,000 physical one-byte source files, executes 20,000 **fake**
serial inference callbacks via the real `AiXSerialAtomPass`, persists receipts
in a fake `M3PmemRegion`, reopens the log, and verifies recovered candidate bytes
without re-inference. The separate PMEM smoke simulates a partial commit,
missing/tampered results, review drift, empty/oversized manifest, cancellation
and logical offsets beyond 2 GiB.

```bash
bash m3-pmem-sdk/scripts/test-task-replay.sh
bash m3-pmem-sdk/integrations/chromeml/scripts/test-bridge.sh
./mvnw -B -ntp -f synexia-openrewrite-recipes/recipe-crates/m3-pmem-task-replay-20261102/pom.xml test
```

Both shell scripts default to `--release 21`; an explicit
`M3_JAVA_RELEASE=17` is only a local no-JNI smoke workaround. JDK21/Maven,
source-built OpenRewrite, actual Chrome ML model, PMDK region reopen/power-loss,
cross-process candidate store durability and product CI remain **HOLD** until
executed on the real repository/toolchain. These smoke tests cannot approve a
merge, even if all assertions pass.
