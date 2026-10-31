<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Parser-neutral source occurrence plane — candidate

`M3SyntaxTree` complements, but does not replace, the existing `M3ASTHierarchy`. The hierarchy continues to summarize proved L0–L5 file/package/module/project/repository facts. This additive source projection retains original source bytes and captures parser-specific kinds, named child fields, token/trivia/missing/error flags, and half-open **byte** spans in immutable primitive lanes. The `ForeignNode<N>` six-method callback is the entire transient parser-facing shape. `projectFragment` adds a synthetic full-file root for independently parsed fragments.

The grammar provider and its semantic bindings remain authoritative. UTF-16 offsets from JDT, JavaParser or similar parsers must be translated by a validated source-image codec; passing them as bytes is wrong. No foreign parser engine, red/green hash-consing, incremental reparse, native/JNI symbol or public JDK AST replacement has been added.

**Provenance:** Synexia draft PR #13397, source-sealed `portable-ast-file-20261025` dataset in the existing canonical OpenRewrite family. These product Java files are candidates received from exact original source templates; no recipe runtime or source is installed in this product. Standalone JDK21 fixtures have passed; OpenRewrite SDK evidence, Maven/JUnit and M3JDK21 installed image acceptance remain **NOT RUN / HOLD**.
