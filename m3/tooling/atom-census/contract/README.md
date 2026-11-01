<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 OR EPL-2.0 -->

# M3 atom coverage contract

This directory is the first executable leaf of
`M3JDK-MACHINE-MAINTAINED-MECHANICAL-SUPERSET-1`. It adds no JDK runtime API and changes no existing JDK behavior.
It defines the metadata and fail-closed evidence boundary that later whole-repository atomizers must satisfy.

## Commands

Dependency-free Java 21 proof:

```text
./verify-contract.sh
```

Ledger verification after compiling the module:

```text
java jdk.m3.tooling.atoms.contract.M3AtomCoverageVerifier validate ATOMS.tsv EVIDENCE.tsv
java jdk.m3.tooling.atoms.contract.M3AtomCoverageVerifier admit ATOMS.tsv EVIDENCE.tsv
```

`validate` checks exact UTF-8/LF form, schema, sorting, identities, references and evidence payloads. It prints missing
or unsuccessful admission facts without converting them into success. `admit` applies the same checks and then fails
unless every atom has a known pattern and a complete admissible kind-specific gate profile.

The verifier validates the ledger and its recorded SHA-256 identities. Evidence producers and target gates remain
responsible for creating the referenced artifacts and proving that their recorded hashes match actual bytes.

## Files

- `src/main/java/.../contract`: enums, annotation/interfaces, immutable records, strict ledger and CLI verifier.
- `src/test/java/.../M3AtomCoverageVerifierSelfTest.java`: dependency-free refusal/admission suite.
- `src/test/java/.../M3AtomCoverageContractTest.java`: Maven/JUnit binding and table fixed-point tests.
- `contract/ATOM_COVERAGE_SCHEMA.tsv`: exact ledger columns and constraints.
- `contract/GATE_PROFILES.tsv`: generated required-gate profile for every atom kind.
- `contract/STATUS_CONTRACT.tsv`: closed evidence-state semantics.
- `m3/docs/M3JDK21_MACHINE_MAINTAINED_MECHANICAL_SUPERSET.md`: repository-wide invariant.
