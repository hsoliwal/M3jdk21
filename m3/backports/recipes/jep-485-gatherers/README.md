# JEP 485 Stream Gatherers — M3JDK21 compatibility packet

Status: candidate implementation packet; not a completion claim.

## Authority

- final feature: JEP 485, Stream Gatherers
- final release donor: OpenJDK JDK 24 GA tag `jdk-24+36`
- package: `java.util.stream` in `java.base`
- Java language grammar: unchanged
- class-file version requirement: unchanged for the API/implementation source

## Upstream lineage retained

The final JDK24 GA source includes the original preview implementation plus fixes and graduation:

- `33b26f79a986d015abdcd84b89842adc0a4bde64` — initial Stream Gatherers implementation
- `ab28045d7785d948b2bce685f06043e8217961f4` — sequential finisher/short-circuit fix
- `8a5b86c52954f6917acfda11df183691beb07f56` — nested/recursive flat-map support
- `384deda65fd63e23d4caaaa9762f2ac80de78029` — internal VarHandle utility adoption
- `ef0dc2518e7636cc8a9ca580613ff5edeb4c19fd` — graduation from Preview
- `450636ae28b84ded083b6861c6cba85fbf87e16e` — mapConcurrent interruption/finishing fix

The backport uses the JDK24 GA source state rather than freezing the earlier preview implementation.

## Java 21 compatibility audit

JDK21 already provides:

- virtual threads / `Thread.ofVirtual()`;
- `SharedSecrets.getJavaUtilCollectionAccess()`;
- `JavaUtilCollectionAccess.listFromTrustedArrayNullsAllowed(...)`;
- `Nodes.castingArray()`.

JDK24 `GathererOp` imports `jdk.internal.invoke.MhUtil`, which is not present in the JDK21 target.
M3 adapts only that helper use to a local `MethodHandles.Lookup.findVarHandle(...)` wrapper with
equivalent fail-fast `InternalError` behavior. Importing the unrelated post-21 `MhUtil` utility is
not required by the Gatherer contract.

## Production target set

New JDK24 GA classes:

- `java/util/stream/Gatherer.java`
- `java/util/stream/GathererOp.java` (Java21-adapted MhUtil leaf)
- `java/util/stream/Gatherers.java`

Existing JDK21 stream integration targets:

- `AbstractPipeline.java`
- `ReferencePipeline.java`
- `Stream.java`
- `package-info.java`

The final API is imported without PreviewFeature annotations or preview enablement.

## Contract / scope

`Stream.gather(Gatherer)` and the public Gatherer/Gatherers APIs are additive public `java.base`
API. Mutation authority is therefore `LIBRARY_API`, even though individual source edits are
mechanically decomposable.

## Required proof

1. exact current-master preimages;
2. source-sealed OpenRewrite recipe and recipe JUnit;
3. JDK24 GA postimage provenance;
4. Java 21 compile / java.base build;
5. upstream Gatherer API/behavior jtreg adapted to Java 21;
6. sequential, parallel, short-circuit and recursive-flat-map tests;
7. `mapConcurrent` virtual-thread/interruption tests;
8. existing Stream regression tests;
9. second-pass recipe fixed point;
10. no performance claim without matched benchmark evidence.

No compatibility or completion claim is made until these gates are green.
