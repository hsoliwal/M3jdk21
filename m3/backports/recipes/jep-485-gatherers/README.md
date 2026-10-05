# JEP 485 Stream Gatherers — opt-in M3JDK21 packet

Status: explicit post-Java-21 SE API extension candidate; **not** part of default Java 21 identity.

## Policy

The authoritative post-21 compatibility ledger classifies JEP 485 as:

```text
classification  OPT_IN_SE_API_EXTENSION
default_java21  NO
owner_lane      API
```

The reason is mechanical: final JEP 485 adds public `java.util.stream` / `java.base` API.
M3JDK21 therefore retains the donor/postimage recipe and can build/test an explicitly selected
extended image, but stock `master` does not expose Gatherer/Gatherers by default.

## Donor authority

- feature: JEP 485 — Stream Gatherers
- final donor state: OpenJDK JDK 24 GA tag `jdk-24+36`
- package: `java.util.stream` in `java.base`
- Java grammar: unchanged
- class-file version dependency: none for the imported source

Cumulative donor lineage is recorded in `UPSTREAM_COMMITS.tsv`.

## Java 21 adaptation

JDK21 already provides the virtual-thread, SharedSecrets/JavaUtilCollectionAccess and stream node
machinery used by the final implementation.

The JDK24 implementation references `jdk.internal.invoke.MhUtil`, which is not present in the
Java 21 target. The retained postimage adapts only that VarHandle lookup leaf to
`MethodHandles.Lookup.findVarHandle(...)` with equivalent fail-fast `InternalError` behavior.
No unrelated post-21 helper is imported.

## Recipe-first work unit

`M3Jep485StreamGatherersBackportRecipe` composes the existing 15-target hash-pinned Java crate:

`jdk24-jep485-stream-gatherers`

Four modified Java 21 stream files must match exact current-master SHA-256 preimages; all new API,
implementation and jtreg files must be absent. JUnit proves exact replay, drift refusal, named recipe
discovery and second-pass fixed point.

Authority is:

`LIBRARY_API + EXPLICIT_CONTRACT_CHANGE`

This is intentionally broader than FILE/PACKAGE/MODULE mechanical authority because the packet adds
public SE API.

## CI materialization

The repository branch carries recipe/evidence only. The workflow:

1. proves recipe JUnit and scope registration;
2. validates `packet.tsv` + `atom-evidence.tsv`;
3. projects the packet through the canonical Camel/Airflow/Drools DAG;
4. verifies exact current-master preimages;
5. materializes the retained postimages **only inside the ephemeral runner checkout**;
6. configures/builds an extended JDK image;
7. runs the focused Gatherer jtreg family;
8. runs existing stream regression tests;
9. compiles/runs a built-JDK API smoke;
10. uploads proof receipts.

No product source is committed by this packet and no default-Java21 promotion is permitted.

## Completion boundary

This packet is successful only when the opt-in workflow is green for the exact branch head. A green
opt-in packet still does not authorize changing `POST21_PRIORITY_COMPATIBILITY.tsv` to default
Java21 = YES. That policy would require an explicit Java identity decision separate from technical
build/test success.
