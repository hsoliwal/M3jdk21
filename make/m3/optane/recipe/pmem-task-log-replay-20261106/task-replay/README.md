<!-- Copyright 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/5ecfe052562cdd1a7a6570068654c7f813fd0f47/synexia-openrewrite-recipes/recipe-crates/m3-pmem-task-replay-20261102/README.md blob 3c60e2d72314; recipe make/m3/optane/recipe/pmem-task-log-replay-20261106/relocate.py v3 -->
# M3 PMEM task replay and Chrome atom recovery bridge — 2026-11-02

Canonical recipe `com.synexia.m3.PmemTaskReplay20261102`, class-backed by
`M3PmemTaskReplay20261102Recipe` and the existing `M3GpuToolJoinRecipe`.
Parent is `hsoliwal/com.synexia` **draft PR #15292** head
`0b39214577e58b5ae7639344141cf74aff531e8f`. The target is an additive,
source-sealed seven-file ABSENT-before image with exact SHA-256 postimages.

The code adds read-only receipt replay after an interrupted append, integrity
checks for SHA-bound candidate bytes, and deterministic recovery of the next
uncommitted reviewed atom. The integration smoke compiles both actual Java
module source owners **explicitly**, without adding a reciprocal Maven
dependency between the PMEM JNI owner and ChromeML. It uses a fake model
and fake RAM-backed region; no native symbols or product-source rewrites.

## Verification

```bash
bash m3-pmem-sdk/scripts/test-task-replay.sh
bash m3-pmem-sdk/integrations/chromeml/scripts/test-bridge.sh
./mvnw -B -ntp -f synexia-openrewrite-recipes/recipe-crates/m3-pmem-task-replay-20261102/pom.xml \
  -Dtest=M3PmemTaskReplay20261102RecipeTest test
```

On the local Java17-only toolchain the same source files were compiled
with `--release 17 -Xlint:all -Werror`; the first smoke verified 20,000
durable-receipt simulations and the bridge created 20,000 physical source
files, 20,000 **fake** atom model calls, 40,000 fake persist fences, and
20,000 successfully replayed receipts. Missing content, mutated content,
review drift, duplicate tasks, empty/oversized catalogues, partial writes,
cancellation and logical offsets beyond 2 GiB are fail-closed or validated.

The donor review names LeetCode, HackerRank, GeeksforGeeks and pinned PMDK
GitHub references by category and compares their algorithmic shapes with
the actual source/PMEM contract. **No challenge code was copied.**

| Gate | State |
| --- | --- |
| Seven exact Java/docs/script/catalogue SHA-256 source templates | PASS |
| Java17 strict compile and 20,000-receipt fake PMEM replay | PASS |
| Java17 strict cross-module 20k physical file/fake model bridge | PASS |
| JDK21 reactor/Maven and actual OpenRewrite SDK fixed-point tests | NOT_RUN |
| Real Chrome model and native PMDK power-loss recovery | NOT_RUN |
| 1.024GB synthetic Java source image, 20k file-level fake calls and PMEM replay | PASS, optional test-only |
| 1 GiB **real project** source, plugin provenance and full M3 product CI | NOT_RUN |

Keep **DRAFT/HOLD** until the source-built Java21, recipe SDK, hardware and
full repository acceptance gates pass. An LLM candidate is never automatically
promoted to a source change. Recipe/source drift must be corrected and resealed
at this class-backed Maven recipe before any deployment.

## 2026-11-03: exact resident **file-level** inference and PMEM replay qualification

The earlier 20k tiny-file test exercises `AiXSerialAtomPass`; this additive
test validates the separate **existing** `AiXSerialCandidatePass` path with
**one fake inference per full 50-KiB source file**. No replacement model runner,
source mutation or JNI/PMEM API is introduced. Run explicitly:

```sh
M3_JAVA_RELEASE=17 bash \
  synexia-openrewrite-recipes/recipe-crates/m3-pmem-task-replay-20261102/scripts/test-resident-file-bridge.sh
```

The script calls the existing cross-module bridge compiler/smoke, then strictly
compiles the crate-local `qualification/PmemChromeResidentFileBridgeSmoke.java`
with `-Xlint:all -Werror` and executes it with `-Xmx5g`. It creates **20,000
distinct, disk-backed synthetic Java files of exactly 51,200 bytes each** in
a uniquely allocated temporary directory. The canonical `AiXExplicitSourceImage`
holds **1,024,000,000 bytes** and immutable source fingerprints while the real
`AiXSerialCandidatePass` does complete reviewed-file admission before exactly
20,000 **fake** inference callbacks. The real `M3PmemTaskLog` and
`M3PmemTaskReplay` validate 20,000 candidate content addresses and receipts in
a **fake** byte-array `M3PmemRegion`, across 40,000 fake persistence callbacks,
without re-inference or source promotion. Source files are removed after the
test; no live product source or seven sealed recipe targets are modified.

**PASS, three successive complete executions**, Linux x86_64 / Java 17.0.20,
8-GiB cgroup ceiling, 5-GiB maximum Java heap. The same image root
`8bc238a6fdb107b0507571546a26f1586a19104648062d84117e03d6ee99612a`
and reviewed task root
`6e78b3eb73f51caee1be44a5c7d99a47970fb1620528f0f72e886de793b5d25f`
were observed across the independent runs. The third run **enforces** both
roots as immutable fixture assertions. That final run reported source creation
4,113 ms, snapshot load 4,465 ms, candidate pass 6,036 ms, receipt replay
131 ms, and process RSS after replay 3,840,204 KiB. The detailed
environment, timing and hashes are in
`qualification/RESIDENT_FILE_BRIDGE_20261103.tsv`. All 20,000 receipts
had candidate-only authority; `promotionAuthorized=false`.

**HOLD** Java21 Maven/OpenRewrite execution and fixed point, actual Chrome
inference calls, plugin quality data, physical PMDK/JNI power-fail validation,
real project source content and full hosted CI. The fake engine's ability to
accept 50-KiB input does not establish that the selected Chrome model or its
context window can do so. Actual source changes still require each file's
contract-safe Maven recipe and compiler/JUnit proof.
