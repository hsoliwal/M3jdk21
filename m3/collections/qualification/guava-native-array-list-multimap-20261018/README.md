# Native Guava ArrayListMultimap receiving qualification

This increment adds lazy expected-key reservation to the existing
M3LinkedHashMap and `M3ArrayList.trimToSize(boolean invalidateIterators)`.
Existing constructors and no-argument list trimming retain their behavior.
No collection owner is replaced. The changes support the companion standalone
Guava 33.4.8 Java 21 variant, which uses these existing private array owners.

The change is stacked on [PR #1290](https://github.com/hsoliwal/M3jdk21/pull/1290),
branch `codex/guava-array-values-20261018-bd133f4a`, commit
`d10f242ef0c0f1a4ff5a0dfe4d1ff547846d7401`. Its factory increment and four
publication/proof files are inherited unchanged. The two qualified core
preimages are identical to source base
`2f2beb2eff7a88f1f7c0012fd17a78a6f0a00b49`.

The canonical PackedHashMap and source-bound recipe changes are a companion
Synexia draft increment on canonical base
`140c263a90f9803b9c46103c63355a286e0515f0`; prior receiving lineage includes
[Synexia PR #12292](https://github.com/hsoliwal/com.synexia/pull/12292).
Do not treat the receiving source as a replacement for canonical ownership.

The focused core tests cover four map contracts and four list trim contracts.
The accompanying report records canonical map 4/4, receiving map 4/4, receiving
list 4/4, existing interop 23/23, unchanged Guava suite 6064/6064, native artifact
tests 4/4, matching public/protected ABI and seeded differential behavior,
bidirectional original/candidate serialization, and packaged class/module-path
smokes. Its report SHA-256 is
`fbb9211f4cf05907b8ea6d7d6a1a6bc9903147a925af89b5c7328c6d7acd518b`.

FOSS_REUSE_DECISION.tsv, SOURCE_PROVENANCE.tsv and the small recipe manifests
bind the borrowed patterns and exact emitted source. Existing factory owners,
the separate M3GuavaListMultimap owner, prior proofs, and all catalogue rows
are preserved. The broader 107-row collection catalogue remains in progress.
Hosted CI and draft review remain separate publication checks.
