<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->

# M3 donor catalogue: BiSheng JDK and Excelsior JET

**Invariant:** donor discovery never authorizes source import. A donor capability becomes an M3JDK candidate only through an explicit, hash-pinned import mode and the existing atom-coverage admission contract.

This leaf establishes the first machine-readable donor catalogue for two materially different Java-runtime families:

* **BiSheng JDK 21** is an OpenJDK downstream. Compatible files may enter candidate review as an exact vendor delta, but only after ancestry, file headers, provenance, contract, tests, ABI and runtime evidence are checked.
* **Excelsior JET** was an independent proprietary Java implementation. Its runtime/compiler source is unavailable here. Its public GPLv3-or-later build API documents useful architecture, but GPLv3 source is not copied or translated into GPLv2-only M3JDK core. Excelsior capabilities therefore default to `SPEC_REIMPLEMENT` or `ORACLE_ONLY`.

The catalogue is deliberately network-free at verification time. Remote repositories were inspected once and represented by immutable commit and Git-object identities. CI validates only those sealed records and the deterministic planning logic; it does not silently follow branches or download donor code.

## Exact donor pins

| Donor | Immutable pin | Treatment |
|---|---|---|
| BiSheng JDK 21 | `cd7d2e7cf826d33ea91ccd8cb1f6e81f4342fd36` | `PATCH` candidates only through exact OpenJDK delta extraction |
| OpenJDK 21.0.13+9 reference | commit `ba94eace682733f66e623c089bcafcf4a1b8e39c`, annotated tag object `10a4adb7b3e073d5f8e4369914af82f9130e69c0` | release reference; the actual merge base is computed from a local combined object graph |
| Excelsior JET API | `06d5dda8f8e3294e5a3389baadf017f4278435eb` | architecture evidence only; public source is GPL-3.0-or-later |
| Excelsior JET core | no public source pin | proprietary, documentation/oracle treatment only |

A release tag is not assumed to be the donor merge base. `M3OpenJdkDeltaInventory` computes the unique Git merge base after both exact commits are present in one local object database and refuses unrelated histories.

## Files

```text
catalogue/DONORS.tsv       one row per donor family and immutable revision
catalogue/SOURCES.tsv      exact evidence objects supporting catalogue claims
catalogue/CAPABILITIES.tsv capability candidates and their permitted import modes
contract/IMPORT_MODES.tsv  closed import-mode semantics
contract/DONOR_SCHEMA.tsv  exact column contracts
src/main/.../M3DonorCatalogue.java
                           strict parser, policy validator and deterministic plan emitter
src/main/.../M3OpenJdkDeltaInventory.java
                           local Git vendor-delta inventory with merge-base receipt
src/test/.../M3DonorCatalogueSelfTest.java
                           dependency-free positive, refusal and Git-fixture tests
verify.sh                  ordered compile, test and fixed-point verification
```

## Closed import modes

* `PATCH`: source-level candidate from a compatible lineage. It still requires per-file licence/header and behavioral admission.
* `ADAPT`: compatible source transformed behind an existing interface. It does not authorize contract changes.
* `SPEC_REIMPLEMENT`: derive a behavioral/architectural specification, then implement independently without copying expression.
* `ISOLATE`: separately licensed component behind an explicit distribution boundary. This leaf does not use it for Excelsior.
* `ORACLE_ONLY`: use behavior, tests, documentation or benchmarks as evidence; import no implementation expression.
* `REJECT`: candidate is inadmissible.

The validator refuses direct-source modes for proprietary donors and for GPLv3-or-later evidence targeting GPLv2-only M3JDK core. It also refuses mutable revisions, unresolved foreign keys, unsorted rows and unknown vocabulary.

## Verification

```sh
bash make/m3/tooling/donor-catalogue/verify.sh
```

The script executes, in order:

1. shell syntax validation;
2. Java 21 compilation with `-Xlint:all -Werror`;
3. dependency-free catalogue and negative-path tests;
4. a temporary Git repository test covering add, modify, delete and rename delta rows;
5. two independent plan generations and byte comparison;
6. strict catalogue validation and receipt generation.

Generated proof is written beneath root `build/m3-donor-catalogue/`, which remains outside tracked source.

## BiSheng delta extraction

After a local repository contains both exact commit objects, run:

```sh
java -cp <classes> jdk.m3.tooling.donors.OpenJdkDeltaInventory \
  make/m3/tooling/donor-catalogue \
  bisheng-jdk-21 \
  /path/to/local/combined-git-repository \
  build/m3-donor-catalogue/bisheng
```

The output contains:

```text
DELTA.tsv    exact raw Git file delta from computed merge base to donor pin
RECEIPT.tsv  donor pin, upstream release pin, merge base, row count and SHA-256
```

The extractor does not fetch remotes, update refs, modify the donor repository, cherry-pick code or claim compatibility. A generated row is a review candidate, not an admitted M3JDK change.

## Initial capability candidates

The catalogue records only claims backed by pinned evidence:

* BiSheng OpenJDK downstream delta and its JIT profile-cache implementation surface;
* Excelsior native AOT build orchestration and runtime packaging;
* Test Run based optional-runtime discovery;
* startup and reflective-access profiles;
* profile-guided optimization;
* persistent project database based incremental native compilation;
* cross-target profiling-image generation.

No Excelsior source, binary, decompilation result or Huawei-private material is included. No JDK product source, Java SE API, class-file contract, JVM behavior or JNI ABI is changed by this leaf.
