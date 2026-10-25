<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3File immutable byte-source generations — candidate

`M3FileSource` adds a bounded, immutable source image for parser consumers **without replacing** the original `M3File`, `M3FileDeltaLedger`, watcher, stream observer or build-impact planner. It uses the existing `M3File.observe(maxBytes, progress, observer)` callback for one checked provider-owned source observation, and cross-checks the observation fingerprint against captured bytes. No second filesystem tree, independent file provider, or mutable duplicate ledger.

The source image retains raw bytes, SHA-256, CR/LF/CRLF byte-line starts, and a lazy sparse strict UTF-8-to-UTF-16 converter. Invalid encodings, split scalar boundaries and surrogate splits are rejected rather than silently changed. Copy-on-edit creates an independent immutable generation with exact old-to-new coordinates outside replaced spans. This is not mmap, structural incremental parse reuse, zero-copy sharing between file and AST, an atomic OS snapshot, or a universal charset decoder.

**Provenance:** Synexia draft PR #13397, canonical `portable-ast-file-20261025` dataset. Source-only JDK21 fixture checks passed with a test-only M3File compatibility shim; real target M3File module, Python verification contracts, Maven reactor and installed JDK/JNI gates remain **NOT RUN / HOLD**.
