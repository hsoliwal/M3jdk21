# Nebula transfer -> JEP residue DAG

Status: admission/control plane for the JDK 22-27 compatible-backport programme.

## Purpose

Backporting remains a first-class M3JDK21 lane.

The live denominator/residue work is owned by `m3/backports/JEP_CATALOGUE.tsv` and
`m3/backports/JEP_RESIDUE_QUEUE.tsv`. The Nebula proving ground contributes the tested
recipe/DAG **mechanics** only; it does not contribute JDK semantic decisions or mutation authority.

The target-side flow is:

```text
pinned hsoliwal/nebula commit
  -> Nebula transfer bundle
  -> M3NebulaTransferAdmission
  -> JEP_RESIDUE_QUEUE.tsv
  -> M3JepResidueDagPlanner
  -> one authority-free work order per unresolved JEP
  -> canonical M3RecipeDag
  -> JDK-specific compatibility/dependency/file-delta/recipe/JUnit/build/jtreg/runtime proof
  -> serial promotion only after all gates pass
```

## Nebula custody

`NEBULA_TRANSFER_BINDING.tsv` pins the source repository/commit and the expected transfer schema:

- schema: `NEBULA_M3_RECIPE_TRANSFER_V1`;
- Java release: 21;
- OpenRewrite: 8.90.4;
- entrypoint: `org.eclipse.nebula.m3.rewrite.NebulaM3Java21ConvergenceRecipe`;
- target lane: `JAVA21_JDK_COMPATIBILITY_AND_BACKPORT_LANES`.

The target does not trust the transfer's hashes. `M3NebulaTransferAdmission` recomputes:

- scheduler-plan root;
- complete transfer root;
- target-lane authority;
- orchestrator authority.

Maven/OpenRewrite, Camel, Airflow and Drools must all remain scheduling/projection mechanisms with
no direct source mutation or promotion authority.

## JEP residue work orders

`M3JepResidueQueue` parses the complete unresolved queue. It rejects:

- non-contiguous order;
- duplicate JEPs;
- releases outside 22..27;
- materialized packets without evidence paths;
- NO_RECIPE_EVIDENCE rows that nevertheless name recipe evidence.

`M3JepResidueDagPlanner` maps the existing next-action vocabulary to one explicit proof lane:

| Residue next action | DAG proof lane |
| --- | --- |
| `PROVE_OR_IMPLEMENT_COMPATIBLE_BACKPORT` | `STANDARD_COMPATIBILITY_PROOF` |
| `CLOSE_DEPENDENCY_AND_HIGH_RISK_PROOF` | `HIGH_RISK_DEPENDENCY_PROOF` |
| `CLOSE_JAVA21_COMPATIBILITY_POLICY` | `JAVA21_COMPATIBILITY_POLICY_PROOF` |
| `RETAIN_RESEARCH_ONLY_UNTIL_STABLE_OR_EXPLICIT_OPT_IN` | `RESEARCH_HOLD` |
| `CLOSE_HOTSPOT_JIT_DEPENDENCY_PROOF` | `HOTSPOT_JIT_DEPENDENCY_PROOF` |

This mapping is planning evidence. It **does not** declare the JEP Java-21 compatible.

Each work order is content-addressed against:

- the complete residue-row evidence;
- the canonical M3 backport DAG semantic root;
- the admitted Nebula transfer root;
- explicit `sourceMutationAuthority=false`;
- explicit `promotionAuthority=false`.

## CI

`.github/workflows/m3-jdk21-backport-dag.yml` now:

1. verifies the Maven/OpenRewrite recipe substrate and 99%-gated DAG kernel;
2. clones the exact pinned Nebula commit;
3. generates its transfer/DAG/orchestrator bundle;
4. recomputes and admits that bundle in M3JDK21;
5. binds every live residue row into one JEP DAG work order;
6. requires the current 41-row queue to produce exactly 41 authority-free work orders;
7. projects the canonical DAG to Camel, Airflow and Drools;
8. emits the existing backport programme status;
9. uploads the transfer admission, JEP work-order queue and scheduler projections.

## What this does not do

It does not auto-accept any JEP, copy Nebula source into OpenJDK, claim JCK/TCK conformance, or
bypass source/classfile/VM/API compatibility review. Spec-visible features remain opt-in/isolated or
rejected according to the existing compatibility policy. HotSpot/JNI/native changes still require
their own dependency, build, jtreg/runtime and parity proof.
