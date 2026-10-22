<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 is the integrated SDK; Synexia alone owns recipes

**Receiving repository invariant (2026-10-22):** M3JDK21 is one integrated Java 21 / HotSpot / JNI SDK, not an M3-on-JDK wrapper. `hsoliwal/com.synexia` is the exclusive convergence workspace: it contains algorithm catalogues, reusable transformation recipes, Maven/OpenRewrite execution engines, qualified source patterns and executed-contract evidence. M3JDK21 and public forks such as Eclipse SWT, Nebula, GEF and RWT **cannot contain recipe implementations or generation templates**. They receive **only actual executed source results**, compiler/test/JNI regressions, provenance and exact execution receipts.

## Admission of generated Java/JNI results

The workflow `m3-synexia-output-only.yml` checks the exact pull-request merge-base and current source hashes against receipts under `m3/qualification/synexia-executed-results/*.json`. The canonical verifier and tests live **only in Synexia**, pinned at immutable commit `5e27da4ab852de517439c744f44320206399ecc2`. Receiving CI temporarily checks out that verified producer revision; it is a **test-time source** and never becomes an SDK runtime or checked-in recipe. The workflow requires an explicitly configured `SYNEXIA_READER_TOKEN` with read permission to the private Synexia repository and its Actions metadata/artifacts. No token or insufficient evidence means **refusal**, not a bypass.

An accepted source-result receipt must identify the actual Synexia producer commit, named recipe, source-bound execution root, fixed point, completed successful Actions run and matching non-expired execution artifact. Every changed Java/C/JNI source file must match the receipt's **exact original and generated SHA-256**. An OpenRewrite template, catalogue row, a candidate diff or an isolated local experiment is **not** an executed recipe receipt and confers no promotion authority. Original public API, observable behavior, native ABI, source ownership and tests remain independently gated.

## Historical code under migration

Historical `m3/tooling/migration-recipes/` and `m3/collections/interop/recipe/` subtrees already exist in this SDK. They are **non-compliant legacy convergence plumbing**, not exceptions to the new architectural rule. Move them, their tests and dependencies into Synexia using non-destructive source-sealed migrations, then remove the old recipe files. Until that migration is demonstrated, the current SDK cannot be declared globally recipe-free. The new receiving gate forbids additions/updates under these paths and permits deletions as part of this migration. It does not delete files, alter the reactor or mask failures.

## Catalogue and contract policy

Review operations serially, per file and per meaningful behavioral atom. Prefer original M3JDK21 dynamic-array owners and physical positions, full-width state masks, backed sorted/filtered views, JNI native code where measured and qualified, and reusable first-party donor algorithms. Catalogue LeetCode/HackerRank/GeeksforGeeks **patterns** by problem category, prove compatibility and timing/space before adoption, record licences; do not copy unlicensed challenge solutions. Any transformation recipe and refinement occurs in Synexia only. Outputs go to the actual JDK or original public-fork owner with test results and authenticated producer execution receipts.

**Qualification limit:** The source-sealed local generation of this policy/workflow demonstrates only mechanical output custody. It does not prove that the full M3JDK21 reactor, canonical Maven/OpenRewrite plugin, HotSpot image, cross-platform JNI, SWT/Nebula/GEF/RWT rendering or GPU/theme/language regressions have passed. Keep unqualified receiving changes in draft.
