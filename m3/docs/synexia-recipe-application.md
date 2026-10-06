# Synexia recipe application receipt

Status: **M3JDK21 applied runtime; Synexia owns reusable recipe custody**.

## M3 String history convergence

Canonical recipe:

`com.synexia.rewrite.m3jdk.M3StringHistoryConvergence`

Authority:

- repository: `hsoliwal/com.synexia`
- branch: `m3/m3jdk21-string-convergence-donor-20261006`
- PR: `hsoliwal/com.synexia#9530`
- pinned revision: `ad5df8458ffeba643a6c343e82dd61a338facf29`
- Java crate: `synexia-m3-string-history-convergence` — 18 targets
- text/native/workflow crate: `m3-string-history-convergence` — 6 targets

The Synexia recipe retains the original exact preimage SHA-256 values from the former
M3JDK21-local crate and the reviewed postimages. It fails closed on any target that matches neither
the admitted preimage nor the admitted postimage.

## Applied result

The current continuation runtime state was checked at:

`hsoliwal/M3jdk21@m3/m3string-runtime-precompute-fix-20261005`
`26a13d9fd94ef762127262a67c86b9d7bb3c8cf0`

Direct manifest verification after moving custody:

- Java targets: **18/18 exact Synexia postimage hashes**
- text/native/workflow targets: **6/6 exact Synexia postimage hashes**
- result: **fixed point / zero transformation changes**

This receipt is provenance/application evidence only. M3JDK21 remains the sole runtime owner of
`java.lang.String`, M3 String storage, all runtime precompute, HotSpot/JNI integration and target
qualification.

## Local-recipe prohibition

Do not recreate these former local authorities:

- `m3/tooling/migration-recipes/src/main/resources/META-INF/rewrite/m3-string-history-convergence.yml`
- `.../jdk21-hash-pinned/jdk22-m3-string-history-convergence/`
- `.../jdk21-hash-pinned-text/m3-string-history-convergence/`
- `M3StringHistoryConvergenceRecipeTest`

Evolve the Synexia recipe first, apply its new admitted postimage to M3JDK21, then update this
receipt and run the M3JDK21-native compiler/jtreg/HotSpot/JNI gates.
