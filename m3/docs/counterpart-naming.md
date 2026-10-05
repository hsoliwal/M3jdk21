# M3 counterpart naming and runtime version contract

This extends the retained authority in `name-mapping.json`. It follows the short-pattern/acronym
rule in `atom-pattern-admission-and-tooling-boundary.md` and the source/target lineage rules in
`migration-mapping-lifecycle.md`. It is not a replacement registry.

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
| Existing JDK String counterpart | existing `java.lang.MIndexString` and backing owners | Retain current implementation lineage | No blanket prefix rename |

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

## Whole-estate scope extension

[Whole-Synexia sanitization](whole-synexia-sanitization.md) extends the scope beyond
MIndex filenames to every source obligation, and includes Apache, Guava, Eclipse
and JDK capability baselines. It preserves this naming and mapping authority and
requires explicit runtime/compiler/tooling/distribution/experimental-JVM placement.
The prefix census remains historical partial evidence, not the estate denominator.
