<!-- SPDX-License-Identifier: Apache-2.0 -->
# Recipe mat-sortable-processor-20261107 (RELOCATE_MAT_SORTABLE_PROCESSOR v1)

Census pass 2, leaf **P2-137** (crate `m3-sortable-comparison-evidence-20261014`: `MatSortableProcessor`,
`SortableProcessorContract`) of the first-party donor `hsoliwal/com.synexia` (develop `56cbdc631259`, the develop-live
heads of both files), relocated into a new module `m3/compiler/mat-sortable` (`com.m3:m3-compiler-mat-sortable`, module
`org.m3jdk.compiler.matsortable`, exported package `jdk.m3.compiler.matsortable`), recipe-first and byte-for-byte except
the declared rows.

| Owner | Contract |
|---|---|
| `M3MatSortableProcessor` | opt-in JSR-269 processor; for every class or record carrying `jdk.m3.collections.M3Sortable` (located by canonical name) one deterministic companion `<Type>Sortable`: `SCHEMA_SHA256`, `ORDER_POLICY_SHA256`, `SCHEMA`, `STRUCTURAL_*` facts and immutable `structuralFeatures()`, `compareExact` over the `@M3SortField` columns (or `Double.compare` over the `@M3SortSignal`), `exactComparator`, `primary` / `signals`, the validated `distance` and the `ComparisonEvidence` receipt (`compareWithEvidence`) |
| refusals | non-concrete, generic, local or inaccessible types; private or static `@M3SortField`; duplicate priorities; non-exact column types; non-`double` signals; a distance that does not take the type; checked exceptions; `@M3SortDependency` descriptors outside 1..64, blank or duplicate; schema budgets (128 members, 128 declared edges, 16,384 characters) |
| `M3SortableProcessorContract` | the donor's standalone javac proof (86 assertions): permutation replay, structural facts, exact / signed-zero / NaN / evidence checks, policy fingerprints, declared dependencies, negatives |

## Decisions ([DECISION.tsv](DECISION.tsv))

* Owner: the processor needs `java.compiler` only (canonical-name strings, no import of the annotations), the shape of
  `m3/compiler/static-use` (#2675): a compile-time module of the `m3/compiler` family, `requires static transitive
  java.compiler`, no service registration; `m3-collections` is a test-scope dependency for the fixtures (S1, S6).
* Names follow peer 22's RELOCATE_M3_OWNER v4 rule (`M3MatSortableProcessor`, `M3SortableProcessorContract`); the
  companion suffix `<Type>Sortable` is unchanged (S2).
* The processor speaks the relocated annotations: five canonical names and the bare `@Sortable` ... `@SortDependency`
  tokens (diagnostics, Javadoc, companion comment, fixture sources) are LITERAL rows to `jdk.m3.collections.M3Sort*`.
* HELD members, declared as REPLACE / DROP rows and handed off ([HANDOFF.tsv](HANDOFF.tsv)):
  * `MatCapabilityEmitter` (the `<Type>M3Capabilities` companion lane over 21 annotations, among them the held facet
    `Comparable` and `com.synexia.anop.*`): OMIT; the processor's supported-set union and `emit` call are held (S4, H1).
  * `MatIntOrder` (HOLD in #2711): the generated `stableSignalOrder` block and the contract's stable-order assertions
    are held; `M3SignalOrder` is the NEC candidate for the mat lane (S5, H2).
* Baseline: the donor contract is red at the donor head (javac's processing lint under `-Xlint:all -Werror`, since the
  processor returns `false` on purpose); the relocated contract disables that one lint category (S8, H3).
* `m3/collections/signal-codegen` `SortableProcessor` is a different annotation and contract: NEC, both kept (S3).
* FOSS ([FOSS_REUSE_DECISION.tsv](FOSS_REUSE_DECISION.tsv)): Lombok, AutoValue and Immutables are architectural
  references only; no third-party expression.

## Oracles

* Donor `M3SortableProcessorContract` (86 assertions, real javac generation and class loading) in the test phase.
* `M3MatSortableProcessorOracle` (test scope, new, with the JUnit wrapper `M3MatSortableProcessorTest`): one fixture
  corpus (10 positive sources in one javac task, a CRLF source, 25 negative corpora, a corpus without annotations)
  through the processor named on the command line with `-proc:only`; 12 companions, 25 diagnostics, 28 corpora.
* Differential ([differential.sh](differential.sh)): the oracle through the donor processor (donor annotations and the
  held emitter compiled from the donor tree) and through the relocated processor, byte-identical after the declared
  masking (S7); `javap -p` of the 4 processor classes identical (minus the declared dropped field); six one-line
  processor mutants refused by the relocated contract; the donor contract printed as `BASELINE_RED` (S8).

## Run

```sh
python -I m3/compiler/mat-sortable/recipe/mat-sortable-processor-20261107/relocate.py --donor-git <com.synexia clone> \
    --donor-rev 56cbdc6312594f06e21ef56f0ce33b605383270b [--check]
JAVA_HOME=<JDK 21> bash m3/compiler/mat-sortable/recipe/mat-sortable-processor-20261107/verify.sh <com.synexia clone> \
    56cbdc6312594f06e21ef56f0ce33b605383270b <scratch> [repo [tail]]
```

Gate results: [GATES.tsv](GATES.tsv); lessons: [RECIPE_MEMORY.tsv](RECIPE_MEMORY.tsv).
