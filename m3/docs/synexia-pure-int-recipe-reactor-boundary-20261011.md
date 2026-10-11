# Synexia custody Maven dependency correction — 2026-10-11

## CI finding

The canonical Synexia custody module already exists at
`m3/synexia-import/pure-int-recipe-custody` and already declares the correct
artifact coordinates, `com.synexia:synexia-openrewrite-recipes:1.0.0-SNAPSHOT`.
The build failure is its OpenRewrite dependency coordinates:
`rewrite-core`, `rewrite-java`, `rewrite-java-21`, and `slf4j-api` use
custom `-m3-source-1` version suffixes that are not published in Maven Central.
The recipe consumer cannot be compiled until this canonical custody artifact is
successfully installed.

## Bounded correction

Use the published OpenRewrite 8.17.1 artifacts and SLF4J 1.7.36 in this
custody module. The ten source files in this custody crate use the standard
OpenRewrite recipe/tree APIs; no implementation source, API signature,
behavior, source pin, or authority state changes.

The default migration-recipes dependency remains the existing canonical
`com.synexia:synexia-openrewrite-recipes:1.0.0-SNAPSHOT`. The existing
workflow already installs the custody module before invoking the consumer
tests, so no extra module, duplicate artifact, or unpinned vendor source is
needed. The full canonical Synexia project in `hsoliwal/com.synexia` remains
the upstream owner; this M3 module is its provenance-pinned receiver custody.

## Adjacent exact-state correction

The recipe ownership checker also found `M3ContractMode.java` had evolved in
commit `d2c1fb3ffd8f2fe5a8d91a6f309b769d74531edd` by adding the package-private
reverse adapter `fromCanonical(M3ContractMode)`. Its enum values and outward
vocabulary remain unchanged. The ledger pin is updated to exact current Git
blob `3ee8dd184d5c382391f6f9590fb9c773044bfaa5`; its canonical pin and pending
adapter state are preserved.

## Verification requirements

1. Install the custody artifact with published dependencies under JDK 21.
2. Run the custody module tests.
3. Run the Synexia recipe ownership checker and exact 15-entry vendor snapshot test.
4. Run the canonical consumer tests and full migration-recipe Maven proof.
5. Keep runtime/JNI/release admission blocked unless their separate gates pass.
