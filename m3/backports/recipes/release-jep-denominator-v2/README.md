# Released JEP denominator convergence v2

This packet restores the complete released JEP denominator for JDK 22 through JDK 27 on the
current M3JDK21 master baseline.

## Authority

The release lists are recorded in `RELEASE_JEP_AUTHORITY.tsv`. The denominator contains 85 JEPs:

- JDK 22: 12
- JDK 23: 12
- JDK 24: 24
- JDK 25: 18
- JDK 26: 10
- JDK 27: 9

The prior 82-row catalogue omitted JEP 404, JEP 483 and JEP 521.

JDK 27 is a released line as of September 2026; it is not treated as an in-development snapshot.

## Recipe atoms

Ten source-sealed FILE atoms own authority, catalogue, pass ledger, work queue, verifier and status
surfaces. Only `module-join` is allowed to promote to MODULE scope. Repository size does not widen
the child atom authority.

All source-changing work is hash-pinned through
`M3Jdk21HashPinnedTextSnapshotRecipe`. Current-master replacements require exact SHA-256
preimages; new authority/queue/pass/status files require `ABSENT`.

## DAG projections

`packet.tsv` plus `atom-evidence.tsv` is the semantic DAG input. The existing
`M3OrchestrationProjectionMain` projects the validated graph to:

- Apache Camel Java DSL;
- Apache Airflow Python DAG;
- Drools DRL;
- a semantic root receipt.

Those projections are orchestration views only. They cannot alter dependencies, edit scope,
compatibility disposition, fixed-point requirements or serial promotion.

## Completion boundary

This packet corrects the denominator and control plane. It does not claim that all 85 JEPs, or the
14,948 released upstream commits, are implemented. Candidate/high-risk rows remain pending until
dependency closure, recipe replay, Java 21 build/jtreg/runtime proof and any required JNI/VM gates
are green.
