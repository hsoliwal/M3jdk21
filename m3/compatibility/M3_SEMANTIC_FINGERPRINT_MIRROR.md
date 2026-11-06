# Synexia canonical semantic-fingerprint receiver

Status: transitional M3JDK21 receiver binding.

Canonical source:
- repository: `hsoliwal/com.synexia`
- revision: `8474618143db4a351398b72c810f3811a0821e7a`
- canonical manifest: `synexia-openrewrite-recipes/CANONICAL_RECIPE_HOME.tsv`
- canonical package: `com.synexia.rewrite.semantic`

Canonical reusable owners:
- `M3SemanticHasher`
- `M3SemanticHashTable`
- `M3SemanticHashRecipe`

M3JDK21 keeps the historical `com.m3.rewrite.semantic` implementation as frozen proof residue
because old receipts/tests may bind its exact hashes. New reusable semantic-fingerprint work uses
the canonical Synexia package.

The canonical Synexia implementation intentionally hardens architecture/IOP marker detection:
`M3-IOP` / `M3-ATOM` text inside strings, chars and text blocks is data, not structural
authority. Real Java comments remain architecture evidence. This is an admitted correctness
improvement, not a claim that all old fingerprints remain byte-identical.

Both old and new hash families remain evidence/search/convergence signals only. Neither proves
behavioral equivalence or grants source mutation/promotion authority.

M3JDK21 product/runtime and target-specific backport recipes remain target-owned.
