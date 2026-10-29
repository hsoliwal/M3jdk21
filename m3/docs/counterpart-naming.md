# M3 counterpart naming and runtime version contract

This extends the retained authority in `name-mapping.json`. It follows the short-pattern/acronym
rule in `atom-pattern-admission-and-tooling-boundary.md` and the source/target lineage rules in
`migration-mapping-lifecycle.md`. It is not a replacement registry.


## Every Java API can have an M3 companion (M3-API-COMPANION-1)

Every Java API type in M3JDK21 is eligible for a canonical M3 companion class.
This includes classes, interfaces, records, enums and annotation types in the
SDK's public API surface. Methods and overloads belong to the companion of
their owning type or an explicitly documented API family. Eligibility spans
the SDK; implementation advances through the established receiving order.

A companion is an explicit association with a Java API and a concrete capability.
It may be an internal implementation helper, operations class, or separately
admitted public extension. Preserve each existing JDK owner, interface, method
descriptor and behavioral contract. Ordinary Java callers continue to use
`String`, `ArrayList`, `HashMap`, `Pattern`, `Files` and other original APIs;
qualified improvements to their existing operations are integrated inside those
owners. A public M3 companion supplies additional, explicitly specified
capabilities and shares the established implementation where contracts permit.

### Names, visibility and module ownership

Use the existing `name-mapping.json` authority and retain stable
`migration.records[].id` values. Resolve the original module, package and binary
type name before selecting a companion. Prefer the established `M3<Type>`
spelling when available and appropriate; existing mapped names take precedence.
Nested types and equal simple names in different packages require explicit
mapping. For example, `java.util.Date` and `java.sql.Date` must retain separate
identities and module ownership. A name proposal does not instantiate a class.

| Java API | Companion relationship | Current status or admission rule |
| --- | --- | --- |
| `java.lang.String` | Existing `java.lang.M3String` and its registered internal owners | Package-private implementation; preserve current visibility |
| `java.lang.Class` | Existing `jdk.internal.mindex.M3Class` | Internal indexed counterpart; preserve module encapsulation |
| `java.util.Arrays` | An `M3Arrays` companion may group additional array/range/mask operations | Illustrative candidate name; resolve existing owners and collisions first |
| `java.util.List` | An `M3List` companion may describe additional operations or an explicit capability contract | Illustrative candidate; association does not make every existing List an M3 subtype |
| `java.nio.file.Files` | An `M3Files` companion may group additional file operations | Illustrative candidate; preserve provider and resource-lifetime contracts |

These examples are not a complete inventory or new naming reservations.
Keep companion relationships and qualification evidence in the existing map,
using its admitted schema and capability records. Do not create another registry.

Place a companion in its registered owning module and package, or an explicitly
admitted M3JDK21 extension module when its dependency closure requires that owner.
Keep `java.base` independent of compiler, desktop and optional higher-level modules.
A public extension needs an explicit accessible package/export decision; a new
subpackage does not inherit its parent's export. Preserve existing internal
visibility and API-boundary tests. The package-private `java.lang.M3String` is
not made public by this convention. Qualified public additions require their
own contract and collision review, including implicit `java.lang` imports and
source recompilation as well as binary linkage.

### Shared implementation and contract boundaries

Choose the companion's form from the capability. Static operations can accept
an ordinary Java receiver; supported capability interfaces can expose additional
operations; explicit stateful objects need documented ownership and lifecycle.
Final and sealed types retain their existing inheritance rules. The companion
relationship alone does not establish subclassing or substitutability.

Reuse canonical storage, indexes, offsets, masks, generation tracking, locking
and Java/JNI owners. Preserve null and exception behavior, equality/hash codes,
encounter order, backed-view mutation, concurrency, serialization, reflection,
Unicode and resource lifetimes where applicable. The association itself adds no
mandatory per-object wrapper, copied payload, cache or native allocation.
Runtime consumers acquire no third-party or Synexia donor dependency.

