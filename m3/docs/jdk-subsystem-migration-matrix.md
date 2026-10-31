# Whole-JDK inventory framework and subsystem migration matrix

Status: documentation-only inventory specification. This file seeds the dependency-aware programme. It does **not** claim that every package/symbol/native edge has already been scanned on this documentation branch.

The authoritative completion rule is mechanical: every relevant source/build/test item in the pinned JDK tree must appear in the generated inventory or in a reviewed exclusion. A hand-written matrix is the programme map, not proof of complete source coverage.

## 1. Inventory row schema

Every generated row needs at least:

| Field | Meaning |
| --- | --- |
| inventoryId | stable row identifier |
| sourceCommit | exact JDK commit |
| module | Java module or non-module runtime component |
| sourceRoot | exact repository path |
| packageOrComponent | Java package, HotSpot/native component, build owner or resource owner |
| symbol | qualified public/internal/native symbol when applicable |
| surface | public API, internal API, VM, native, build, generated, resource or test |
| owner | current semantic/storage owner |
| mutability | immutable, mutable, versioned, external |
| identity | value/object/reference/owner/generation identity rules |
| nativeConsumers | JNI/JVMTI/launcher/library consumers |
| vmConsumers | interpreter/JIT/GC/CDS/serviceability consumers |
| generatedBy | generator/build input if generated |
| dependencies | module/symbol/capability edges |
| routeA | explicit API applicability |
| routeB | compiler-lowering applicability |
| routeC | custom-JDK applicability |
| disposition | replace-backend, adapt, reuse, retain-pending-evidence, platform-specific, blocked, deferred, exclude-custody |
| mappingIds | existing migration capability IDs |
| tests | source tests and acceptance suites |
| evidenceState | discovered, contract-reviewed, implemented, verified, accepted, blocked, drifted |
| openQuestions | unresolved contract or ownership issues |

A package row never substitutes for a symbol row when a native, reflective, serialization or subclass contract is symbol-specific.

## 2. Mechanical discovery requirements

The inventory toolchain must enumerate from the pinned repository rather than a curated allow-list.

### JDK modules

Discover every module root under `src/` and read each `module-info.java` where present. Capture:

- exports and qualified exports;
- opens;
- requires / requires transitive / static requires;
- uses/provides;
- platform-specific source overlays;
- generated sources and data;
- launchers and native libraries.

### Java source

For every Java compilation unit capture:

- package;
- top-level/nested types;
- public/protected signatures;
- package-private/internal owners that participate in compatibility;
- serialization IDs/forms;
- reflective/service registrations;
- native declarations;
- Unsafe/VarHandle/MethodHandle use;
- synchronized/volatile/atomic fields;
- public/protected subclass hooks.

### Native and VM source

Capture:

- HotSpot components under `src/hotspot`;
- native libraries under module `lib` trees;
- launcher entry points;
- JNI/JVMTI/JVM TI agents;
- generated headers;
- platform-specific implementations;
- object-layout and GC/JIT consumers.

### Tests

Bind tests to owners rather than storing a flat test list:

- jtreg groups;
- JCK/TCK where externally available;
- hotspot tests;
- serviceability/JFR;
- native tests;
- serialization golden streams;
- platform-specific tests;
- performance harnesses.

### Build and generated artifacts

Inventory:

- `make/` rules;
- gensrc/gendata;
- module lists;
- CDS inputs;
- generated constants/tables;
- native generated headers;
- jimage/jlink/jmod packaging;
- symbols used for `--release`.

## 3. Module seed map

The following is the programme's JDK 21 module seed. The generated scan must confirm exact presence for the pinned fork and add platform-conditional modules discovered by the build. Missing or extra modules are inventory differences to record, not reasons to edit this list silently.

### Java SE/API modules

- `java.base`
- `java.compiler`
- `java.datatransfer`
- `java.desktop`
- `java.instrument`
- `java.logging`
- `java.management`
- `java.management.rmi`
- `java.naming`
- `java.net.http`
- `java.prefs`
- `java.rmi`
- `java.scripting`
- `java.se`
- `java.security.jgss`
- `java.security.sasl`
- `java.smartcardio`
- `java.sql`
- `java.sql.rowset`
- `java.transaction.xa`
- `java.xml`
- `java.xml.crypto`

