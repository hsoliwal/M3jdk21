# Normalize-before-absorb: validate before materialization

This bounded repair reuses the source convergence gate and materializer from PR61.
It does not create another atomization engine, alter JDK product source, add a new
JEP, weaken release admission, or claim whole-JDK completion.

## Contract
Canonical source and normalized workspace must be disjoint. Every selected row,
input, candidate, target, and receipt must be validated before the first target
write. Filesystem aliases must not redirect writes into canonical source. Invalid
unchanged hashes, malformed rows, and conflicting receipts fail closed.

A completed normalized workspace is replayable with identical receipt bytes and
no source rewrite. Each individual update uses a sibling temporary file followed
by replacement; hard-linked target files cannot mutate the original inode.

This is validation-failure atomicity plus per-file replacement, NOT a multi-file
filesystem transaction or a hostile-concurrent-writer sandbox. Exclusive ownership
of the workspace and immutable source/evidence during a run remain required.
An I/O error may leave reviewed postimages in the detached workspace; no final
success marker is published until readback succeeds. Replay verifies before resume.

## Scope / patterns / evidence
- Gate: read-only Specification; semantic evidence validity.
- Loader: one shared manifest Recognizer reused by materializer.
- Materializer: two-phase plan/apply with immutable per-file candidates.
- Replacement: resource/lifecycle Adapter, file-local operation.
- Receipt: reproducible projection, not proof of behavioral equivalence.

Scope is MODULE tooling behavior repair, not a contract-preserving JDK refactor.
Existing output fields and V1 roots for valid manifests are preserved.
Maven's existing backport unittest discovery owns the Python tests. A reusable
OpenRewrite text-snapshot crate and JUnit replay/refusal tests own application of
the repair. Direct Python execution must not be called Maven/OpenRewrite/JUnit.

## Prior failures to reproduce
1. UNCHANGED with different pre/post hashes is accepted.
2. A later row failure occurs after an earlier target was modified.
3. A hard-linked target writes into the canonical original.
4. Replay of a successfully materialized changed workspace is rejected.

New refusals intentionally tighten invalid-evidence handling. Normalization is
candidate generation; compatibility, 99% coverage, runtime/native parity, and
serial promotion remain independent gates.

## Admission limitations
The read-only gate certifies only selected Java targets. Empty/non-Java target
selections do not certify whole-tree convergence. Receipts are source identity
evidence, not proof that a recipe preserved semantics or that a JEP is compatible.
The caller must establish tree completeness and recipe/test authority separately.

The existing Maven backport test discovery includes the new Python test. No POM,
CI permission, skipped-test flag, canonical registry or original test is changed.
Named recipe: `com.m3.rewrite.backport.ConvergencePreflight`. The underlying recipe
requires PlainText inputs; Python is not misrepresented as a Java LST. The manifest
owns both existing scripts and an explicitly ABSENT-before Python test. File modes
remain 100644. Readback and the JUnit fixture preserve exact pre/post text.
