# Exact source transport preflight — M3JDK21 recovery + SimHash (2026-11-04)

STATUS: isolated Git source custody / NO MASTER OR PRODUCT PROMOTION / HARD HOLD.

## Frozen original inputs

- Shared standalone-M3JDK21 master base: `172e39d5e69a8f7775c25ad1ce2bfaf2bf5d1246`.
- Existing receiving draft PR #2392 head: `000299bb3b4d35d4c638d820e02beb9a22865f9b` (30 ADDED / 0 MODIFIED / 0 DELETED from common base).
- Incoming draft PR #2407 head: `e6782bb9f1ebb134a8a00025bd9c46a905d6d6c6` (3 ADDED / 0 MODIFIED / 0 DELETED from common base).
- GitHub official changed-path filename intersection: **empty**.
- Each original head is one commit ahead of the same frozen base with zero commits behind.
- New receiver branch: `m3/frozen-source-recovery-simhash-20261104-gpt6` created at exact #2392 head.
- No original donor ref, master, release or runtime branch may be rebased, force-updated, squashed or deleted.

## Authorized source-only transport

Create a new non-draft transport PR (distinct from original draft product PR) with head `m3/indexdb-chunk-simhash-20261104` and base this isolated branch; require exact `e6782bb9f1ebb134a8a00025bd9c46a905d6d6c6`, GitHub `merge_method=merge`, and a true two-parent merge. First-parent delta MUST be exactly 3 new paths, 0 modifications/deletions/renames; verify exact original Git blob SHA on every incoming path and stage ref. Post-merge documentation only after readback.

Retain canonical recipe/test source from #2392, and retain #2407 Java postimages/tests exactly. This source-only integration does not claim full M3JDK21 standalone JDK behavior, any live replacement or production feature activation.

## Gates NOT RUN / HOLD

Canonical com.synexia OpenRewrite source seal, actual Recipe.run/Result.getAfter, fixed point/refusal, Java21/JUnit, JNI C17 ABI/JVMTI/JMM/GC, standalone JDK build, JMOD/JTReg/JCK, provenance/licensing, benchmarks and exact-head hosted CI. Do not merge this stage into master until green.

`ALL_PRS_MERGED=false`; `PRODUCTION_QUALIFIED=false`.
