# Whole-JDK M3 subsystem and module disposition matrix

Status: **documentation-only planning matrix**, not migration state. Target source pin: `hsoliwal/M3jdk21@45f546ff5bcb06a1b2604f14baf998785d98d9a1`. Synexia owner inspection pin: `hsoliwal/com.synexia@3db24805d640c72ab1bd637d83561696d99561a0`.

This document supplies the whole-JDK coverage denominator requested by [whole-jdk-migration-scope.md](whole-jdk-migration-scope.md). It does **not** replace `m3/docs/name-mapping.json`, change any mapping status, or assert that package/symbol/native dependency enumeration is complete. Each row is a planning disposition that must be decomposed into stable mapping records before implementation.

## Canonical planning role

This is the canonical module/source-root planning disposition matrix for the whole-JDK documentation set. It answers “what must be reviewed and what is the initial disposition?” It does not answer “what has been implemented or verified?”

Use [../migration/docs/COVERAGE.md](../migration/docs/COVERAGE.md) for the human-readable migration/evidence projection, [whole-jdk-work-packets.md](whole-jdk-work-packets.md) for dependency-ordered execution packets, and [name-mapping.json](name-mapping.json) for operational capability state. Do not copy this matrix into a second status registry.

## Disposition vocabulary

- **replace backend**: an M3 representation may become an internal owner if compatibility evidence permits it.
- **adapt**: preserve the semantic owner/API while adapting selected storage, indexing, materialization, or consumers.
- **reuse**: current JDK representation is already compact/specialized enough that the initial plan is to keep it and reuse it as an oracle/owner.
- **retain pending evidence**: no backend change is justified until a measured or semantic case exists.
- **platform-specific**: work is inseparable from OS/CPU/runtime integration.
- **blocked/deferred**: deliberately held until named prerequisite gates close.

Routes are A = explicit stock-JVM M3 API, B = compiler lowering, C = matched custom JDK.

## Source-root denominator

The pinned `src/` directory has 70 `java.*` / `jdk.*` module directories plus `hotspot`, `demo`, and `utils`. Build, test, generated, resource, launcher and packaging surfaces outside `src/` remain part of the programme.

