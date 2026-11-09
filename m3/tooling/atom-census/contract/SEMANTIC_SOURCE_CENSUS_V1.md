<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 -->

# Semantic Java source-atom census — M3JDK21

The original four-argument `M3AtomCensus` behavior and two TSV files are
unchanged for valid source input. The additive `--semantic` switch emits a
third `SEMANTIC_CENSUS_<repo>.tsv` file from the same javac AST traversal:

```text
java jdk.m3.tooling.atoms.AtomCensus --semantic <repo> <PATTERN_RULES.tsv> <out-dir> <java-source-root>...
```

The table contains `atom_id, repo, path, kind, owner, name, signature,
start_utf16, end_utf16, source_sha256, pattern_id, public_contract_locked`.
Every Java file has a FILE_CONTAINER; Java method/constructor/native, class
and field declarations, initializer blocks, field expressions and lambdas are
source-bound to their exact UTF-16 spans and SHA-256. Identity is deterministic
for path/owner/kind/name/signature/occurrence; source moves require review.

Source roots may not silently skip syntactically invalid Java or symlinked
Java files/source directories. javac parse ERROR or unit-count mismatch refuses
before output creation. Any unrecognized pattern stays UNKNOWN_PATTERN.

**Scope limitation:** This is a staging semantic census, not the
`ATOMS.tsv`/`EVIDENCE.tsv` admission ledger. JNI_EXPORT, C/C++, assembler,
HotSpot AD, build and script atoms require their owning native scanners.
It does not assert coverage, correctness, JIT speed or public API admission.
Original LeetCode/HackerRank/GeeksforGeeks pattern categories are reused by
reference only; no third-party solver expressions are imported.

Canonical source-sealed implementation is in Synexia
`recipe-crates/m3jdk21-semantic-census-20261101`; do not edit JDK product
sources, public Java signatures, or JNI ABI.
