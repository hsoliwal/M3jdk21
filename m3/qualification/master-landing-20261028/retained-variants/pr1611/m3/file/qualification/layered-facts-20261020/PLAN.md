<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3File layered fact receipts

M3File does not become an AST/parser/regex owner. One physical file read feeds any number of
M3FileFactObserver implementations. Each layer publishes only a named SHA-256 receipt.

The retained representation is structure-of-arrays shaped:
- sorted String[] names;
- one flat byte[] containing 32-byte SHA-256 rows;
- no Map.Entry objects;
- binary-search lookup;
- linear two-pointer delta merge.

This is the file-level bridge to M3AST, token, regex, structured-document and other precompute
owners. Their implementations remain independent and may consume the stream incrementally.

Recipe correction memory records algorithm/representation improvements and their executable guards.
