# Community capabilities, compatible backports and optional module packs

Status: PROPOSED_ADMISSION_POLICY. This packet documents the next distribution work; it does not
claim a backport, third-party module, JMOD or linked product image has been built or admitted.

## Intent and existing authority

M3JDK21 should make useful Java 21 capabilities easier to obtain and maintain. It complements
OpenJDK and the Java ecosystem; projects do not need to become competitors, be renamed, or be
reimplemented to be useful parts of the distribution.

The full compatible-backport obligation remains in
[COMPATIBILITY_ADMISSION.md](../backports/COMPATIBILITY_ADMISSION.md). Community demand controls
scheduling and optional-pack selection; it must never remove a compatible upstream change from
that inventory. Security and correctness work is not popularity-contingent.

The existing [atom/pattern/tooling boundary](atom-pattern-admission-and-tooling-boundary.md)
remains authoritative. This document adds distribution planning, not a competing compatibility,
naming or recipe registry. `m3/docs/name-mapping.json` remains the naming authority.
The companion [candidate ledger](../backports/COMMUNITY_CAPABILITY_CANDIDATES.tsv) is a demand
overlay, not an implementation or compatibility verdict.

## Three delivery planes

| Plane | Meaning | Admission boundary |
| --- | --- | --- |
| CORE_BACKPORT | Changes to the JDK/HotSpot, standard modules and JDK tools | Existing complete Java 21 compatibility admission |
| OPTIONAL_PACK | Explicit modules, native resources, modular JARs, JMODs or application dependency packs | Versioned artifact and complete dependency/image proof |
| TOOLING_PACK | Maven, OpenRewrite, test tooling and development integrations | External authoring/build layer; not a java.base dependency |

An optional module can be part of the M3JDK21 distribution without becoming part of Java SE or a
mandatory JVM bootstrap dependency. The same distribution may publish a minimal base and
optional desktop, server, diagnostics, data and M3Index selections.

Do not replace Spring, JavaFX, Apache libraries or other upstream APIs merely to put M3 in a name.
Retain their identities and publish provenance. Rename owned MIndex/MatIndex components only
through the approved M3 mapping and compatibility migration. Existing Synexia modules should be
reused by versioned artifacts or reconciled source ownership, not copied into a second implementation.

## What “used by people” means

Evidence is explicit and separable from capability availability:

* ADOPTION_SURVEY: a dated survey with population and question preserved;
* USER_REQUEST: a specific request from a user, issue or downstream application;
* UPSTREAM_CAPABILITY: an upstream feature or maintained artifact exists;
* WORKLOAD_MEASUREMENT: a reproducible application experiment;
* MAINTAINER_PROPOSAL: a candidate to investigate, not measured community demand.

A feature announcement is not a popularity measurement. Stars, downloads and issue reactions may
be recorded as separate dated signals, not treated as equivalent people or unique installations.
Missing evidence is UNKNOWN, not zero demand. A failed search means NOT_FOUND_IN_THIS_PASS,
not proof that nobody uses a capability.

JetBrains' 2025 Java survey reports Spring usage at 65% and Maven at 67% among its surveyed Java
population [W5]. This supports investigating familiar Spring/Maven workflows; it does not prove a
request to install Spring into java.base or a request for a Spring JMOD.

## Backport work

Continue inventorying delivered JEP implementations, non-JEP fixes, tooling, follow-up repairs,
tests and prerequisite changes. First check whether a candidate is already present in the pinned
M3 baseline or a maintained Java 21 update stream. Do not re-import duplicate work.

Demand-oriented examples are virtual-thread monitor/pinning improvements [W6], memory-layout
improvements such as compact object headers [W8], startup/class-linking/profile work [W7], and
compiler/documentation/diagnostic improvements already represented in the repository [R1,R3].
These are planning candidates, not assertions that they are trivially backportable.

A compatible leaf inside a mixed patch remains eligible. A public API addition is not a FILE
refactor and must be admitted explicitly. A bug correction that changes existing observable
behavior must be classified and approved as such, never labelled behavior-identical.

A newer library or preview API is not compatible merely because its source compiles with Java 21.
Check standard API linkage, binary behavior, exceptions, ordering, resource ownership, reflection,
serialization, JNI/JVMTI, tool behavior and the supported OS/architecture matrix. Do not silently
replace Java 21 preview/incubator contracts with later contracts or claim that an optional adapter
is the newer standard API. Keep experimental alternatives separately named and opt-in.

## JAR, JMOD and image rules

The Java 21 jmod guide recommends modular JARs for normal deployment/module-path use and Maven
publication. JMOD is a compile/link-time container for classes, native libraries, configuration and
related resources; it is not a runtime class-path/plugin format [W1].

`jlink` accepts explicit modular JARs, JMODs and exploded modules and includes transitive module
dependencies [W2]. Therefore:

1. Keep modular JAR publication as the normal library deliverable.
2. Build JMODs when native/configuration packaging or a distributable link input is useful.
3. Produce selected `jlink` images as separate deliverables.
4. Never imply that copying a JMOD into `$JAVA_HOME/jmods` installs a runtime feature.
5. Never use `$JAVA_HOME/lib` as a third-party dependency installer.

Before a linkable pack is admitted, inspect the actual artifact's module descriptor. An
Automatic-Module-Name alone is not an explicit module descriptor. Test automatic-module
rejection, split packages, module-name collisions, descriptor edges, service providers, reflection,
resources, native loading and signatures. A generated descriptor is a candidate requiring review,
not a proof of modularization.

