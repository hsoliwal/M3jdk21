<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# M3JDK21 public JDK owner and internal M3 naming contract

Status: target-receiver instructions; **not a second naming registry or implementation admission**.

**M3JDK21 is one JDK.** An alternative implementation that satisfies the entire applicable
JDK class contract becomes an **implementation of the existing JDK class**, not an exported
parallel `M3HashMap` or an adapter that callers must select manually. The public
`java.lang.String`, `java.lang.Class`, `java.util.HashMap`,
`java.util.ArrayList`, `java.util.regex.Pattern` etc. keep their names,
method descriptors, superclass/interface shape and supported compatibility contracts.
Internal M3 array/index/mask, facts, JNI and ownership helpers remain behind those APIs.
Candidate code is **never promoted merely for being faster on one workload**.

## Authoritative mapping and file placement

For every source change, start with
[`m3/docs/name-mapping.json`](name-mapping.json), specifically the durable
`migration.records[].id` plus source/target symbol, module, path, revision,
license, recipe, tests and state. Follow the existing
[`m3jdk21-porting-invariant.md`](m3jdk21-porting-invariant.md) and
[`counterpart-naming.md`](counterpart-naming.md). Name proposals add or amend
entries in that **same authority** and retain the stable mapping ID; the queue under
`m3/tooling/name-contract/name-plan.json` references that authority but is not
a second registry. Preserve earlier lineage when packages/classes move.

| Role | Actual class/package policy | File location |
| --- | --- | --- |
| Public JDK String | `java.lang.String`; use internal M3String owner/coordinates | `src/java.base/share/classes/java/lang/String.java` |
| Public JDK collections | `java.util.*` / `java.util.concurrent.*` stay public; do not export a competing M3 replacement | `src/java.base/share/classes/java/util/**`, `java/util/concurrent/**` |
| Public regex | `java.util.regex.Pattern` / `Matcher` stay public; internal M3TQ is fact/search helper | `src/java.base/share/classes/java/util/regex/**`; `jdk/internal/mindex/M3TQ.java` |
| Public Class/reflection | `java.lang.Class`, `java.lang.reflect.*`; `M3Class` is internal | `src/java.base/share/classes/java/lang/**`; `jdk/internal/mindex/**` |
| Internal string/array/index owners | Reuse already mapped `java.lang.M3String`, `jdk.internal.mindex.M3*`, qualified array/mask/lane owners | Owner module's `src/<module>/share/classes/<qualified-name>.java` |
| Compiler/AST internals | `com.sun.tools.javac.*` or mapped compiler module owner; no `jdk.compiler` dependency leaking into `java.base` | `src/jdk.compiler/share/classes/**` |
| Isolated SDK/port candidate | Retain `com.m3.*` names only while separate proving ground; do not claim JDK compatibility | Existing `m3/<family>/**` source layout |
| JNI/HotSpot implementation | Target-owned symbols/headers in owning module; no stale heap pointer; preserve ABI | `src/<module>/share/native/**`, `src/hotspot/**` where applicable |
| Recipe and evidence | Canonical reusable Maven/OpenRewrite implementation remains in Synexia; M3JDK target-only receiver, tests, jtreg and receipts here | Synexia `synexia-openrewrite-recipes/**`; target `m3/tooling/**` |

Use **JDK conventional name** when implementing a JDK type. Keep the
existing class/constructor names unchanged and retain package-visible,
protected, reflection, serialization and JNI compatibility as applicable.
Within internal helpers, choose a short meaning-bearing class name, e.g.
`M3StringFacts`, `M3TQ`, `M3IntLane28`; do not mechanically change
`MIndexFoo` to `M3Foo`, add gratuitous `Manager/Impl` suffixes or duplicate
canonical storage. Name internal methods for their real operations; never rename
a public JDK overload, break native entry points, or change checked exceptions
via text replacement. Atomization and JNI acceleration are implementation
details of the existing contract.

## Mandatory recipe for formatting, relocation and promotion

