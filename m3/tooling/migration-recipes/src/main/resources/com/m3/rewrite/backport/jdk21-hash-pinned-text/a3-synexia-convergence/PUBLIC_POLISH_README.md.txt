# Synexia public-code polish workspace

Status: convergence-workspace contract. Candidate generation only; no automatic promotion.

## Authority

Synexia is the **CONVERGENCE_WORKSPACE**. Donors, challenge catalogues, historical branches and
product feedback flow into Synexia for inventory, atomization, patternization, recipe qualification,
implementation and verification. M3JDK21 and other repositories are delivery targets for a polished,
verified export; they do not redefine Synexia architecture silently.

This workspace binds the public-code polish lane to the live root Maven reactor at:

- develop revision: `d1ecbd43dbadaf218deec0d98a2ddc23f3a3f45c`
- root `pom.xml` Git blob: `a799bfd15bc1170fb73a3c931980c34347bbdba3`
- declared reactor projects: **202**

## Target census

`TARGETS.tsv` is the canonical ordered target list for this packet.

| Kind | Count | Action |
| --- | ---: | --- |
| OWNED_CODE | 192 | `com.synexia.PublicPolish` dry-run candidate |
| OWNED_MIXED_NATIVE | 6 | same Java dry-run plus mandatory JNI/native gate |
| BUILD_METADATA | 3 | verification only; no Java polish mutation |
| VENDOR_READ_ONLY | 1 | immutable vendored source; never polished in place |

The six mixed/native projects are:

- `synexia-dawn-jni`
- `cognix-eclipse-mechanics-native-bridge`
- `synexia-indexstring-tornadovm`
- `synexia-neural-renderer-jni`
- `synexia-classic-tools-jni`
- `synexia-cpu-camel-fabric/providers/tornadovm`

The vendored `third_party/re2j` module remains byte/provenance owned by its upstream source and is
**READ_ONLY** under the repository's third-party invariant.

## PublicPolish composition

The short public entry point is `com.synexia.PublicPolish`.

It composes existing owners only:

1. `M3EveryModuleAtomPatternInventoryRecipe` — read-only atom/pattern/donor inventory;
2. `M3ApiEvidenceReviewRecipe` — read-only API/documentation/native evidence inventory;
3. `com.synexia.RepositorySetMechanicalCleanup` — existing mechanical import/order/format owner.

No second atomizer, patternizer, donor selector or cleanup engine is introduced.

Public API/Javadoc quality is inventoried before cleanup and remains a verification surface in this lane. The recipe does **not** invent
Javadoc text, rename public contracts, alter signatures, change exception/null semantics, or perform
semantic modernization merely to make code look newer.

## Execution

Build and test the recipe pack first, then install it into the approved local Maven repository:

```bash
./mvnw -B -ntp -pl synexia-openrewrite-recipes -am test
./mvnw -B -ntp -pl synexia-openrewrite-recipes -am -DskipTests install
```

List targets:

```bash
synexia-code-convergence/public-polish/run.sh --list
```

Dry-run one owned module:

```bash
synexia-code-convergence/public-polish/run.sh --module synexia-indexstring
```

Serially dry-run every owned source target:

```bash
synexia-code-convergence/public-polish/run.sh --all
```

The driver invokes only OpenRewrite `dryRunNoFork`; it never invokes `run`/`runNoFork` and never
promotes patches.

## Promotion gates

For each module, promotion remains serial and requires:

```text
inventory / exact target
→ PublicPolish dry-run candidate
→ reviewed diff
→ formatting + static analysis
→ Java 21 compile
→ unit/contract tests
→ public API + Javadoc/doclint review
→ JNI/native parity when native_gate=true
→ affected reactor verification
→ fixed point
→ additive promotion
```

A finite clean dry-run is not repository completion. A module with no Java source is still retained
as a project target and closes through its build/package-specific verification path.
