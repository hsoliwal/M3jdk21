# Receipt: m3/tooling/companion (census2 leaf P2-117)

## Source

The source is the com.synexia crate `synexia-openrewrite-recipes/crates/m3jdk21-companion-admission-20261029` at
develop `06082e32476e`. The module receives its three dependency-free classes and four `main` proofs.
`M3CompanionInventoryRecipe` and its test import `org.openrewrite` and stay in com.synexia, so M3jdk21 carries no
external artifact.

## Transformation (mechanical)

1. The package `com.synexia.m3.companion` becomes `jdk.m3.tooling.companion`, in the `package` line only.
2. `apply_naming.py` applies the JDK-naming rewrite (`a3/jn/jdk_naming.rewrite_text`) to every file. That
   changes the derived candidate prefix from `com.m3.companion.` to `jdk.m3.companion.`, in the planner and in the
   test expectations alike, and the prose "M3JDK21" to "M3JDK 21". The result is a fixed point: 0 of 7 files
   change on a second pass.

`verify.sh` and `README.md` are new receiver files.

## Gates (OpenJDK 21, Linux)

`GATES.tsv`. `gate_pre.sh` runs the same proofs before the naming step, and the output is byte-identical.
`PROVENANCE.tsv` maps each file to its donor blob.