Canonical reusable recipe: **Synexia** `m3jdk21-owner-naming` crate,
`com.synexia.rewrite.M3Jdk21StagingFormat`, and existing
`M3HashPinnedJavaSnapshotRecipe`. Source PR:
[hsoliwal/com.synexia#12806](https://github.com/hsoliwal/com.synexia/pull/12806).
Pin its exact commit/revision/license and target map Git blob in the receiving
packet before execution. Do not treat the open draft as proven.

`INTERNAL_MOVE`: use attributed OpenRewrite `ChangeType` for
class/package name and consumer imports/usages; use overload-specific
`ChangeMethodName` for an approved internal method. Inventory callers,
nested/anonymous classes, resource/service names, reflection strings, serial
names, native symbols, `module-info`, generated code, tests and third-party
ABI. Unknown consumers require refusal.

`FORMAT_ONLY`: use OpenRewrite `AutoFormat` on **sealed staging Java
compilation units**, scoped to reviewed files and their module's actual JDK
style conventions. Don't run bulk reformatting across `src/**`, lose
copyright headers, normalize native code using Java rules, or hide semantic
changes inside formatting. Repeated passes must reach a byte-stable fixed
point; preserve unrelated files.

`JDK_BACKEND`: **do not** `ChangeType` from `M3Class` to `Class` or
from `M3LongLongTreeMap` to `TreeMap`. Instead use a source-sealed,
reviewed recipe to optimize the **original JDK owner class in place**.
Keep reusable M3 helpers internal; move other specialized algorithms into
the relevant existing internal/package owner and update only qualified
consumers. Preserve donors' exact source attribution and per-file license.

Promotion requires **all** of: mapping record accepted/verified for the
affected capability; exact before/postimage and candidate source hashes;
public/protected API and binary signatures; differential
behavior (null, exceptions, ordering, views, concurrency, serialization,
reflection, Unicode where applicable); matching installed JDK build,
jtreg and module tests; JNI/HotSpot/JIT/GC/CDS and cross-platform gates
when applicable; comparative cold/warm CPU, allocation, retained Java heap
and native/process memory; rights/NOTICE review; dry-run and real
OpenRewrite replay, fixed point, collision/refusal and rollback tests.
Record **not run** separately from pass. Benchmark superiority is necessary
only where a performance claim is made and never sufficient for promotion.

## Executable queue and verification

`m3/tooling/name-contract/name-plan.json` holds reviewed **queued**
ideas with existing mapping IDs and an exact canonical map blob, not active
source edits. `check.py` verifies the map pin, actual mapped Java
package/class/location, queue safety and AGENTS.md discoverability.

```sh
python3 m3/tooling/name-contract/check.py
python3 -m unittest discover -s m3/tooling/name-contract -p 'test_*.py' -v

# From a separate pinned com.synexia checkout, no hidden global edits:
mvn -B -f synexia-openrewrite-recipes/crates/m3jdk21-owner-naming/pom.xml test
python3 synexia-openrewrite-recipes/crates/m3jdk21-owner-naming/emit.py \
  --workspace /path/to/M3jdk21 \
  --plan /path/to/M3jdk21/m3/tooling/name-contract/name-plan.json
```

## Canonical-target naming census

`audit.py` derives a deterministic, read-only TSV from the same canonical
`migration.records[]` map. Each concrete target entry retains its mapping ID,
module, path, symbol, mapping state, number of mappings sharing the path, and
a conservative presence/type-declaration disposition. Pending or blocked source
targets are not treated as failures merely because a file is absent.
Native C/H paths are not parsed as Java, but missing native files under
an implemented mapping are review failures. Missing pending/blocked native
paths stay explicitly pending. Output uses `target_path` so future
mapping edits cannot mistake a receiving target for its donor source. A file present with the expected
package/type name is **not** proof of ABI, behavior, performance or installed
JDK admission, and this is not a full-source inventory.

```sh
python3 m3/tooling/name-contract/audit.py --strict
```

This fails for broken path/type pairs, missing files claimed by an implemented
mapping, duplicate capability IDs and changes to the authoritative map blob.
Multiple capability records may share an implementation file without
being reported as a collision; the shared owner must still be reconciled
before any source-changing recipe.

An executable `READY_FOR_STAGING` recipe is additionally required to
provide a hash-pinned `closed_java_scope` for the defining compilation unit
and **every affected caller**. The canonical Synexia emitter refuses missing
caller preimages, path collisions and unknown source owners. Its `--verify-stage`
pass refuses output-drift, residual old-path files and unexpected Java
compilation units in an isolated staging tree. This remains candidate verification,
not evidence of installed-JDK behavioral equivalence.

The current queue has no promoted candidates. Do not execute an OpenRewrite
product mutation from this queue until mapping updates, exact staging receipts
and independent tests are reviewed. Source-first processing stays serial per
file; different files may prepare candidates concurrently, and promoted
results must be proven on the actual JDK image.


## Executable target-image public/internal boundary

The target receives **only** the product-side jtreg check
[`test/jdk/java/lang/String/M3CanonicalApiBoundaryTest.java`](../../test/jdk/java/lang/String/M3CanonicalApiBoundaryTest.java).
The reusable OpenRewrite compiler and source-structure admission logic remain
exclusively in the Synexia convergence workspace, currently under review in
[hsoliwal/com.synexia#13180](https://github.com/hsoliwal/com.synexia/pull/13180).

The target test checks package-private `java.lang.M3String` and the
non-exported/non-opened `jdk.internal.mindex` implementation package,
including qualified exports and runtime export overrides. It inspects
public/protected reflection signatures for 19 canonical JDK owners spanning
String, Class, I/O streams, core collections and concurrent collections,
regex and NIO file APIs. It also enumerates compiled classes through
`java.base`'s `ModuleReader` and checks every top-level M3/MIndex/MatIndex
type inside a module-exported or opened package without class initialization.
Package visibility is checked against a single indexed set of exposed
packages, not a repeated scan per class. No production class, serialized
format, JNI symbol or JDK-facing method signature is modified by this receiver.

Compile and run **against the actual M3JDK21 build image**, after
configuration and jtreg acquisition:

```sh
make run-test TEST=test/jdk/java/lang/String/M3CanonicalApiBoundaryTest.java
```

A local patched stock-JDK21 fixture is an additional syntactic and refusal
proof only; it is **not** runtime qualification of the fork. Record jtreg
results separately from lint, Maven recipe replay, ABI, differential behavior,
JNI/GC/JMM and CPU/heap/native measurements.

Selected source audit (M3JDK21 master, 2026-10-23): 22 direct `M3*.java`
classes under `java.lang` and four under `java.util` were observed
package-private; `jdk.internal.mindex.M3Class` has `public` JVM
accessibility within an unexported package. Separate `com.m3.collections`
and `com.m3.file` modules still export legacy `M3ArrayList` and `M3File`.
Those public standalone SDK symbols are unresolved migration debt, not proof
that they are canonical JDK replacements. Resolve their external clients and
serialization contracts before any visibility-changing OpenRewrite recipe.


## Exported legacy M3 types: pending migration IDs

The existing canonical map records two observed exported standalone SDK
classes as **pending**, with no JDK implementation equivalence or ABI claim:

| Stable mapping ID | Existing public class | Current disposition |
| --- | --- | --- |
| `m3jdk21.legacy-export.M3ArrayList` | `com.m3.collections.M3ArrayList` | Exported `com.m3.collections` legacy API. Must close List/storage/snapshot/serialization callers before transferring its qualified backend into the original `java.util.ArrayList`. |
| `m3jdk21.legacy-export.M3File` | `com.m3.file.M3File` | Exported `com.m3.file` legacy API. Its metadata, channel and shadow-stream contracts are not an implementation of `java.nio.file.Path`; separate API classification and caller migration required. |

These are traceability entries only: `kind=pending`,
`status=pending`, `sync.direction=traceability-only`. They pin the
observed Git source blobs, do not enable a recipe or update a class,
and do not silently remove any exported packages. The previous 61 mapping
records, the other map content and all four queued plan operations remain
unchanged. No new operation becomes `READY_FOR_STAGING`.

Provenance of this bounded mapping change:

```text
m3/docs/name-mapping.json
  old Git blob SHA-1: fe77055e75116df42fafb40c433d351fbac139cc
  new Git blob SHA-1: 28df2b56aec04f00ca82c15d6b3e8c10e0e87aa8
  old SHA-256: 2beaf836d56b50e71f94e1be94d7923198baed7b7355dc4997342ca5257e8116
  new SHA-256: 3cd37056abe11669f0d1b8c49c07eae4b72a1286583d2a063bb021e71c32fc72

m3/tooling/name-contract/name-plan.json
  old Git blob SHA-1: 98d543dbd0c4f8fabbe23c222da837b3e5f558ca
  new Git blob SHA-1: 67b88d919bee9d22873b6540a570abbf4b689505
  old SHA-256: e28fc37d2a91ea4bd022c393427e32b04dc19a9751a4905f3388632116ef1b99
  new SHA-256: 73dcc7a4cb044ba810970d71a32a460cd6c02dff34afe99d3b24dc197f324bfc
```

The full source-tree, exported ABI and native/serialized consumer closure
remains explicitly **unverified**; none of these hashes prove semantic
equivalence or performance. Source-backed migration must use Synexia-owned
OpenRewrite recipes with deterministic fixed-point, compiler, runtime and
refusal evidence before the canonical JDK class receives any implementation.


## Historical evicting-queue map vocabulary correction

The existing `m3.collections.evicting-queue.owner-naming` mapping (not a new
migration record) carries four deliberate historical state literals:
`kind=internal-relocation-proposal`, `status=proposed`,
`sync.direction=same-repository internal name review`, and
`source_state=implemented-proving-module-unpromoted`.
The earlier schema enums did not admit these existing values, making the
canonical map fail JSON Schema checks **before** the legacy-export additions.

The schema correction preserves that record and all other map data unchanged.
It admits the four historical literals **only** for that exact stable ID
through JSON Schema 2020-12 conditional `if/then/else`; every other
migration record remains restricted to the original enum vocabularies.
This is a mapping-contract repair, not evidence that the queue was
source-sealed, renamed, installed or performance-qualified.
