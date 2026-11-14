# M3 native index-range and ordered snapshot recipe

Target: `hsoliwal/M3jdk21`, inspected `master@4e29dc9d4141261e8763fbe9ffd6a20e017b1aff`.
Extends the received Java direct-offset work from PR #1088 rather than replacing its owner.
Private Synexia repositories are convergence donors; no private repository dependency is
introduced into the product. Original source-seal tooling is reused verbatim at build time.

## Mandatory M3 workflow

Classify the operation; consult the accumulated catalogue; edit recipe/templates;
qualify against exact preimages; emit serial per-file candidates; verify public contracts,
JNI behavior and exact postimages. Fix and rerun the recipe, never patch generated output.
Keep recipe, tests and generated results together in review. This task records and tests
that invariant; it does not claim organization-wide policy enforcement is implemented.

Named Maven/OpenRewrite recipe: `com.synexia.m3.NativeIndexRange20261017`.
Class: `com.synexia.rewrite.M3NativeIndexRangeRecipe`, a thin extension of unchanged
`M3GpuToolJoinRecipe` and `M3GpuToolJoinManifest`. Resource manifest has exact before/after
SHA-256 and KEEP pins. `before/` contains the three source preimages. SDK errors must
prevent promotion. The SDK scheduler is a separate gate from direct kernel replay.

```sh
# From repository root; no product writes, requires already-generated outputs:
python3 m3/collections/recipes/native-index-range-20261017/verify.py
# Explicit qualification then serial promotion in an isolated checkout:
python3 m3/collections/recipes/native-index-range-20261017/verify.py --apply
# Real public OpenRewrite 8.17.1 + JUnit + Maven verify binding:
mvn -B -f m3/collections/recipes/native-index-range-20261017/pom.xml clean verify
```

The Maven POM compiles the actual two product owners and their generated JUnit adapter,
four actual SDK tests, and then invokes the Java/C/JNI qualification. It does not depend
on the custom collection reactor or pretend to build OpenJDK. Maven is not installed in
the local execution container; SDK/JUnit results must come from a real Maven run, not the
locally executed manifest kernel. The generated CI workflow rejects missing/skipped tests.

Native proof requires Linux, Java21, GCC, Python. It runs the unchanged original sources,
then the C, compactor-Java and projection-Java file passes separately. It checks new and
legacy native binaries, absent-library refusal, a native-dispatch sentinel, malformed
private JNI calls, two compiled faulty C variants, public javap surfaces, fixed points and
allocation observations. Inputs/donor hashes and limitations are in evidence and catalogues.
No new model/viewport toolkit, external controller, object graph or native state owner.

`--apply` operates only after requested local gates pass and rechecks admitted source
hashes. It is serial rather than filesystem-transactional; use an isolated worktree.
The review sequence is recipe/tests/evidence commit, then exact generated receiver commit.
