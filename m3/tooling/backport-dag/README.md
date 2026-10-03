# M3JDK21 recipe DAG control plane

This module is the framework-neutral composition layer for the M3JDK21 compatible-backport
programme.

## Authority

The canonical authority is:

`src/main/resources/com/m3/tooling/dag/m3-backport-dag.tsv`

and its Java validator `M3RecipeDag`.

Camel, Airflow, Drools, CI and future workflow engines are projections only. They may schedule or
visualize the graph; they may not change dependencies, edit scope, compatibility decisions or
serial-promotion semantics.

## Locked execution order

The canonical path is:

`inventory -> compatibility-proof -> dependency-closure -> file-delta -> recipe-crate ->
recipe-junit -> diff -> lint -> compile -> jtreg -> runtime -> promote`

Independent FILE recipes derived from one work packet may fan out in the same topological layer.
Canonical promotion is one serial terminal node.

## Scope law

Recipe mutation authority is monotonic:

`FILE -> VISIBILITY -> PACKAGE -> MODULE -> MULTI_MODULE -> LIBRARY_API`

A DAG edge that broadens the inherited mutation boundary must declare
`scope_promotion_approved=true`. Broadening scope for convenience is invalid.

## Maven

From repository root:

`mvn -B -ntp -f m3/pom.xml clean verify`

The default reactor verifies:

- OpenRewrite migration/backport recipes;
- this DAG semantic kernel;
- backport inventory/admission scripts.

Use `-Pm3-extended` to include independently built M3 collection lanes.

## Recipe atom evidence

Mutation scope alone is not enough to admit an M3 recipe atom. A strict packet also supplies a
companion evidence TSV with one row per atom:

```text
packet_id
atom_id
contract_ref
documentation_ref
pattern
iop_role
junit_proof_ref
fixed_point_required
```

`fixed_point_required` must be `true`. Missing, extra, duplicate or mixed-packet evidence fails
closed. The evidence manifest receives its own SHA-256 semantic root through
`M3AtomEvidenceRoot`.

This makes the M3 requirement executable:

```text
recipe atom
  -> documented contract
  -> pattern / IOP role
  -> JUnit recipe proof
  -> fixed point
  -> DAG execution
```

The legacy one-file packet CLI remains useful for structural inspection. The evidence-bound form is:

```bash
mvn -B -ntp -f m3/tooling/backport-dag/pom.xml \
  exec:java \
  -Dexec.mainClass=com.m3.tooling.dag.M3BackportPacketMain \
  -Dexec.args="packet.tsv atom-evidence.tsv"
```

`M3OrchestrationProjectionMain` also accepts the evidence TSV as an optional third argument and
emits `atom-evidence.tsv` beside the framework projections.

## Framework projections

- `adapters/camel/m3-backport-route.yaml`: Camel route projection.
- `adapters/airflow/m3_backport_dag.py`: Airflow scheduling projection.
- `adapters/drools/m3-backport-admission.drl`: Drools promotion-policy projection.

These files intentionally have no authority to accept a backport by themselves.

## Coverage

The semantic DAG kernel is gated at 99% line and branch coverage. Tests prove cycle rejection,
missing dependencies, parallel FILE layers, explicit scope promotion, serial terminal promotion and
promotion-fact admission.
