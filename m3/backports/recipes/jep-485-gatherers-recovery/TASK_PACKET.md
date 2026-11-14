# JEP 485 recovery on current M3JDK21 master

Status: recovery candidate only; not a compatibility or completion claim.

## Exact base

Current master: b71ee5bf88398fb80961c13db03e8c675fb445ce.

The four existing JDK21 stream owners are byte-identical to the source-sealed preimages used by merged recovery PR #105:

- AbstractPipeline.java
- ReferencePipeline.java
- Stream.java
- package-info.java

The three added Gatherer classes and the eight focused Gatherer tests are absent on current master.

## Recovery source

Recover the exact reviewed tree from merged PR #105 head
`ec5ca82ecffcdc5b146587e55e5ade04d74f6c0f`.

Do not re-derive or hand-edit product code. Reuse the exact reviewed blobs for:

- Gatherer.java
- GathererOp.java
- Gatherers.java
- the four integrated stream postimages
- eight focused JDK tests
- source-sealed recipe, named OpenRewrite recipe, preimage fixtures
- packet/atom/dependency/upstream evidence

Current master retains the JEP 485 compatibility policy classification as an opt-in SE API extension. This recovery must not represent Stream Gatherers as default stock Java 21 identity.

## Allowed reconciliation

Only two non-verbatim reconciliations are allowed:

1. register the restored JEP 485 wrapper in the current external scope registry as
   `LIBRARY_API + EXPLICIT_CONTRACT_CHANGE`, preserving any newer registry entries;
2. repair the historical workflow prerequisite by adding `libxrandr-dev`, because exact prior
   configure evidence failed only on missing Xrandr headers after all preceding native prerequisites passed.

No dependency/POM/coverage threshold weakening, no rebase/force-push, no Java grammar change, and no unrelated JDK source mutation.

## Required proof

diff -> recipe compile/JUnit -> packet/DAG verification -> OpenJDK configure -> images ->
Gatherer jtreg family -> existing stream regression tests -> built-JDK API smoke.

The previous PR #105 hosted run is evidence of two historical infrastructure failures only:
- recipe lane blocked by an unrelated A3 compile defect later repaired elsewhere;
- OpenJDK configure blocked by missing `libxrandr-dev`.

Neither failure is evidence of a Gatherer semantic defect.

## Output contract

Maintain STATUS.tsv, FINAL_REPORT.md, RUN_CONTEXT.tsv, PROVENANCE.tsv,
VERIFY_CONTRACT.tsv and OUTPUT_CONTRACT.tsv. Whole-JDK completion remains false.
