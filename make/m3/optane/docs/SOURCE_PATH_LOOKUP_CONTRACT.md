<!-- Copyright 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
<!-- FLLDCIM: RELOCATE - donor https://github.com/hsoliwal/com.synexia/blob/5ecfe052562cdd1a7a6570068654c7f813fd0f47/m3-pmem-sdk/docs/SOURCE_PATH_LOOKUP_CONTRACT.md blob 1ae553ab5982; recipe m3/optane/recipe/pmem-source-image-20261106/relocate.py v3 -->
# M3 PMEMSDK bounded source-path lookup

Status DRAFT/HOLD. Canonical parent #14976 at `4fd459339e5031127a90d45e33566ab140299606`. Preserve the entire `PmemSourceImage.fileIndexOf(String)` external contract: sorted exact case-sensitive path ordinal, -1 on miss and NPE on null. All Java public APIs, JNI entrypoints, source snapshot bytes, format, digest and resource lifetimes remain unchanged.

Internal leaf only: immutable primitive `int[]` slots, power-of-two 2x load, zero empty and ordinal+1 stored. At most 16 probes; construction abandons index under adversarial collisions, lookup falls back to original sorted `Arrays.binarySearch` when unresolved. Require full String equality, never hash-only equivalence. Empty or small corpora (<64) stay binary-only. 20,000 paths need 256 KiB of slots. No new object per path, model runtime, JNI API or parser.

Source-sealed canonical Maven/OpenRewrite recipe has complete preimage, complete afterimages, exact SHA-256 and second-pass fixed-point tests. Recipe-first commit must precede product candidate commit. Qualification order: diff, lint, compile, JIT, `-Xint`, Maven/JUnit, JNI/JMOD, provenance. Unexecuted gates are HOLD, not PASS.
