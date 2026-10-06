# J485 — Stream Gatherers backport packet

Status: candidate, not admitted to the default Java 21 identity.

JEP 485 is the final Stream Gatherers API. It adds public `java.util.stream` API and therefore is
an M3JDK21 **LIBRARY_API superset** candidate, not a claim that stock Java SE 21 contains Gatherers.

## Canonical donor lineage

The packet preserves the final implementation plus all directly relevant fixes found by walking the
actual Gatherer file histories:

1. JDK-8319123 — JEP 461 preview implementation.
2. JDK-8328316 — finisher may emit after sequential integrator short-circuit.
3. JDK-8334162 — correct Gatherer.defaultCombiner Javadoc link.
4. JDK-8342707 — graduate Gatherers from preview (JEP 485).
5. JDK-8347274 — mapConcurrent variable-delay/interruption/finishing correction.
6. JDK-8357647 — forward upstream size information downstream.

Two nearby commits are deliberately **not** copied wholesale:

- JDK-8325949 introduced the later `MhUtil` helper. M3JDK21 lacks it; J485 uses the identical
  Java-21 `MethodHandles.Lookup.findVarHandle` operation with the same InternalError failure policy.
- JDK-8196106 is a broader flatMap refactor. Its GathererOp delta is import cleanup only; the final
  Gatherer engine does not call the new flatMap helper, so the unrelated primitive/reference
  pipeline refactor is excluded.

## Java 21 compatibility findings

The final product sources use no post-21 language syntax. The only directly missing internal helper
found by source audit is `jdk.internal.invoke.MhUtil`.

Other required facilities already exist in M3JDK21, including virtual threads,
`JavaUtilCollectionAccess.listFromTrustedArrayNullsAllowed`, `Nodes.castingArray`, the stream
pipeline machinery and VarHandle/MethodHandles support.

The final jtreg corpus has eight Gatherer test files. Two contain post-21 unnamed `_` parameters;
those identifiers must be renamed mechanically for source-21 tests without changing behavior.

## Admission boundary

Product materialization must be recipe-first and source-sealed. Admission requires:

- patched `java.base` compilation under Java 21;
- all adapted Gatherer jtreg tests;
- sequential and parallel behavior;
- short-circuit finisher behavior;
- mapConcurrent interruption/finishing/order behavior;
- size propagation;
- window/fold/scan contracts;
- API/binary review for adding `Stream.gather`;
- fixed-point recipe replay;
- whole-image build/jtreg once native build prerequisites are available.

No default-distribution selection is implied by this packet.
