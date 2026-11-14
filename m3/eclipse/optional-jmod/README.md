<!-- SPDX-FileCopyrightText: 2026 Hitesh Soliwal and contributors -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# Optional Eclipse core JMOD — existing M3 owners, no parallel framework

This is a **real Java 21 JPMS/JMOD packaging candidate** named
`com.m3.eclipse.core`. It exposes `ServiceLoader<M3EclipseCore>` as a small
capability surface for existing Eclipse/RCP adapters. It is **optional**:
neither the JDK bootstrap nor the Eclipse public APIs acquire a required
dependency, and no new Eclipse widget, parser, AST graph, registry or executor
implementation is added.

## Actual M3 authority, not a shadow implementation

| Boundary | Existing M3 product owner | Exactly what the module does |
|---|---|---|
| M3String | `com.m3.text.M3StringPiece`, `LocalM3Arena` | Retains the given owner as-is, or explicitly copies caller UTF-16 into the existing arena |
| M3Index* | `com.m3.collections.M3LongIndexProjection`, `M3IndexMaskCompactor` | Snapshots primitive index rows; sorting/filtering/JNI remain in the original owners |
| M3AST | `com.m3.ast.M3ASTHierarchy` | Forwards source-sealed `FileFact` inputs to the existing L0–L5 hierarchy |
| M3 arrays | `com.m3.arrays.M3Arrays` | Uses existing immutable UTF-16 array snapshots and facts |
| M3File | `com.m3.file.M3File` | Narrow, source-attributed read-only NIO boundary over the original `Path`; no second file-tree cache |

**Limit:** `M3StringPiece` on an ordinary stock JDK is not proof of the
OS-backed canonical `java.lang.M3String` VM representation. Existing Eclipse
`String` APIs remain `String`; this optional API does not mechanically rename
or globally intern them. The M3File edge is adapted from Synexia
`MIndexResourceAccess`, not a claim that full Eclipse workspace mutations,
markers, deltas or permissions are implemented.

## Build the JMOD and complete named dependency closure

Run from the repository root using JDK 21 (no downloaded libraries required
for this builder):

```sh
python3 m3/eclipse/optional-jmod/build.py verify \
  --jdk "$JAVA_HOME" \
  --out /tmp/m3-eclipse-jmod-proof
```

The output contains `jmods/com.m3.eclipse.core.jmod`, the five prerequisite
M3 named modules, `JMOD_BUILD.tsv`, and a `jlink` image. The builder
compiles the **real** existing `m3/core`, `m3/arrays`, `m3/collections`,
`m3/ast`, and new `m3/file` source; it does not copy any production M3
implementation into the Eclipse module. Only descriptor **overlays** are
staged for the existing nonmodular AST/Collections modules. Existing module
descriptors for M3Text/Arrays are reused directly. Every JMOD is built
twice with the same fixed timestamp and must match byte for byte. Apache
`LICENSE` and `NOTICE` are retained as JMOD legal notices.

`ServiceLoader<M3EclipseCore>`, signed-long stable index occurrences,
UTF-16/unpaired surrogates, M3AST receipts and masks, NIO file bounds,
symlinks, concurrent filesystem changes and missing files are tested by
`M3EclipseCoreSelfTest`. Java and JNI mask selection share the existing
`M3IndexMaskCompactor` owner and ABI. The **exact-head CI workflow**
compiles its existing C JNI implementation, then **requires** that native
library to load and produce Java-equivalent results under `-Xcheck:jni`.
Neither this module nor M3File invents a JNI symbol.

The builder refuses occupied output paths and aborts on any missing real
module or native/legal prerequisite; mock module smoke tests are only
diagnostics, not qualifying runtime proof.

## Recipe-first and source identity

Canonical recipe:
`hsoliwal/com.synexia` → `com.synexia.rewrite.M3EclipseCoreOptionalJmod`.
Eight Java postimages are source-sealed ABSENT→SHA-256 and mirrored under
`recipe/src/main/resources`; Maven/OpenRewrite fixed-point and drift refusal
are mandatory. `build.py` is a packaging/verification tool, **not** an
alternative source transformation engine. No Synexia runtime JAR dependency
enters the generated JMOD. M3JDK21 remains the target API and acceptance
authority. See `qualification/SYNEXIA_RECIPE_AUTHORITY.md` and the
target naming map sidecar.

## Still not complete

JMOD presence alone does **not** convert or prove all of SWT, Nebula, JFace,
Equinox resolver/extension loading, JDT, EMF, Workbench, JNI native-widget
lifetime or Eclipse installation. The existing OSGi forwarding PRs continue
separately; Eclipse's original public interfaces and provider semantics
must remain stable. The full M3JDK21 modified JDK image, jtreg, native
platform matrix, actual Eclipse PDE/Tycho launch and performance gates
are separate and may not be marked passing by this JMOD-only test.

 
### Strict Java 21 compiler gate

The real-owner JMOD job exposed a pre-packaging `missing-explicit-ctor` warning in
the existing exported M3 text catalog's nested `Builder`, promoted to an error by
the intentional warnings-as-errors policy. The one-line explicit public constructor
is tracked by a separate canonical Synexia hash-pinned recipe (PR
https://github.com/hsoliwal/com.synexia/pull/12337) and receiver pin in
`qualification/SYNEXIA_SHARED_BUILDER_CTOR_RECIPE_PIN.tsv`. Its generated source
update and exact-head JMOD/JNI rerun remain gated on actual recipe/CI evidence.
