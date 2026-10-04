# Release JEP denominator authority — 2026-10-04

## Problem

The previous control plane could reach a false fixed point because both the JEP catalogue and work
queue omitted official release features together. The 82-row denominator omitted JEP 404 and JEP
483 from JDK 24 and JEP 521 from JDK 25.

The corrected repository denominator is 85 rows:

- JDK 22: 12
- JDK 23: 12
- JDK 24: 24
- JDK 25: 18
- JDK 26: 10
- JDK 27: 9 (explicit in-development snapshot)

JDK 22-26 authority is the official OpenJDK release feature page for each release. JDK 27 remains
an explicitly mutable snapshot until release.

## Atomization

Nine independent FILE recipe atoms own exactly one target each:

1. release authority TSV;
2. authority verifier code;
3. authority verifier test;
4. JEP catalogue;
5. work queue;
6. program-status code;
7. program-status test;
8. programme status report;
9. fail-closed backport verifier.

The MODULE join is `M3ReleaseJepDenominatorRecipe`. A later join does not widen the mutation
authority of its FILE leaves.

## Pattern / IOP

Each leaf is an evidence/configuration atom. The join is a Composite with
`Backport.ReleaseDenominator` participation. The release authority is external truth; catalogue,
queue and reports are projections and may not override it.

## Fixed point

Every FILE atom is hash-pinned to the exact parent preimage or ABSENT and exact reviewed postimage.
JUnit proves all nine changes on first application, refusal of a drifted leaf, and zero changes on
the second application.

## DAG projections

`packet.tsv` is canonical. Camel Java DSL, Airflow Python and Drools rules are projections of the
same packet/evidence root and receive no mutation or promotion authority.

## Completion implication

This repair does not claim all compatible JDK changes are backported. It prevents the JEP-level
denominator from falsely reporting completion while the complete 14,948-commit released-change
denominator continues independently.