| Source entry | Workstream | Initial disposition | Routes | Primary boundary / first gate |
| --- | --- | --- | --- | --- |
| `java.base` | core/value/collections/IO/concurrency/security | replace backend + adapt, per family | A/B/C | bootstrap closure, public/API binary compatibility, serialization, JMM, VM assumptions |
| `java.compiler` | compiler API | retain + adapt | A/B/C | public compiler model unchanged; no dependency inversion into application tooling |
| `java.datatransfer` | desktop data transfer | retain pending evidence | A/C | flavor/serialization/native clipboard semantics |
| `java.desktop` | graphics/UI/audio/font/image | retain pending evidence + selective adapt | A/C | native peers, object identity, image/buffer formats, UI thread semantics |
| `java.instrument` | instrumentation | adapt | C | retransformation, class bytes, JVMTI/agent boundaries |
| `java.logging` | logging | retain pending evidence | A/C | bootstrap recursion and service loading |
| `java.management` | management | adapt | C | MXBean/open-type compatibility and VM counters |
| `java.management.rmi` | management/RMI | retain + adapt | C | remote serialization/protocol compatibility |
| `java.naming` | naming/JNDI | retain pending evidence | A/C | providers, factories, serialized/referral behavior |
| `java.net.http` | HTTP client | adapt | A/B/C | buffer ownership, async ordering, TLS/network contracts |
| `java.prefs` | preferences | retain pending evidence | A/C | persistence/provider format and platform stores |
| `java.rmi` | RMI | retain + adapt | C | wire compatibility, stubs, serialization, security |
| `java.scripting` | scripting API | retain | A/C | provider discovery and external engine semantics |
| `java.se` | aggregator | reuse | C | module graph only; no storage owner |
| `java.security.jgss` | GSS | retain pending evidence | C | security protocol/provider compatibility |
| `java.security.sasl` | SASL | retain pending evidence | C | provider/protocol compatibility |
| `java.smartcardio` | smart card | platform-specific | C | native PC/SC ABI and device semantics |
| `java.sql` | JDBC | retain + adapt | A/B/C | driver SPI, temporal/numeric semantics, streaming and serialization |
| `java.sql.rowset` | RowSet | retain pending evidence + selective adapt | A/C | disconnected row state, serialization, listeners |
| `java.transaction.xa` | XA | retain | C | external transaction protocol contracts |
| `java.xml` | XML | retain + selective adapt | A/B/C | parser/DOM/SAX/StAX semantics, providers, security limits |
| `java.xml.crypto` | XML crypto | retain pending evidence | C | cryptographic canonicalization/signature correctness |
| `jdk.accessibility` | accessibility bridge | platform-specific | C | desktop/native accessibility contracts |
| `jdk.attach` | attach | platform-specific + adapt | C | process attach protocol and serviceability |
| `jdk.charsets` | charsets | adapt/reuse | A/B/C | exact encoding/decoding, malformed/unmappable behavior |
| `jdk.compiler` | javac | adapt | B/C | attributed lowering only; diagnostics, classfile and annotation processing parity |
| `jdk.crypto.cryptoki` | PKCS#11 | platform-specific, retain | C | token/provider ABI; no representation shortcut may alter crypto semantics |
| `jdk.crypto.ec` | EC crypto | retain pending evidence | C | constant-time/security and vector/native correctness |
| `jdk.crypto.mscapi` | Windows crypto | platform-specific | C | Windows CAPI/CNG ABI |
| `jdk.dynalink` | dynamic linkage | retain + adapt | B/C | call-site/linker semantics and identity |
| `jdk.editpad` | tool | retain | A/C | no runtime substrate priority |
| `jdk.hotspot.agent` | serviceability agent | platform-specific + adapt | C | exact VM layout offsets and symbols |
| `jdk.httpserver` | HTTP server | retain + selective adapt | A/C | protocol and selector/buffer semantics |
| `jdk.incubator.vector` | vector API | reuse + specialize where measured | A/B/C | intrinsic/vector semantics and fallback parity |
| `jdk.internal.ed` | internal editor | retain | C | tool-only |
| `jdk.internal.jvmstat` | VM statistics | adapt | C | counter layout and attach compatibility |
| `jdk.internal.le` | line editor | retain | C | tool-only |
| `jdk.internal.opt` | option parser | retain | A/C | API behavior; no compelling storage owner yet |
| `jdk.internal.vm.ci` | JVMCI | platform-specific + adapt | C | compiler interface layouts, metadata and code install contracts |
| `jdk.internal.vm.compiler` | JVM compiler | blocked/deferred unless retained in build | C | compiler/runtime coherence and exact supported configuration |
| `jdk.internal.vm.compiler.management` | compiler management | adapt | C | management surface tied to compiler module |
| `jdk.jartool` | jar tool | adapt | A/B/C | ZIP/JAR format, manifests, signatures, reproducibility |
| `jdk.javadoc` | docs tool | retain + adapt | B/C | compiler model and output compatibility |
| `jdk.jcmd` | serviceability tool | adapt | C | diagnostic command protocol and counters |
| `jdk.jconsole` | management UI | retain | C | MXBean/JMX compatibility |
| `jdk.jdeps` | dependency tool | adapt | B/C | classfile/module graph exactness |
| `jdk.jdi` | debugger API | adapt | C | JDWP/JDI event and object identity |
| `jdk.jdwp.agent` | debugger native agent | platform-specific + adapt | C | JDWP/JVMTI/native ABI |
| `jdk.jfr` | flight recorder | adapt | C | event layout, timestamps, checkpoints and VM integration |
| `jdk.jlink` | image linker | adapt | B/C | module image format and reproducibility |
| `jdk.jpackage` | packaging | platform-specific + retain | C | OS packaging formats/tools |
| `jdk.jshell` | REPL | retain + adapt | B/C | compiler/runtime class loading and snippets |
| `jdk.jsobject` | browser bridge API | retain | C | compatibility-only |
| `jdk.jstatd` | monitoring daemon | retain + adapt | C | JVMStat/RMI protocol |
| `jdk.localedata` | locale resources | reuse | A/C | generated/resource correctness, size measurements before redesign |
| `jdk.management` | management extensions | adapt | C | VM counters and MXBeans |
| `jdk.management.agent` | management agent | retain + adapt | C | service loading/network/JMX boundary |
| `jdk.management.jfr` | JFR management | adapt | C | JFR/MXBean compatibility |
| `jdk.naming.dns` | DNS naming provider | retain | C | provider/protocol semantics |
| `jdk.naming.rmi` | RMI naming provider | retain | C | provider/RMI semantics |
| `jdk.net` | extended networking | platform-specific + adapt | C | socket options/native ABI |
| `jdk.nio.mapmode` | mapped IO modes | adapt/reuse | A/C | persistence/durability semantics and platform support |
| `jdk.random` | random generators | retain + specialize only with proof | A/B/C | exact algorithm streams/contracts and statistical requirements |
| `jdk.sctp` | SCTP | platform-specific | C | native network ABI |
| `jdk.security.auth` | auth | retain | C | provider/login subject semantics |
| `jdk.security.jgss` | GSS extensions | retain pending evidence | C | protocol/provider compatibility |
| `jdk.unsupported` | Unsafe | platform-specific + adapt | C | object layout, memory access, fences and external dependencies |
| `jdk.unsupported.desktop` | desktop compatibility | retain | C | compatibility module |
| `jdk.xml.dom` | DOM APIs | retain | C | API compatibility |
| `jdk.zipfs` | ZIP filesystem | adapt | A/C | ZIP format, channels, concurrency and filesystem semantics |
| `hotspot` | VM/runtime | platform-specific + replace/adapt only behind gates | C | interpreter/C1/C2, GC, object layout, barriers, intrinsics, CDS, JNI/JVMTI/JFR/serviceability, OS/CPU |
| `demo` | examples | retain | A/C | test/documentation consumer only |
| `utils` | source utilities | retain + adapt as build requires | B/C | generated/build-time tools, never bootstrap runtime dependency |

