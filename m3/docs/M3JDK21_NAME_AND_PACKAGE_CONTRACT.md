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


## Installed-image API and internality jtreg guard

`test/jdk/java/lang/String/M3CanonicalApiBoundaryTest.java` is an **owned
JDK target-side test**, not a Synexia source transformer or an additional
public `M3*` API. It checks 19 existing JDK public owners; protects
package-private `java.lang.M3String`; and verifies that the public JVM
visibility of `jdk.internal.mindex.M3Class` is not mistaken for an
exported/opened `java.base` API.

The test reads the **actual installed module descriptors** of `java.base`
and `jdk.compiler`, refusing exported or opened internal M3 implementation
packages (including qualified exports and modules opened with runtime flags).
It also inspects the installed `java.base` class resource index and
public/protected JDK generic signatures, rejecting M3/MIndex/MatIndex types
or renamed implementation types from `jdk.internal.m3`,
`jdk.internal.mindex`, `com.sun.tools.javac.m3` or `com.m3.*`.
No M3 implementation class is initialized simply to inspect its identity.

A portable GitHub Actions mechanism gate compiles this test using Java 21
and runs one synthetic patched-module **positive** and two deliberately
invalid **negative** fixtures. This does not qualify the fork image. The
installed M3JDK21 JDK acceptance command is:

```sh
make run-test TEST=test/jdk/java/lang/String/M3CanonicalApiBoundaryTest.java
```

The data-only receipt is under
`m3/docs/qualification/current-api-internality-20261027/RECEIVER_RECEIPT.json`.
Until the source-built M3JDK21 image runs jtreg, mark that gate **NOT RUN**,
irrespective of success on a stock JDK patched with stubs. Full
serialization/JNI/GC/JMM and performance gates remain independent.
