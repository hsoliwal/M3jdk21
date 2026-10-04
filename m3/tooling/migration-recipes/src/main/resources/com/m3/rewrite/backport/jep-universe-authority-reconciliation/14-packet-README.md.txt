# JEP universe authority reconciliation — 2026-10-04

## Purpose

Reconcile the complete M3JDK21 JEP file-atom planner with the authoritative 85-row
release-feature denominator and immutable upstream donor trees.

This packet does not apply OpenJDK product source. It fixes the control plane that discovers,
compares and emits candidate FILE recipe atoms.

## Denominators

Feature authority:

- JDK 22–26: 76 released JEPs.
- JDK 27: 9 in-development snapshot JEPs.
- Total planning rows: 85.

Donor-tree authority:

- JDK 22 jdk-22+36
- JDK 23 jdk-23+37
- JDK 24 jdk-24+36
- JDK 25 jdk-25+36
- JDK 26 jdk-26+35
- JDK 27 snapshot commit f3701c80216900f3ded26f9de1befe43813be95c

JDK 27 is deliberately not represented as a GA tag.

## M3 atomization

Every mutable control file is a FILE atom. The FILE leaves are composed only for semantic
coordination:

1. donor-authority — release/donor-ref authority and proof.
2. file-atom-mechanics — verbatim comparison and Java/text FILE-crate generation.
3. jep-planning — JEP seed inventory and donor-fenced FILE-atom queue.
4. ci-proof — exact donor fetching and 85-row non-authority assertions.
5. control-join — MULTI_MODULE proof-only join.

The join never widens the mutation authority of a leaf. All product-source mutation and canonical
promotion authority remain false.

## Pattern / IOP roles

- authority files: EvidenceAuthority / ReleaseDenominator
- comparator/generator: MechanicalTransform / FileAtomFactory
- JEP inventory/planner: InventoryPipeline / CandidatePlanner
- workflows: VerificationAdapter / ProofRunner
- control join: DAGComposition / ControlPlane

## Recipe-first rule

M3JepUniverseAuthorityReconciliationRecipe is the replay authority for the bounded control-plane
target set. Existing targets must match exact parent text or reviewed postimage. Additive targets
must be absent or already match the reviewed postimage. Third states fail closed.

Recipe JUnit must prove:

- exact first-pass replay;
- missing/drifted/duplicate/occupied target refusal;
- unrelated source preservation;
- second-pass zero-change fixed point;
- declared MULTI_MODULE scope.

## Orchestration

packet.tsv is the canonical framework-neutral DAG. CI may project it to Camel Java DSL,
Airflow Python and Drools rules using M3OrchestrationProjectionMain.

Those projections are execution/admission views only. They never gain compatibility,
source-mutation or promotion authority.

## Completion boundary

This packet closes the denominator/planner mismatch. It does not claim all compatible JDK 22–27
features or non-JEP changes are implemented. Product packets still require dependency closure,
compatibility proof, source-sealed recipes, OpenJDK build/jtreg/runtime proof and fixed point.
