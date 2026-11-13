# M3JDK21 native viewport page — review custody, not runtime application

Copyright 2026 Hitesh Soliwal and contributors. SPDX-License-Identifier: Apache-2.0

M3JDK21 is the integrated product. The original CPU model and intrinsic viewport remain authoritative; this candidate adds no renderer, widget wrapper or canonical payload store.

## Canonical source and exact scope

Synexia PR #12855, commit `073aeea39f7561a4f050f895f5a6a6834d4a1ae5`, owns the reusable recipe and the complete seven-target dataset:
`synexia-openrewrite-recipes/recipe-crates/m3-ast-build-reverse-20261017/datasets/viewport-cpu-regions-20261021/`.

The three .txt sources here are inert, exact Apache-2.0 review snapshots of that canonical candidate. They do not enter a product build or replace the canonical recipe. The product preimage is `5246250c4a33386f34abccd0e5c2b279e86a3560`. RECEIPT.json binds all seven targets and records execution limits. Earlier bulk-copy sources and owner-candidate.patch remain historical, not patches to stack manually.

## New native page atom

The same package-private M3MaskViewportNative owner adds projectPage, matching the existing Java filtered-rank page contract. Full-window preflight records the first and last page positions. Native output scratch contains only the returned page; 128-int input chunks and one final writeback preserve bounded reads, aliases and failure publication. Dense second-pass reads need only page offsets. Sparse second-pass work spans the first through last page item. No pointer is retained across JNI.

Every existing Java descriptor and JNI prototype/export is preserved. Exactly one internal native page method/export is added; this is not a public API change. Old clients work with the new binary; an old binary still handles project but refuses projectPage with UnsatisfiedLinkError and no silent fallback.

## Executed finite proof

The exact final host verifier exited 0 after source admission, strict lint, Java21/C compilation, ABI comparison, Java tests and actual JNI execution. New page oracles passed 14,514 Java and 29,027 Java/JNI checks in mixed and interpreted modes. Existing 72,591/108,891 viewport and 36,424/37,384 sorting corpora also passed. The independent allocator/read-failure fixture passed 446,661 checks over 3,933 cases with ASan/UBSan and leak detection. Real JVM runs used -Xcheck:jni and fatal UBSan. Runtime stderr was empty.

For one million dense or sparse input rows and a 64-ID page, the instrumented real C code requested 256 bytes of native output scratch, plus fixed input buffers, and published once. This is an output-allocation bound, not total process memory or a throughput measurement. Full-window validation is still O(n). Inputs remain caller-synchronized. Repeated finite corpora are not additive independent coverage or universal equivalence proof.

Raw compiler/runtime logs and SHA-256 records are retained in the canonical NATIVE_PAGE_RAW_LOGS.json, with local command/process records in the downloadable evidence bundle.

## HOLD

Actual Maven/OpenRewrite SDK replay, fixed-point/refusal and receiver execution remain NOT_RUN. The hosted job was queued with no completed steps; local Maven/dependency acquisition and the development bridge were unavailable. No active product Java/C source has been changed. Do not manually copy these snapshots into runtime source. Apply only qualified SDK outputs through the existing receiver after its gates execute.

Whole filtered/sorted-provider contracts, installed JDK/HotSpot/jtreg, SWT/Nebula/GEF/RWT and GPU/theme/language/IME/accessibility integration, cross-platform/concurrency and comparative performance/heap/native-process gates remain pending. The global receiving phase is not advanced. No merge, rebase, force push or default-branch write is performed.
