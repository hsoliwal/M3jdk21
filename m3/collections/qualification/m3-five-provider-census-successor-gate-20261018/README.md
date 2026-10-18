<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3 source-census successor admission — source-sealed recipe

One proven necessary gap: the original 34-source M3JDK21 census workflow correctly rejected a new, otherwise strictly source-sealed generation because it only allowed the historical postimages. Do not delete or disable that gate. Retain its exact original-file checks and admit *only* the explicitly named and pinned M3 five-provider indexed projection successor.

The new Python block verifies:
1. Unchanged historical workflow manifest remains exactly four records, with matching before/after templates.
2. Only source receipt and verifier paths may change; the other two historical files must remain byte-identical.
3. The successor manifest has pinned SHA256 `5aad026f5c2159aa62fe2236eb1484eff6724a1d3b56b59b11b97ab0851501d4`, exactly four entries, no duplicate path.
4. For the two approved files, successor.before == predecessor.after, successor.after equals one of two explicit SHA-256 constants, and the currently installed bytes equal their exact successor templates.
5. Source-level Maven/OpenRewrite named recipe, missing/drift and fixed-point proof are mandatory.

All other mutation remains a hard failure. This is an additive serial source guard refactoring only, not a new collection representation, native ABI or third-party code. Original owners JDK, Guava, Eclipse, Apache Commons, Spring; existing first-party source Apache-2.0. No upstream source copied. No weakening of `-Werror`.
