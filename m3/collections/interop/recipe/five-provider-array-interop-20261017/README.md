<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and Contributors to the Synexia Project -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Five-provider M3 array/count/flag recipe

Canonical owner: `hsoliwal/com.synexia`, this sealed task under `synexia-openrewrite-recipes`.
Named recipe: `com.synexia.m3.FiveProviderArrayInterop20261017`.
Target: the independent Java21 `m3/collections` core and optional `interop` module in
`hsoliwal/M3jdk21`, frozen at `f3c615693c41bac1bf6ff49ba53e83f85425bca0`.

The task adds a shared stable-slot primitive count/flag owner, Guava Multiset/BiMap,
Eclipse MutableBag/ordered-map factories, Apache MultiSet/legacy Bag/BidiMap,
M3-owned multivalue inversion, shared-owner overloads and nine stream collectors.
It retains the new base's Guava filters, Apache predicated decorators, SDK names,
mask APIs and state-slot extensions. Apache's optional dependency advances from
4.5.0 to the inspected 4.6.0 source/JAR; the other compatibility versions stay pinned.
The exact API limits and example are the generated interop README and coverage TSV.

## Reused machinery

The Maven task compiles the existing canonical M3HashPinnedJavaSnapshotRecipe,
M3HashPinnedTextSnapshotRecipe, M3ScopeInference and M3EditScope classes directly.
Their exact source blobs are unchanged between canonical engine base
892e3f6c51d78bc0948e8a8cf736055b4c70eafa and preparation parent
12383e05d7e37cdce5fd4a64d70c310c283a80bb and final merge parent
0a5d36185342e6f59d5cd8e4321ea0ab0d9aeecd. There is no alternate Java parser,
regex-based Java rewrite or replacement snapshot engine.

The existing five-test task harness uses real OpenRewrite Java compilation units,
the SDK scanner/scheduler/results and the named YAML composition. It checks exact
outputs, input immutability, mixed pre/postimages for every target, fixed point,
input-order invariance, missing/duplicate/drift/wrong-representation refusal and
changes between scan and visit. All emitted candidates have SDK input/output
receipts. The byte-identical receiver validates actual JUnit reports, complete
input identities and candidate hashes, rejects aliases/drift, preflights every
target, and performs serial file replacement only after admission. Fixture
lineage is explicit in contract/recipe-lineage.json, including the lack of a
claimed earlier Git commit for the locally reused constructor proof harness.

## Reproduce

```sh
mvn -B -ntp -f synexia-openrewrite-recipes/recipes/m3-five-provider-array-interop-20261017/pom.xml test
python3 synexia-openrewrite-recipes/recipes/m3-five-provider-array-interop-20261017/receive.py \
  --crate synexia-openrewrite-recipes/recipes/m3-five-provider-array-interop-20261017 \
  --target /absolute/path/to/pinned/M3jdk21 --check
```

Remove only `--check` to install the qualified SDK outputs into an exclusively
owned pinned target checkout. The receiver accepts exact preimages or exact
postimages; it does not delete installed sources to manufacture absent admission.
For the attributed target proof mirror, pass Maven
`-Drecipe.owner.root=/absolute/path/to/task/sdk-owner`; the receiver detects that
frozen SDK owner copy. No Synexia runtime service or artifact dependency enters
M3's java.base-only core.

`contract/test_source_admission.py` exercises the source verification extension
against disposable copies. All historical manifests/receipts stay immutable. A
separate exact-base reconciliation admits the sources actually present on the
frozen base; the task successor admits the new count owner/test, the two test
oracle corrections and two explicit no-argument constructors. The original
ObjectStateSlots28 constructor workaround is historical only: newer baseline
methods are retained and that owner is not replaced. The two unlisted
ObjectStateSlots28 resource fixtures are retained as hash-bound historical
inputs only; neither belongs to the 42-target manifests or receiving results.

## Evidence and rights

`contract/donor-intake.json` records complete shallow clones of four official
source repositories and exact source/license hashes. Guava/Apache adaptations
retain their source notices; Eclipse-derived bag code remains EPL-1.0 or the
EDL/BSD3 distribution option and both original license texts travel with it.
The Maven artifact metadata reflects that mixed source scope. OpenJDK licenses
and names are untouched. Source-level adaptation is distinct from the still-open
repository-wide offline dependency/source-vendoring frontier.

`evidence/` retains actual recipe/runtime reports, failed baseline diagnostics,
source admission tests and the unfiltered diagnostic cost samples. Reports state
their precise source and compiler boundaries. The first cost report uses the
original isolated core and remains historical when a later core is measured.
Measured construction growth allocation is not retained heap, and no universal
speedup or whole-library compatibility is claimed.

The disclosure scope in `contract/DISCLOSURE_SCOPE.md` covers existing public M3
mechanisms and ordinary provider adaptation. No unpublished invention-specific
algorithm or patent-priority claim is introduced. This packet does not qualify
a complete installed M3JDK image, all provider APIs, concurrency/serialization,
all intrinsic allocating results, or receiving-phase promotion. STRING remains
the existing active receiving phase.
