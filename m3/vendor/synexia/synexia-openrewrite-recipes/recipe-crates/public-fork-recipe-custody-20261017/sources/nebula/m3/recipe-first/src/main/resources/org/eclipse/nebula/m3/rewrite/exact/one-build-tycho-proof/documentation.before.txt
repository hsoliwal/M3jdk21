# M3 one-build convergence trial — Nebula

Status: proving ground before mechanical transfer to M3JDK21 and Synexia.

## Objective

Use the reusable OpenRewrite recipe as the unit of work, not hand-edited target files.

Nebula is already Maven/Tycho based and already carries the M3 recipe-first module. The trial therefore
does not add another build system. It binds the existing canonical FILE-local recipe DAG to the root
Maven reactor and proves it in one disposable CI build.

## Canonical FILE DAG

```text
INVENTORY
  -> ATOMIZATION
  -> PATTERNIZATION / IOP
  -> DOCUMENTATION
  -> PROVEN_FILE_CONVERGENCE
  -> SECOND-PASS FIXED POINT
```

Every source-changing leaf remains a tested OpenRewrite recipe. Independent files may fan out, but
the recipe order inside one file remains deterministic and serial.

## Orchestration

OpenRewrite owns source mutation. Maven owns build/proof.

The existing content-addressed DAG can also be projected to:

- Apache Camel YAML;
- Apache Airflow Python DAG;
- Drools/KIE DRL.

Those projections schedule/select recipe atoms only. They have no direct source mutation or
promotion authority.

## One-build proof

The root Maven profile is:

`m3-nebula-file-convergence`

The CI sequence is:

1. diff hygiene;
2. Java 21 compile/JUnit/99% semantic coverage of the recipe module;
3. DAG + Camel/Airflow/Drools manifest generation;
4. root OpenRewrite dry run;
5. candidate apply only in the disposable checkout;
6. candidate diff capture;
7. second OpenRewrite dry run with no remaining patch;
8. recipe/JUnit/coverage re-proof;
9. candidate/DAG evidence upload.

The workflow never commits its disposable candidate automatically.

### FILE-scope admission after application

The reusable `m3/ci/verify-file-local-candidate.py` gate examines the complete Git
worktree delta, not a hand-authored list. It admits only **modified, already-tracked
Java source files** for this FILE-only atomization/patternization/documentation run.

A newly added/deleted/renamed/copied file, a POM/YAML/native/binary change, an
untracked source output, an unsafe filename or whitespace-error diff is **HOLD**,
not an implicit PACKAGE/MODULE promotion. Those cases require a separately
scope-qualified recipe and proof receipt. This deliberately does not limit the
number of independently modified Java files.

`m3/ci/test_verify_file_local_candidate.py` proves the rule with adversarial
status/path fixtures and real temporary Git repositories. CI executes the tests
before Maven, invokes the gate after the candidate apply, writes
`candidate-paths.txt`, and preserves the diff as an artifact. An empty
candidate set is acceptable only as a genuine no-change fixed point; it does
not prove that all possible Nebula files were transformed.

This admission gate alone **does not prove ABI/behavior equivalence**, 99% semantic
coverage, a successful Tycho/SWT build, or correctness of a JNI/native port.
Those remain independent test/compiler/runtime/transfer gates.

## Transfer / backport order

Backporting remains part of the programme; it is deliberately downstream of this proof.

```text
Nebula proven recipe/DAG
  -> bind exact recipe + DAG root + proof receipt
  -> hsoliwal/com.synexia canonical reusable recipe
  -> hsoliwal/M3jdk21 mechanical application
  -> JEP / PR / tooling backport lanes use the same atomized/patternized substrate
```

No M3JDK21 source is changed merely because the Nebula candidate exists. Transfer requires the exact
recipe identity, Java 21 proof, fixed point and contract evidence first.

## Invariant

Repository size never widens edit authority. A contract-preserving FILE transformation stays FILE
even when applied to thousands of files. Scope promotes only when visibility, package, module,
multi-module or library/API boundaries are actually crossed.


## Hosted proof bootstrap

GitHub evaluates `pull_request` workflows from the default branch. The proof workflow was first bootstrapped onto `master` by merge commit `17e7af115dbde767141cd37d6fb0d145f26b5b0a`. This feature branch then merged that master commit ancestry-first before the hosted proof was retriggered. The bootstrap changes CI definition only; it grants no mutation or promotion authority.