Use the matching M3JDK21 build's standard JMODs. Pin and verify the entire third-party dependency
closure and native OS/architecture/ABI. Newer-JDK JMODs are not interchangeable with Java 21 ones.
A native pack needs per-platform build/release proof; the JMOD filename is not portability evidence.

OpenJFX documents JDK+JavaFX images directly [W3], and Gluon publishes JMODs and minimum-JDK
requirements [W4]. JavaFX is consequently a concrete first optional-pack candidate. Select and pin
a Java-21-compatible release/build; do not substitute the newest JavaFX release without checking
its minimum JDK, licensing, native dependencies and artifact hashes.

## Scope, documentation, atomization and patternization

Every owned change keeps documentation, semantic atom identity, effect/contract envelope,
pattern/IOP participation and proof ownership together. A cohesive semantic leaf need not become
many meaningless helpers. Pattern markers must not introduce application/tooling dependencies
into the JDK product.

The unchanged authority ladder is:

`FILE -> VISIBILITY -> PACKAGE -> MODULE -> MULTI_MODULE -> LIBRARY_API`

Independent FILE candidates may run in parallel. Dependent passes stay ordered and canonical
promotion stays serial. New module descriptors, exports, requires edges, services and artifact
coordinates need their actual broader authority; a one-file diff is not automatically FILE scope.

For each recurring migration or LLM-driven coding task, the primary deliverable is a reusable
Maven/OpenRewrite recipe plus tests. Prefer the existing recipe/DAG substrate. Native/source-build
transformers may be coordinated by that control plane but must not be falsely described as Java
AST transformations.

Prove positive transformation, negative/nonmatching behavior, drift refusal, scope refusal,
representative transformed compilation, ordered composition and fixed point. An exact
preimage/postimage replay proves identity of an approved patch, not general algorithmic correctness.
Recipe tests and 99% coverage are evidence, not universal semantic-equivalence proofs.

Require measured >=99% executable line and branch coverage for newly admitted M3 Java recipe and
behavior kernels, with atom, pattern/IOP and contract tests. Keep inherited upstream coverage debt
visible with its denominator; do not claim whole-JDK 99% or hide native code under Java coverage.
Use jtreg/JDK test suites for product changes and native unit/parity/sanitizer tests as applicable.
Coverage must not be achieved by removing cases or weakening gates.

## Licensing and operational ownership

Keep OpenJDK and upstream component licenses, notices and source obligations intact. Original,
eligible M3Index work follows its Apache-2.0 policy; a rename, JNI bridge, copied file or separate
directory does not relicense upstream code. Distribution and linkage need artifact-specific review.

Each pack must identify an owner, exact release and source commit, checksum/signature evidence,
license/NOTICE, dependency lock, update policy, supported platforms, test receipts and rollback
path. Security-provider installation is explicit and must not silently reorder existing providers
or claim FIPS certification. The JCA supports multiple packaging/configuration routes [W9];
choosing one requires application proof.

No network downloads are introduced into JDK bootstrap or implicit runtime initialization.
Build-time acquisition must use verified, pinned artifacts. Rebuild linked images when their
components receive updates. Keep Maven/OpenRewrite in the tooling plane and preserve OpenJDK
configure/make as product build authority.

## Ordered work packets

| Packet | Deliverable | Exit criterion |
| --- | --- | --- |
| COMMUNITY-0 | This specification and evidence-typed candidate overlay | Documentation and ledger reviewed; no runtime admission |
| COMMUNITY-1 | Extend existing inventory recipes to capture real artifact descriptors, dependencies and provenance | JUnit-positive/negative fixtures; no invented module readiness |
| COMMUNITY-2 | JavaFX Java-21-compatible optional-pack recipe and image fixtures | Maven/JUnit + exact jmod/jlink image launch/native proof for supported targets |
| COMMUNITY-3 | Demand-prioritized existing backport packets | Compatible prerequisite closure + JDK build/jtreg/runtime proof |
| COMMUNITY-4 | Spring/Maven, diagnostics and eligible Synexia pack recipes | Reproducible application smoke tests and documented dependency/lifecycle boundaries |

These packets are a starting order, not a finite feature whitelist. Backport intake continues for
every compatible upstream change. Add further community candidates as evidence is collected.

The existing control entry is `mvn -B -ntp -f m3/pom.xml clean verify` [R3]. Product configure/make,
jtreg and final image execution remain separate required gates. This documentation packet did not
execute that reactor or build a JMOD.

## Evidence references

Repository references are inspected at `bc175dd7b5a250477479354ce9fd9d4304416fd6` or the
corresponding files read during this packet's inventory. Web sources were inspected on 2026-10-03.
They substantiate capabilities and constraints, not target-build readiness.

- [R1] m3/backports/COMPATIBILITY_ADMISSION.md
- [R2] m3/docs/atom-pattern-admission-and-tooling-boundary.md
- [R3] m3/README.md
- [W1] https://docs.oracle.com/en/java/javase/21/docs/specs/man/jmod.html
- [W2] https://docs.oracle.com/en/java/javase/21/docs/specs/man/jlink.html
- [W3] https://openjfx.io/openjfx-docs/modular
- [W4] https://gluonhq.com/products/javafx/
- [W5] https://lp.jetbrains.com/the-state-of-java-2025/
- [W6] https://mail.openjdk.org/pipermail/nio-dev/2024-November/018122.html
- [W7] https://mail.openjdk.org/pipermail/leyden-dev/2025-August/002586.html
- [W8] https://www.oracle.com/news/announcement/oracle-releases-java-25-2025-09-16/
- [W9] https://docs.oracle.com/en/java/javase/21/security/howtoimplaprovider.html
