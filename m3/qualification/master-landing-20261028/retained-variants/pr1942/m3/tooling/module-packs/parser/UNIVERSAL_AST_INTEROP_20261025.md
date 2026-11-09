<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Source-bound universal AST interoperability — candidate contract 2026-10-25

Original integrated M3JDK21 `jdk.m3.parser` module. SOURCE OWNER IS **M3JDK21**, not an M3-on-JDK wrapper. Canonical source-sealed authoring/recipe branch: `hsoliwal/com.synexia:m3-universal-ast-20261025`. This receiver is a **draft/HOLD** until the original parser/JMOD CI and canonical Maven/OpenRewrite recipe are executed and reviewed.

## Owner boundaries

Existing `M3ASTIndex`, `M3ASTIngress`, `M3JavacTree`, `com.sun.source.util.M3AST`, `M3ASTPC`, and `com.m3.file.M3File` APIs, behavior, JNI symbols, module exports and hashes are unchanged by this candidate. `jdk.m3.parser.M3File` is an immutable source snapshot with UTF-16BE and optional raw-byte SHA-256; `com.m3.file.M3File` is original provider NIO observation. Names do NOT justify merging these classes.

* `M3ASTFieldRoles`: immutable per-occurrence `short[]` + `int[]` sidecar with bounded dictionary, parent-first row order and no native node retention.
* `M3ASTProjectionCapture`: generic **already-parsed** native AST provider callbacks. No second parse, source I/O or semantic attribution. The source is always marked `SYNTAX_ONLY|SOURCE_UNVERIFIED`; unknown ranges stay unknown.
* `M3FileLineIndex`: source-bound CR/LF/CRLF UTF-16 line/column index; no guessed byte mapping.
* Two deterministic contracts: observed field roles/geometry, deep bounds, 200 text coordinate fixtures, 4096 role names and wrong-source refusals.

## Outstanding before promotion

Run canonical Maven/OpenRewrite receipt and fixed-point replay; actual M3JDK21 parser JMOD lint→compile→test→runtime (including existing consumers) and CI; source hashes at exact PR head; optional provider-specific adapters and native Tree-sitter/JDT/Jackson parity are **not** claimed here. Structural kind/range facts are not semantic/ABI facts. Build invalidation remains conservative for PARTIAL dependencies.

No third-party parser code, new JNI kernels, new public java.base types, branch rebases, merges, or changes to the original javac frontend are part of this PR.
