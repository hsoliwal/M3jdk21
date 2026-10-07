# Synexia recipe mirror receiver V2

This directory is a proof-bound **receiver**, not a reusable recipe or ownership implementation.

Canonical owner:

- repository: `hsoliwal/com.synexia`
- revision: `e0e323fabb016d95a21417f93d4b4262d6514356`
- canonical snapshot: `.m3/m3jdk21-recipe-tree-audit-v2.tsv`
- canonical content root: `f9554fad00e93eb4fc721b1a8a114b44c3e987d7394e656032b2550379ffb809`

The received snapshot classifies the reviewed M3JDK21 recipe tree at target revision
`80b8534ad1e45002185fb3eeab233f98d3a7c0bd`.

Rules:

- Synexia-canonical residue is frozen in M3JDK21 and may not evolve independently.
- A reusable defect/change returns to Synexia, is mastered there, and arrives through a new sealed handoff.
- Thin target adapters may adapt only target integration.
- JDK-specific backport/JEP/runtime recipes remain target-local.
- This receiver grants no source mutation, merge, semantic-equivalence, licensing, or promotion authority.
- The validator/CLI remains canonical in Synexia and is invoked from the target through an opt-in Maven profile.