Keep extended domains explicit. For example, `Collection.size()` remains
`int` with its specified saturation behavior. A distinct exact-long cardinality
operation is admissible only when the storage owner can supply that information;
casting `size()` to `long` cannot recover a saturated count. A genuinely
long-indexed API also needs a complete indexing, iteration, overflow and
materialization contract. A companion's presence is not a performance result.

### Recipe-driven admission

For each proposed relationship, record the original API identity, existing M3
owner, intended capability, target module/package/visibility, source and license
lineage, recipe identity and exact qualification disposition in the existing
mapping and receipt authorities. Reuse an existing helper before materializing
an additional class. Materialize a class when its capability and contract are
defined; record unresolved associations as pending.

Synexia owns the Maven/OpenRewrite recipes, fixtures and their execution.
M3JDK21 receives executed postimages, product tests and passive receipts.
Preserve serial per-file replay, source-drift refusal and fixed point; qualify
actual API/ABI/behavior, installed modules, JNI/GC and resource costs whenever
the change affects them. Keep String -> arrays -> collections -> AST/compiler
-> remaining families as the receiving order. This documentation change
establishes companion eligibility; it adds no runtime type, export or completed
all-API coverage claim.

References: Java 21 [class and method rules](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html),
[package and module rules](https://docs.oracle.com/javase/specs/jls/se21/html/jls-7.html),
and [Collection.size contract](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Collection.html#size()).

## Names and placement

Use an established JDK or M3 owner first. Define an acronym once beside its contract, then use the
short name consistently in source, recipes, tests and documentation. Preserve conventional API
names, acronyms such as AST/DAG/JNI, and native symbols that have an existing ABI. Avoid adding
`Runtime`, `Framework`, `Manager`, `Implementation` or repeated `Index` words without a distinct role.
Do not shorten arbitrary words into unexplained initials.

| Source capability | Target spelling | Meaning / placement | This increment |
| --- | --- | --- | --- |
| `MIndexVersionTable` | `jdk.internal.mindex.M3VI` | VI = Version Index; java.base | Inlined, recipe-generated internal kernel; local module tests passed |
| `MIndexLibraryRelease` | `jdk.internal.mindex.M3Release` | Release provenance; java.base | Inlined, recipe-generated internal value; local module tests passed |
| `MIndexVmSession` | `jdk.internal.mindex.M3CB` | CB = Class Bridge | Inlined caller-owned bridge; explicit lifecycle invalidation |
| `MIndexClassIndex` | `jdk.internal.mindex.M3CI` | CI = Class Index | Inlined reflection snapshot, primitive lanes and weak defining-loader boundary |
| `MIndexClass` | `jdk.internal.mindex.M3Class` | Indexed class view | Inlined owner/row view; public type remains `java.lang.Class` |
| `MIndexVmPrecomputeKey` | `jdk.internal.mindex.M3PC` | PC = Precompute Context | Inlined VM-local immutable context; ordered dependency equality |
| `com.synexia.mindex.ast.MIndexAST` | `jdk.internal.mindex.M3AST` | AST = Abstract Syntax Tree | Reserved mapping; root-package facade must be reconciled separately |
| `MIndexASTPrecompute` | `jdk.internal.mindex.M3ASTPC` | AST precompute context | Reserved mapping; dependency/format review pending |
| `MIndexCompilerASTPrecompute` | `com.sun.tools.javac.m3.M3ASTPC` | Compiler-specific context, jdk.compiler | Reserved mapping; must not pull compiler dependencies into java.base |
| `MIndexRegexTrigramQuery` | `jdk.internal.mindex.M3TQ` | TQ = Trigram Query; java.base | Inlined candidate kernel and canonical-backing range bridge; full-image acceptance pending |
| `SegmentedAddress28` | `jdk.internal.mindex.M3Address28` | 28-bit primitive collection lane/address owner; java.base | Complete source adaptation; full-image admission pending |
| `SegmentedIntLane28` | `jdk.internal.mindex.M3IntLane28` | 28-bit primitive collection lane/address owner; java.base | Complete source adaptation; full-image admission pending |
| `SegmentedLongLane28` | `jdk.internal.mindex.M3LongLane28` | 28-bit primitive collection lane/address owner; java.base | Complete source adaptation; full-image admission pending |
| `SegmentedBitLane28` | `jdk.internal.mindex.M3BitLane28` | 28-bit primitive collection lane/address owner; java.base | Complete source adaptation; full-image admission pending |
| `PackedBitAtoms` | `jdk.internal.mindex.M3Bits` | Shared bit-address arithmetic; java.base | Complete source adaptation; full-image admission pending |
| `com.synexia.indexstring.MIndexString` | `java.lang.M3String` | Internal canonical String value; Synexia is donor/reference, M3JDK owns target naming | Replace the provisional `java.lang.MIndexString` naming/representation; preserve donor owner+coordinate semantics |
| `IndexStringTuple` | `java.lang.M3StringTuple` | Canonical persistent composition owner | Ported as M3 child-coordinate DAG; no text arrays |
| native MIndex String scalar resolver/interner | `java.lang.M3StringAtom` / `M3StringPool` | Canonical native/mapped scalar ownership | Adapted to JDK bootstrap/runtime boundary |
| `MIndexStringCanonicalFacts` / String precompute | `java.lang.M3StringFacts` + internal M3 kernels | Internal precompute only | Fixed-size canonical facts implemented; richer donor facts remain mapped work |
| `MIndexStringBacking` | `jdk.internal.mindex.M3StringBacking` | Internal backing contract | Renamed target counterpart |
| `MIndexMappedStringBacking` | `jdk.internal.mindex.M3MappedStringBacking` | Internal mapped backing | Renamed target counterpart |



### String/precompute naming invariant

Synexia names are provenance and donor names, not M3JDK ABI names. The String donor relation is `MIndexString -> M3String`. The target must preserve the donor's canonical owner/coordinate semantics while using the M3JDK name.

All precompute remains implementation-internal in both worlds. M3JDK may use compact internal M3 names for fact stores and kernels, but must not expose donor `*Precompute*` classes as public JDK contracts merely to mirror Synexia. Precompute attaches to exact M3String scalar/range/composition identity and never becomes canonical text storage. JNI byte/char/UTF shadows are materialization products only. The machine-readable rules are in `m3-runtime-invariants.tsv`; the inspected donor history is recorded in `m3string-synexia-lineage.md`.

Reserved names are candidates, not new exported APIs or claims that code exists. The mapping
records bind concrete source paths/blobs and dispositions. Package/module collision checks and
source/binary/native compatibility are required before each reserved name becomes an implementation.
The Java public class remains `String`, `Class`, `ClassLoader`, etc. Existing Synexia spellings remain
valid in their source repository; ports are traced adaptations, not destructive moves.

## Effective version (EV)

EV means the effective counterpart implementation and its admitted revision for an operation.
It is distinct from the library's raw release label, version-ordering scheme, Java class-file major
version, JVM feature version, snapshot generation and class-redefinition epoch.

A future target operation may use a counterpart only when its exact contract, dependency closure,
module/loader context and supported execution mode have passed admission. The effective revision
then derives from that counterpart's immutable implementation/context stamp. A mapping row or
the numerically greatest library version cannot enable a backend.

The runtime association must bind the actual defining `Class` and VM-local loader identity to the
immutable counterpart view, owner namespace/generation and epoch. Same binary names in different
loaders remain different classes. Delegation resolves to the defining owner. Hidden classes,
bootstrap classes, arrays and primitive mirrors need explicit contracts. A global catalogue may
share immutable content identities; it must not retain live Class/ClassLoader/JNI references or
equate classes between JVMs. A supplied release digest is a declaration, not authenticated final
post-transform class bytes.

The precompute context must include implementation/analysis revision, exact definition inputs,
ordered dependency closure, format/ABI, module access, VM/JDK/compiler flags and native target
when relevant. Changed dependencies, retransformation/redefinition, unloading, access changes or
owner generations invalidate affected reuse before publication. Hash hits discover candidates;
they do not bypass verification, linking, class initialization, JNI lifetime or JIT deoptimization.

Integration uses explicit OFF / OBSERVE / ENABLED modes, with the original JDK operation as the
fallback. Version-index queries and caller-owned Class bridge operations are active.
`M3CB.effectiveVersion(Class<?>)` resolves a declared release through that exact Class's
counterpart row; `effective` also returns the current context and epoch. Neither API
authenticates post-transform class bytes. Lifecycle owners must invalidate before definition
or access-context changes. Automatic Class/AST dispatch,
loader attachment, native code installation and a universal EV API are **not implemented**.
There is no new VM-wide class registry. Existing `Class.getName`, class identity, module resolution,
`java.version` and class-file formats are unchanged.

## Every MIndex family remains in scope

`m3/migration/vi-census.tsv` is a refreshed review queue of 2,494 matching production Java filenames
from eight exact, untruncated Git subtrees. Its receipt states the source commit, subtree roots,
selection rule and exclusions. It includes all matching paths in those subtrees, including names
that occur in multiple packages. It does not cover every repository module, nested declaration,
non-prefix dependency, generated input, native symbol or contract. Every row remains REVIEW.
It supplements the earlier census and does not advance the complete-source coverage flag.

For each capability, inspect its body/history, dependencies, existing JDK owner, source license and
behavior before assigning a name. Record direct port, adaptation, consolidation, dependency reuse
or a reasoned pending/blocked disposition in the existing authority. Include all required helpers;
do not import just the prefix-matching file or create a second payload/collection owner.

Order: immutable storage and identifiers -> structural lanes/views -> class and AST precompute
-> loader/compiler consumers -> JNI/HotSpot/JIT integration. Product code and native dependencies
must be repository-owned and build with the OpenJDK toolchain. Maven/OpenRewrite remains authoring
and proof tooling. No Synexia application artifact may become a product dependency.

## License and tests

The VI/Release kernels and fourteen Class bridge owners retain Apache-2.0. Exact source originals,
license and NOTICE snapshots live in `m3/tooling/vi/donor` and `m3/tooling/cb/donor`; the java.base distribution notice is
`src/java.base/share/legal/synexia.md`. Existing OpenJDK and third-party notices remain authoritative
for their respective code. The user requested Apache-2.0 for relevant Synexia code; this does not
relicense unrelated donors or the JDK.

The VI recipe has generation/fixed-point, target-drift, parser, tamper and unrelated-input checks.
Java 21 patch-module compilation and interpreter/mixed runtime probes compare version ranges with
the independent JPMS oracle, including raw-label preservation, equal-precedence rows, cancellation,
budgets and revision drift. Library release identity is exact, not precedence equality. The source
and target have explicit String/progress-boundary adaptations, not a drop-in binary ABI claim.

The matching Linux x86_64 fastdebug JDK image build and bounded jtreg tests pass. Class
bridge tests pass in interpreter, mixed, C1-only and non-tiered C2 modes. Automatic runtime
consumers, lifecycle/access changes, agents/redefinition, expanded CDS/JVMTI/GC coverage and
other architectures remain open; see the exact evidence receipt for the tested boundary.
AST promotion needs exact source/bytecode provenance and compiler regression tests. Radical
class-file/linkage/identity changes remain a separately reviewed experimental JVM project.

The CB packet runs 4,515 assertions per stock patch-module interpreter/mixed mode and
4,525 per matching image mode (reflection exposes additional M3 String metadata). These cover
metadata parity, defining-loader
identity, hidden/bootstrap/primitive/array classes, unloading, declared releases and invalidation.
The jtreg sources are generated by the same recipe. Exact executed modes and remaining gates are
recorded in `m3/tooling/cb/evidence/receipt.json`; patch-module evidence alone is not a fork-image pass.