## Surfaces outside `src/`

| Surface | Disposition | Required inventory |
| --- | --- | --- |
| `make/`, `configure`, top-level `Makefile` | retain + adapt | feature flags, generated source rules, native library linkage, module images, toolchain/platform matrices |
| `test/` | retain as oracle; extend | jtreg/native/JVM tests, problem lists, stress suites, serialization fixtures, failure expectations |
| generated sources/resources | adapt through producer | generator inputs, generated ABI/resource formats, regeneration determinism |
| launchers/native libraries | platform-specific | C entry points, JNI/JVMTI exports, OS APIs, symbol/versioning contracts |
| packaging/images | adapt | jmods, modules image, CDS archives, installers, symbols/debug images |
| third-party/bundled notices | retain | exact provenance, license, source offer/notice obligations |

## Mandatory decomposition fields

Before a planning row can become an implementation mapping, record at least:

1. stable capability/mapping ID;
2. sealed contract boundary and source/target atom DAG roots;
3. atom IDs, typed dependencies, reverse consumers and unresolved SCC/compound atoms;
4. applicable pattern IDs/versions, preconditions/refusals and atom/pattern coverage receipts;
5. explicit source-atom → pattern/recipe → target-atom mapping edges;
6. exact target module/package/symbol/native/resource/build surface;
7. current semantic and physical owner;
8. public/internal API, ABI, serialization/format and service contracts;
9. identity, mutability, lifetime, GC/JMM and bootstrap requirements;
10. proposed Synexia owner or explicit reason to retain JDK ownership;
11. eligible routes A/B/C and refusal cases;
12. deterministic recipe/source transformation identity where applicable;
13. differential, build, runtime, performance and rollback gates.

## Known existing Synexia owners to evaluate before inventing new foundations

At the inspected Synexia pin, the following are real candidate owners/references, not automatically approved JDK replacements:

- `synexia-mat-collections`: primitive/adaptive immutable collection layouts, ID pools, `MatRecordArena`, intrusive handles and explicit Java materialization boundaries.
- `synexia-mindex/collections`: MIndex spaces, primitive-ID mutable collections, canonical frozen collections, composite identity and explicit `MIndexJavaViews`.
- `synexia-common/com.synexia.common.collections`: broad packed/segmented primitive and JDK-shaped collection machinery, including packed hash/tree/list/deque/weak/transfer/concurrent families.
- `synexia-indexstring`: text atoms, immutable backing, composition, indexing/precompute and explicit compatibility projections.
- `m3-java-contracts`: API/ABI/format contract snapshots, deterministic change catalogues and execution receipts.
- `synexia-openrewrite-recipes`: reusable transformation crate infrastructure; it remains tooling, not a java.base runtime dependency.

Selection requires semantic compatibility and exact source review. Similar names, benchmark folklore, hashes, signals or donor category labels are not equivalence proofs.

## Promotion rule

A module remains in this denominator until every relevant package/symbol/native/resource/build surface has a stable disposition and every changed target has evidence scoped to the exact candidate. Module-directory coverage alone is never “whole JDK migrated.”