### JDK implementation/tooling modules

- `jdk.accessibility`
- `jdk.attach`
- `jdk.charsets`
- `jdk.compiler`
- `jdk.crypto.cryptoki`
- `jdk.crypto.ec`
- `jdk.crypto.mscapi` where the platform build provides it
- `jdk.dynalink`
- `jdk.editpad`
- `jdk.hotspot.agent`
- `jdk.httpserver`
- `jdk.incubator.vector`
- `jdk.internal.ed`
- `jdk.internal.jvmstat`
- `jdk.internal.le`
- `jdk.internal.opt`
- `jdk.internal.vm.ci`
- `jdk.jartool`
- `jdk.javadoc`
- `jdk.jcmd`
- `jdk.jconsole`
- `jdk.jdeps`
- `jdk.jdi`
- `jdk.jdwp.agent`
- `jdk.jfr`
- `jdk.jlink`
- `jdk.jpackage`
- `jdk.jshell`
- `jdk.jsobject`
- `jdk.jstatd`
- `jdk.localedata`
- `jdk.management`
- `jdk.management.agent`
- `jdk.management.jfr`
- `jdk.naming.dns`
- `jdk.naming.rmi`
- `jdk.net`
- `jdk.nio.mapmode`
- `jdk.random`
- `jdk.sctp`
- `jdk.security.auth`
- `jdk.security.jgss`
- `jdk.unsupported`
- `jdk.unsupported.desktop`
- `jdk.xml.dom`
- `jdk.zipfs`

The mechanical scan is authoritative if the fork differs.

## 4. Non-module runtime programme

The following source areas are not treated as “just another library package”:

| Area | Representative source owner | Initial disposition | Mandatory consumers/gates |
| --- | --- | --- | --- |
| HotSpot runtime | `src/hotspot/share/runtime` | Route C only; adapt cautiously | safepoints, handles, threads, synchronization, deopt |
| Object model | `src/hotspot/share/oops` | retain until exact layout proposal | GC, interpreter, C1/C2, SA |
| Memory/GC | `src/hotspot/share/memory`, `gc` | retain/adapt only after owner proof | barriers, roots, remembered sets, reference processing |
| Interpreter | `src/hotspot/share/interpreter` | adapt for accepted layouts | bytecodes, templates, deopt |
| C1 | `src/hotspot/share/c1` | adapt after exact intrinsics/layout | compiled code correctness |
| C2/opto | `src/hotspot/share/opto` | adapt after exact intrinsics/layout | ideal graph, escape analysis, deopt |
| Code cache/compiler interface | `src/hotspot/share/code`, `compiler` | retain/adapt | nmethods, dependencies |
| Classfile/CDS | `src/hotspot/share/classfile`, `cds` | adapt if storage/layout changes | archives, symbols, loaders |
| Primitives/JNI/JVMTI | `src/hotspot/share/prims` | adapt | JNI, JVM TI, native methods |
| Services/JFR | `src/hotspot/share/services`, `jfr` | adapt after layout | observability/serviceability |
| Platform HotSpot | `src/hotspot/os*`, `cpu` | platform-specific | ABI/intrinsics/assembly |
| Native JDK libraries | module `lib` roots | adapt selectively | encoding, I/O, networking, crypto |
| Launchers/tools | module launcher roots | usually reuse | command compatibility |

## 5. Subsystem matrix

### 5.1 java.base — text

Scope:

- `java.lang.String`, builders/buffers and joiners;
- character/code-point utilities;
- charset implementation and String coding bridges;
- regex;
- formatter and text-adjacent utilities;
- String-related VM/native consumers.

Disposition: **replace backend incrementally**.

M3 candidates:

- immutable indexed atoms;
- segmented ranges;
- shared immutable backing;
- owner-local precomputed hash/length/code-point facts;
- explicit materialization.

Required gates:

- exact UTF-16;
- Java object identity distinctions;
- intern semantics;
- regex/Matcher parity;
- charset replacement/error semantics;
- serialization/constants;
- JNI modified UTF-8 and critical/region functions;
- JIT/intrinsics;
- CDS/serviceability.

See `string-text-replacement-spec.md`.

### 5.2 java.base — collections and arrays

Scope:

- `java.util`;
- `java.util.concurrent`;
- `java.util.concurrent.atomic`;
- `java.util.concurrent.locks`;
- array support and internal collection helpers.

Disposition: **replace backend per concrete implementation; never mass-replace by interface**.

M3 candidates:

- primitive/compact lanes;
- open addressing;
- packed indexes;
- sorted arrays/pages;
- persistent/immutable structural sharing;
- specialized loops.

Required gates:

- identity/equality/null/order;
- backed views;
- iterators/spliterators;
- serialization;
- fail-fast/weak/snapshot behavior;
- JMM/linearization/progress for concurrent owners.

See `collections-replacement-spec.md`.

### 5.3 java.base — streams/functions

Scope:

- `java.util.stream`;
- primitive streams;
- functional interfaces;
- spliterators and iterator adapters.

Disposition: **adapt**.

Candidates:

- direct traversal over primitive M3 lanes;
- fewer boxed intermediate nodes;
- pre-sized or fused internal sinks where semantics allow.

Risks:

- laziness;
- side effects;
- short-circuit;
- encounter order;
- parallel decomposition;
- close handlers.

### 5.4 java.base — I/O/NIO/files

Scope:

- `java.io`;
- `java.nio`, buffers, channels, selectors;
- `java.nio.file`;
- zip/native I/O helpers.

Disposition: **reuse/adapt**.

Candidates:

- indexed path segments;
- pooled immutable path atoms;
- direct segmented buffers;
- fewer staging copies.

Risks:

- aliasing;
- native memory lifetime;
- mapped-file mutation;
- partial reads/writes;
- close semantics;
- filesystem-provider behavior.

### 5.5 java.base — networking

Scope:

- sockets;
- InetAddress;
- URI/URL-adjacent core owners;
- native network libraries.

Disposition: **retain/adapt narrowly**.

Candidates:

- immutable parsed-address facts;
- compact lookup tables;
- reduced temporary text conversion.

Security, DNS caching and platform behavior remain authoritative.

### 5.6 java.base — reflection, invoke, class loading, modules

Scope:

- `java.lang.reflect`;
- `java.lang.invoke`;
- `java.lang.module`;
- class loader internals;
- constant descriptors.

Disposition: **retain/adapt only with exact linkage proof**.

Candidates:

- indexed metadata projections;
- immutable symbol/name atoms;
- compact dependency graphs.

Risks:

- hidden classes;
- weak/soft lifetime;
- linkage identity;
- method-handle species;
- serialization/reflection compatibility;
- bootstrap cycles.

### 5.7 java.base — time, math, number/value helpers

Disposition: **retain pending measured evidence**.

Potential M3 use:

- immutable tables;
- cached exact facts;
- primitive lookup indexes.

Do not alter precision, rounding, locale, chronology or exceptional behavior for a storage saving.

### 5.8 java.compiler / jdk.compiler / javadoc / jshell

Disposition: **adapt**.

M3 owners:

- MIndexAST;
- immutable attributed atoms;
- DAGs for dependencies;
- indexed symbols/names;
- partial AST LRU as an optional residency policy.

Contracts:

- ordered children;
- source positions;
- types/attribution;
- diagnostics;
- processor/plugin APIs;
- file manager semantics;
- incremental invalidation;
- no stale symbol cross-generation reuse.

### 5.9 desktop / datatransfer / accessibility

Disposition: **retain/adapt after profiling**.

Potential wins:

- immutable lookup tables;
- compact event metadata;
- indexed text/layout caches with explicit invalidation.

UI/native peer identity and thread-affinity rules are non-negotiable.

### 5.10 XML / XML crypto / scripting / dynalink

Disposition: **adapt where representation is internal and bounded**.

Candidates:

- indexed names;
- compact AST/DOM-adjacent projections;
- immutable parser tables.

Do not collapse DOM node identity, namespace semantics, scripting object identity or dynamic linkage.

### 5.11 SQL / rowset / transaction XA

Disposition: **retain/adapt narrowly**.

Candidates:

- immutable metadata indexes;
- primitive row/column projections internal to implementations.

Public JDBC driver/provider behavior remains external authority.

### 5.12 management / attach / jcmd / jconsole / jstat / JFR

Disposition: **adapt after runtime representation stabilizes**.

Requirements:

- existing counters/events/commands remain readable;
- no accepted layout can become invisible to serviceability;
- SA must understand changed object layouts before Route C promotion.

### 5.13 security / crypto / GSS / SASL / smartcard

Disposition: **retain by default**.

Optimization proposals require security review. Constant-time behavior, provider ordering, key lifetime and zeroization outweigh generic “bloat” reduction.

### 5.14 jlink / jpackage / jartool / zipfs

Disposition: **adapt selectively**.

Potential wins:

- indexed archive paths;
- compact immutable metadata;
- fewer transient Strings.

Formats and reproducibility are compatibility surfaces.

### 5.15 vector/incubator and CPU-specific code

Disposition: **reuse/adapt as an accelerator**, never correctness authority.

M3 may expose batchable primitive lanes to vectorized kernels after scalar semantic equivalence is established.

## 6. Collection owner seed

Actual master source confirms the following current representation families and therefore requires separate replacement decisions:

| JDK owner | Current shape | Initial M3 direction |
| --- | --- | --- |
| `ArrayList` | resizable `Object[]` + size/modCount | compact object lane; primitive specialization only behind proven boundaries |
| `HashMap` | bucket array + Node/TreeNode chains/trees | compact/open-addressed or indexed entries only if all order/view/serialization contracts remain |
| `LinkedHashMap` | hash nodes plus linked encounter order | compact hash lanes + explicit order links/index lane |
| `TreeMap` | red-black tree nodes | packed tree/index lanes or paged sorted representation after comparator/view proof |
| `IdentityHashMap` | reference-identity hash table | preserve `==`; never content-intern arbitrary keys |
| `ArrayDeque` | circular object array | ring lane; keep null prohibition and iterator semantics |
| `PriorityQueue` | binary heap object array | packed heap lane |
| `ConcurrentHashMap` | concurrent bins/tree bins/counters | no replacement until exact JMM/resize/weak-iterator proof |
| `CopyOnWriteArrayList` | volatile immutable array generations | immutable generation sharing is a natural candidate, but snapshot semantics must remain exact |

Detailed designs are in `collections-replacement-spec.md`.

## 7. Existing Synexia owner seed

The source repository already contains candidate primitives that must be evaluated before new implementation:

| Existing owner | Observed contract | Potential JDK use |
| --- | --- | --- |
| `MIndexString` | immutable owner + packed value/span; canonical factory boundary; resolver/tuple storage | text Route A and design input for Route C |
| `PackedFlatArrays` | primitive growth/ring-copy atoms | collection internal primitive lanes |
| `PackedLongDeque` | power-of-two `long[]` ring | primitive internal queues/deques |
| `PrimitiveDeques` | primitive ring family | specialized internal lanes |
| `PackedLongLongHashMap` | open-addressed `long[]/long[]/byte[]` | internal primitive maps |
| `PackedLongSortedSet` | sorted primitive lane + merge path | sorted primitive indexes |
| MIndexAST family | immutable/indexed AST plus secondary-storage work in source history | compiler/tooling projections |
| MIndexPath family | typed indexed path model in source history | file/archive/tooling internals |
| MatIndex/MIndex ordering families | primitive handles and precomputed ordering in source history | immutable indexes and compiler/catalog uses |

This is an owner seed, not a statement that any of these classes can directly replace a JDK public class.

## 8. Dependency order

Default implementation order:

1. inventory/mapping tooling;
2. primitive storage and handle rules;
3. immutable text and immutable collections;
4. mutable single-threaded collections;
5. streams/iterators over accepted backends;
6. compiler/tooling projections;
7. I/O/path internals;
8. concurrent collections;
9. custom String/JDK runtime;
10. VM/GC/JIT/native integration;
11. serviceability;
12. platform-specific completion.

A later layer may run experiments earlier, but cannot be promoted ahead of its dependencies.

## 9. Inventory completion gate

A subsystem inventory is complete only when all of the following reconcile to zero unexplained rows:

- source modules/packages/types;
- generated sources;
- native entry points;
- VM consumers;
- public/protected surfaces;
- serialization forms;
- services/reflection registrations;
- build inputs;
- tests;
- mapping IDs/dispositions.

Any remaining “unknown” row keeps the subsystem at inventory-required or blocked.
