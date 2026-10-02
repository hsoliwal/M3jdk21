# Whole-JDK inventory framework and subsystem migration matrix

Status: **all source-root directories accounted for; recursive file, symbol, dependency and native-consumer closure remains OPEN**.
This is a documentation projection, not a replacement migration registry or proof that the JDK has been migrated.
Read [architecture](whole-jdk-architecture.md), [collections](whole-jdk-collections.md), [text](whole-jdk-text-operations.md), and [execution packets](whole-jdk-execution.md).

## 1. Pinned scope and honest coverage

Target source revision: `45f546ff5bcb06a1b2604f14baf998785d98d9a1`.
The inspected [repository root](https://github.com/hsoliwal/M3jdk21/tree/45f546ff5bcb06a1b2604f14baf998785d98d9a1) identifies `src` tree `868e084a3c2c9d8adbca7f19aa39aeedd279c7e4`. Its [nonrecursive Git tree response](https://api.github.com/repos/hsoliwal/M3jdk21/git/trees/868e084a3c2c9d8adbca7f19aa39aeedd279c7e4) reports `truncated=false` and 73 directory entries. Seventy names start with `java.` or `jdk.`; the other three are `demo`, `hotspot`, and `utils`. Names alone do not establish JPMS descriptor presence, successful compilation or membership in a particular platform image.

The table below accounts for every one of those entries exactly once. Paths resolve under `src/` at the pinned revision; relative links are navigational conveniences in this documentation-only branch. Counts must not be generalized to later trees.

| Coverage layer | Result for this documentation pass |
|---|---|
| Repository top-level ownership surfaces | Observed and assigned below |
| Complete `src` immediate-child list | 73 of 73 accounted for, nontruncated source-tree response |
| All recursive tracked files, blobs and generated-input relationships | OPEN; no complete file inventory emitted here |
| All packages, classes, public/protected and internal symbols | OPEN; selected bodies and existing evidence inspected, not a full parser run |
| All native symbols, generated symbols and platform/build variants | OPEN |
| Closed runtime-consumer, bootstrap and external dependency graph | OPEN |
| Capability migration and behavior acceptance | OPEN; directory coverage is not migration progress |

A complete inventory framework is delivered here. A complete semantic inventory is **not** claimed. Contributors must close the explicit lower-level obligations rather than convert this directory table into an unsupported completion percentage.

## 2. Dispositions and packets

`ADAPT` means review a bounded internal reuse while retaining the exposed contract. `RETAIN_PENDING_EVIDENCE` keeps the current owner until a benefit and compatibility case exist. `PLATFORM_SPECIFIC` requires separate platform/provider/native review. `DEFERRED` names a visible lower-priority or build-conditional obligation, not an omitted one. A future per-capability record may select `REPLACE_BACKEND`, `REUSE`, or `BLOCKED` after owner and contract review.

The `WP-*` labels below are documentary work-packet labels defined in the execution document, not new operational capability IDs. Mixed-module decisions must be refined to package/symbol scope. Every row additionally requires WP-00 inventory and WP-01 ownership/mapping gates.

## 3. Complete source-root disposition table

| Source directory | Packet | Preliminary disposition | Scope, reason and decisive acceptance boundary |
|---|---|---|---|
| [demo](../../src/demo) | WP-15 | RETAIN_PENDING_EVIDENCE | Examples remain compatibility workloads; do not count demo rewrites as runtime replacement. |
| [hotspot](../../src/hotspot) | WP-11 | ADAPT | Coordinated layouts, GC, interpreter, JIT, intrinsics and serviceability; exact complete-image gates. |
| [java.base](../../src/java.base) | WP-01 through WP-12, WP-16, WP-17 | ADAPT | Decompose text, collections, threads, streams, IO, values, loading, security and native consumers; no blanket backend. |
| [java.compiler](../../src/java.compiler) | WP-10 | RETAIN_PENDING_EVIDENCE | Preserve compiler/model interfaces; indexed implementations must retain types, elements and diagnostics. |
| [java.datatransfer](../../src/java.datatransfer) | WP-13 | PLATFORM_SPECIFIC | Transfer flavors, clipboard/native lifecycle and identity cannot be replaced by text IDs alone. |
| [java.desktop](../../src/java.desktop) | WP-13 | PLATFORM_SPECIFIC | AWT/Swing, fonts, imaging, printing, sound and native peers; threading and resource acceptance per platform. |
| [java.instrument](../../src/java.instrument) | WP-09, WP-11 | ADAPT | Transformation/redefinition and class identity; instrumented matched-image tests. |
| [java.logging](../../src/java.logging) | WP-14 | ADAPT | Bounded immutable metadata candidates; retain logger hierarchy, handlers, configuration and caller behavior. |
| [java.management.rmi](../../src/java.management.rmi) | WP-14, WP-15 | ADAPT | Management transport adapters only after protocol, security and remote identity tests. |
| [java.management](../../src/java.management) | WP-15 | ADAPT | Match management metadata/counters to VM implementation; preserve public management contracts. |
| [java.naming](../../src/java.naming) | WP-14 | ADAPT | Provider-scoped names/indexes; retain context, naming exceptions, resource closure and external provider behavior. |
| [java.net.http](../../src/java.net.http) | WP-07 | ADAPT | Buffer/segment reuse with async ownership, cancellation, flow control and protocol tests. |
| [java.prefs](../../src/java.prefs) | WP-14, WP-16 | RETAIN_PENDING_EVIDENCE | Platform persistence, listeners/events and cross-process behavior need dedicated review. |
| [java.rmi](../../src/java.rmi) | WP-14 | RETAIN_PENDING_EVIDENCE | Preserve remote object identity, serialization, distributed lifetime and wire compatibility. |
| [java.scripting](../../src/java.scripting) | WP-14 | RETAIN_PENDING_EVIDENCE | External engines/bindings expose arbitrary objects; no speculative primitive substitution. |
| [java.se](../../src/java.se) | WP-15 | RETAIN_PENDING_EVIDENCE | Aggregate module surface; preserve descriptor/resolve behavior, not a storage owner. |
| [java.security.jgss](../../src/java.security.jgss) | WP-12, WP-16 | PLATFORM_SPECIFIC | Authentication contexts, credentials and native/provider boundaries require security review. |
| [java.security.sasl](../../src/java.security.sasl) | WP-12 | RETAIN_PENDING_EVIDENCE | Retain authentication state and provider contracts; do not globally intern secrets. |
| [java.smartcardio](../../src/java.smartcardio) | WP-12, WP-16 | PLATFORM_SPECIFIC | Device handles, transport errors and provider lifecycle require real-device/authorized simulator gates. |
| [java.sql.rowset](../../src/java.sql.rowset) | WP-14 | ADAPT | Metadata/index opportunities; preserve cursor, disconnected state, updates and serialization. |
| [java.sql](../../src/java.sql) | WP-14 | RETAIN_PENDING_EVIDENCE | JDBC contracts and driver boundaries remain authoritative; driver objects are not shared immutable atoms. |
| [java.transaction.xa](../../src/java.transaction.xa) | WP-14 | RETAIN_PENDING_EVIDENCE | Preserve XA identifiers, exceptions and transaction protocol boundaries. |
| [java.xml.crypto](../../src/java.xml.crypto) | WP-12, WP-14 | RETAIN_PENDING_EVIDENCE | XML canonicalization/signatures are security semantics, not generic text normalization. |
| [java.xml](../../src/java.xml) | WP-14 | ADAPT | Parser vocabulary and immutable tables are candidates; preserve namespace/order/entity/security behavior. |
| [jdk.accessibility](../../src/jdk.accessibility) | WP-13 | PLATFORM_SPECIFIC | Preserve assistive technology and native/desktop accessibility interfaces. |
| [jdk.attach](../../src/jdk.attach) | WP-15, WP-16 | PLATFORM_SPECIFIC | Process attachment permissions, protocols and platform lifecycle. |
| [jdk.charsets](../../src/jdk.charsets) | WP-02, WP-17 | ADAPT | Reuse verified charset tables; preserve stateful encoding, malformed policies and provider lookup. |
| [jdk.compiler](../../src/jdk.compiler) | WP-10 | ADAPT | Indexed syntax/attribution candidates; preserve diagnostics, processing, options and emitted semantics. |
| [jdk.crypto.cryptoki](../../src/jdk.crypto.cryptoki) | WP-12, WP-16 | PLATFORM_SPECIFIC | PKCS#11/native token state, ownership, secret handling and provider compatibility. |
| [jdk.crypto.ec](../../src/jdk.crypto.ec) | WP-12 | RETAIN_PENDING_EVIDENCE | Retain cryptographic algorithms until separate correctness, timing and provider review. |
| [jdk.crypto.mscapi](../../src/jdk.crypto.mscapi) | WP-12, WP-16 | PLATFORM_SPECIFIC | Windows provider and native handle contracts. |
| [jdk.dynalink](../../src/jdk.dynalink) | WP-09 | ADAPT | Class-loader-scoped linkage facts only; preserve guards, invalidation and call-site behavior. |
| [jdk.editpad](../../src/jdk.editpad) | WP-13, WP-15 | RETAIN_PENDING_EVIDENCE | Tool UI; benefit and event-thread contracts before internal text adaptation. |
| [jdk.hotspot.agent](../../src/jdk.hotspot.agent) | WP-11, WP-15 | ADAPT | Every changed VM layout must have matching agent readers and real dump/live-target tests. |
| [jdk.httpserver](../../src/jdk.httpserver) | WP-07 | ADAPT | Internal buffers/metadata; preserve protocol, filters, executors and request lifecycle. |
| [jdk.incubator.vector](../../src/jdk.incubator.vector) | WP-08, WP-11 | PLATFORM_SPECIFIC | Species, lane semantics, masks and supported lowering per CPU/configuration. |
| [jdk.internal.ed](../../src/jdk.internal.ed) | WP-15 | RETAIN_PENDING_EVIDENCE | Internal editing-tool contract and measured benefit precede reuse. |
| [jdk.internal.jvmstat](../../src/jdk.internal.jvmstat) | WP-15 | ADAPT | Counter formats and lifecycle must agree with VM producers and monitoring tools. |
| [jdk.internal.le](../../src/jdk.internal.le) | WP-15 | RETAIN_PENDING_EVIDENCE | Terminal/editor semantics and dependencies require owner review. |
| [jdk.internal.opt](../../src/jdk.internal.opt) | WP-15 | RETAIN_PENDING_EVIDENCE | Option parsing is not automatically a whole-JDK storage bottleneck; retain behavior. |
| [jdk.internal.vm.ci](../../src/jdk.internal.vm.ci) | WP-11 | ADAPT | VM/compiler interface agreement, constants, metadata and code installation gates. |
| [jdk.internal.vm.compiler.management](../../src/jdk.internal.vm.compiler.management) | WP-11, WP-15 | DEFERRED | Resolve actual build/descriptor status first; directory presence is not an enabled implementation. |
| [jdk.internal.vm.compiler](../../src/jdk.internal.vm.compiler) | WP-11 | DEFERRED | Resolve selected-build ownership before compiler integration; do not assume presence or absence of an active backend. |
| [jdk.jartool](../../src/jdk.jartool) | WP-07, WP-15 | ADAPT | Archive metadata/name reuse; preserve formats, manifests, signatures and reproducibility policies. |
| [jdk.javadoc](../../src/jdk.javadoc) | WP-10, WP-15 | ADAPT | Reuse attributed source facts; preserve doclet interfaces, positions and diagnostics. |
| [jdk.jcmd](../../src/jdk.jcmd) | WP-15 | ADAPT | Diagnostic command contracts and VM metadata agreement. |
| [jdk.jconsole](../../src/jdk.jconsole) | WP-13, WP-15 | ADAPT | Management views may reuse immutable metadata; UI/connection lifecycle remains intact. |
| [jdk.jdeps](../../src/jdk.jdeps) | WP-10, WP-15 | ADAPT | Typed dependency graphs preserve module/class/edge distinctions and reporting. |
| [jdk.jdi](../../src/jdk.jdi) | WP-09, WP-15 | ADAPT | Debugger mirror identity, suspension, exceptions and transport semantics. |
| [jdk.jdwp.agent](../../src/jdk.jdwp.agent) | WP-11, WP-16 | PLATFORM_SPECIFIC | Native agent, object IDs, pinning/references and debugger protocol acceptance. |
| [jdk.jfr](../../src/jdk.jfr) | WP-11, WP-15 | ADAPT | Event metadata/storage reuse requires matching recorder, reader, schema and runtime paths. |
| [jdk.jlink](../../src/jdk.jlink) | WP-15 | ADAPT | Module/resource indexes; preserve plugins, services, image format and launchability. |
| [jdk.jpackage](../../src/jdk.jpackage) | WP-15, WP-16 | PLATFORM_SPECIFIC | Packaging/installers and runtime-image composition per target OS. |
| [jdk.jshell](../../src/jdk.jshell) | WP-09, WP-10, WP-15 | ADAPT | Incremental facts must respect snippets, redefinition, loaders, diagnostics and execution. |
| [jdk.jsobject](../../src/jdk.jsobject) | WP-13, WP-14 | RETAIN_PENDING_EVIDENCE | External JavaScript object boundary; preserve public identity and calls. |
| [jdk.jstatd](../../src/jdk.jstatd) | WP-15 | ADAPT | Monitoring transport/counters and security boundary. |
| [jdk.localedata](../../src/jdk.localedata) | WP-17 | ADAPT | Versioned immutable locale resources; preserve provider order, locale fallback and outputs. |
| [jdk.management.agent](../../src/jdk.management.agent) | WP-15 | ADAPT | Agent initialization, configuration, transport and permission contracts. |
| [jdk.management.jfr](../../src/jdk.management.jfr) | WP-15 | ADAPT | Recorder/management agreement and event/recording lifecycle. |
| [jdk.management](../../src/jdk.management) | WP-15 | ADAPT | Platform VM extensions and metric meaning must remain matched. |
| [jdk.naming.dns](../../src/jdk.naming.dns) | WP-14, WP-16 | RETAIN_PENDING_EVIDENCE | DNS/provider caches need TTL, context, security and protocol review, not global permanent facts. |
| [jdk.naming.rmi](../../src/jdk.naming.rmi) | WP-14 | RETAIN_PENDING_EVIDENCE | Naming/RMI identity, security and transport boundaries. |
| [jdk.net](../../src/jdk.net) | WP-07, WP-16 | PLATFORM_SPECIFIC | Socket options and native networking behavior. |
| [jdk.nio.mapmode](../../src/jdk.nio.mapmode) | WP-07, WP-16 | PLATFORM_SPECIFIC | Mapping modes, persistence and platform support must be tested explicitly. |
| [jdk.random](../../src/jdk.random) | WP-08 | RETAIN_PENDING_EVIDENCE | Generator algorithm, splitting/jumping/state and statistical contracts; no speculative cached results. |
| [jdk.sctp](../../src/jdk.sctp) | WP-07, WP-16 | PLATFORM_SPECIFIC | Native protocol, message boundaries and association lifecycle. |
| [jdk.security.auth](../../src/jdk.security.auth) | WP-12, WP-16 | PLATFORM_SPECIFIC | Authentication modules, principals and credential lifecycle. |
| [jdk.security.jgss](../../src/jdk.security.jgss) | WP-12, WP-16 | PLATFORM_SPECIFIC | Extended security/provider behavior and native/context ownership. |
| [jdk.unsupported.desktop](../../src/jdk.unsupported.desktop) | WP-13 | RETAIN_PENDING_EVIDENCE | Legacy desktop compatibility remains visible; unsupported does not mean safe to remove. |
| [jdk.unsupported](../../src/jdk.unsupported) | WP-09, WP-11 | RETAIN_PENDING_EVIDENCE | Low-level compatibility surfaces must be reconciled with layout and memory changes. |
| [jdk.xml.dom](../../src/jdk.xml.dom) | WP-14 | RETAIN_PENDING_EVIDENCE | Preserve DOM interfaces and graph identity/order semantics. |
| [jdk.zipfs](../../src/jdk.zipfs) | WP-07, WP-14 | ADAPT | Archive paths and metadata candidates; preserve filesystem/provider, update and encoding semantics. |
| [utils](../../src/utils) | WP-15 | RETAIN_PENDING_EVIDENCE | Build/support utilities need explicit owners and generated-output dependency links. |

## 4. Surfaces outside `src`

The inspected repository root also contains `make`, `bin`, `configure`, `Makefile`, `test`, `doc`, `.github`, `.jcheck`, `m3`, `README.md`, `CONTRIBUTING.md`, `.gitignore`, `.gitattributes`, `LICENSE`, `ADDITIONAL_LICENSE_INFO`, and `ASSEMBLY_EXCEPTION`.

`make`, `bin`, configure and Makefile are build/generation inputs, not Maven modules; retain the existing build and map any generator/template/native-config dependencies through WP-00/WP-15/WP-16. `test` contributes Java, native, VM, compiler, microbenchmark and test-library consumers; register those alongside production symbols. CI and `.jcheck` are evidence/quality policy surfaces, not obstacles to disable. `doc`, READMEs and contributor guidance remain documentation owners. Git attributes and ignore policy affect checkout/reproducibility and cannot silently exclude tracked obligations.

`m3` contains the existing core, ports, algorithms, compatibility, lexicon, recipes, migration, history, tooling and evidence owners. Preserve their recipes, hashes and separate runtime histories. License/notice files remain unchanged; provenance review must inspect applicable per-file and third-party notices rather than infer a repository-wide license from a directory name.

## 5. Required recursive inventory records

Extend the existing inventory producer with an OpenJDK build-topology adapter; do not treat its existing Maven-topology support as proof that it already understands this build. Reuse its Java parsing and existing indexed storage where compatible. Native/build/generated surfaces require the appropriate pinned parser or build-derived producer rather than forcing C++ or templates through a Java parser.

For each tracked item, record repository and commit, path and Git object identity, raw-byte SHA-256, kind, source root, platform/build condition, owning module/package, generated-input/output relation, provenance and source-obligation identity. Keep binary, extensionless, test, resource, template and support files visible. An exclusion requires a reason and counts toward the denominator as an exclusion, not as successful migration.

For each Java declaration, record the fully qualified owner, nesting, signature and descriptor where resolved, visibility, generic/annotation/throws contract, field mutability, native and synchronization flags, inheritance/default-method relations, service and module visibility. Distinguish parsed syntax from resolved attribution and emitted binary ABI. Compiler options, preview status, source variant and dependencies are part of the producer pin. A parse failure remains a diagnostic-backed blocked item.

For each native/runtime surface, record exported or registered entry point, Java counterpart where present, C/C++ signature/calling convention, generated headers, layout offsets, GC barriers/rooting, lifetimes, platform condition and runtime consumers. Track interpreter, compiled/intrinsic, reflection, serialization, agents, image/CDS and diagnostics readers separately. Build-conditioned absence must not be reported as global absence.

For each dependency, preserve edge kind: initialization, linking, allocation/GC, call, field/layout, module access, service lookup, generation, native ABI, format or testing. Keep direction and conditions. Unknown edges prevent closure; they must not disappear when converting to a graph index.

## 6. Finite acquisition and closure procedure

1. Pin the source commit and producer revision independently. Enumerate tracked Git objects from that commit, not a dirty working directory. Retain complete traversal boundaries and verify `truncated=false` or paginate/decompose until every subtree is accounted for.
2. Inventory build descriptors, templates and selected-platform source variants. Discover real JPMS modules from their descriptors/build, not the 70 directory names. Preserve dormant/build-conditional roots.
3. Reuse the existing Java declaration producer, then add appropriate native/generated/build passes. Record the exact bytes, options, diagnostics and projection rules for every item. Neutral staging is an acquisition detail, not inferred module ownership or type proof.
4. Resolve declaration and runtime-consumer dependencies to a fixed point, retaining unresolved items. Inspect dynamic service/reflection/native registrations with explicit uncertainty instead of assuming static calls are exhaustive.
5. Join source obligations to the existing target capability map by reviewed semantic ownership. A path-derived source-obligation ID is not automatically a stable capability identity. Splits and consolidations can be many-to-many.
6. Assign per-capability dispositions, owner, route, packet dependencies, acceptance requirements and open decisions. Register unclassified items; never silently drop a root, package, native consumer or platform.
7. Emit bounded derived projections and counts from the same owner. Report discovered/parsed/resolved/contract-reviewed/ported/retained/verified counts separately, with included/excluded/blocked denominators.
8. Reconcile deltas on every new source or target revision. Preserve old receipts and tombstones; only new evidence can advance a changed capability.

### Closure invariants

Every discovered source item has one accountable obligation, possibly linked to multiple capabilities. Every capability has an explicit disposition and owner. Every public/native/format boundary has acceptance requirements. Every blocked or excluded item retains a reason. Every output projection binds to its producer and input hashes. A tool reporting a valid inventory of failures is not reporting successful source admission.

The following must remain failed/open until actual evidence exists: incomplete traversal, unresolved module ownership, missing native reader, generator without inputs, unattributed required type, conflicting source owners, undocumented identity conversion, inaccessible provenance, unowned mapping delta, or a receipt from another tree/configuration. The implementation team can find these directly from the records rather than asking the user to list missing subsystems.
