# M3JDK21 atomization, patternization and tooling boundary

Status: canonical implementation invariant for M3JDK21 admission.

## Product plane

M3JDK21 is a JDK. Product source under the OpenJDK tree and any native implementation used by the
built JDK must remain buildable by the OpenJDK `configure` / `make` toolchain and JDK-native
dependencies.

M3JDK21 product code must not require Maven, OpenRewrite, Synexia application modules, or another
downstream artifact at JDK build time or runtime.

A capability selected for M3JDK21 must ultimately be implemented in the JDK source/native tree or a
JDK-owned module with a valid bootstrap/module dependency position. Tooling may generate or verify
that implementation; tooling is not the implementation dependency.

## Tool plane

The following are authoring, transformation and proof tools:

- Maven and Maven plugins;
- OpenRewrite and recipe modules;
- JUnit and JaCoCo;
- MIndexAST inventory/partial-AST analysis when used from the external tooling layer;
- donor inventories, migration catalogues and provenance tooling.

They may inspect the JDK, produce candidate diffs, replay transformations and verify them. They must
not leak into the JDK product dependency graph merely because they were used to author a patch.

## Atomization invariant

A file may be atomized repeatedly as long as the declared externally observable contract and
behavior remain unchanged.

Atomization means identifying/coalescing/splitting semantic behavioral leaves so each leaf has a
stable purpose, input/output/effect envelope and proof target. It does not mean mechanically
minimizing line count or creating meaningless helper methods.

A contract-preserving file-local atomization is mechanically independent of other files and is
eligible for deterministic parallel application.

## Patternization invariant

A file and its atoms may be patternized repeatedly as long as the declared externally observable
contract and behavior remain unchanged.

Patternization means assigning existing or newly recognized semantic leaves to an admitted
pattern/IOP role and validating their participant relationships. Pattern metadata used only by the
tool plane must not create a downstream dependency in product code.

Atomization and patternization are orthogonal and both are required admission dimensions.

## Scope promotion law

Every refactoring recipe must operate at the narrowest sufficient authority:

1. FILE — private/file-local implementation only;
2. VISIBILITY — a declaration's accessibility changes, without assuming package-wide authority;
3. PACKAGE — coordinated package/default/protected relationships across files in one package;
4. MODULE — one JDK/module boundary;
5. MULTI_MODULE — reactor/multiple modules;
6. LIBRARY_API — exported/public/API or cross-library contract.

Do not promote scope because it is convenient. Promote only when proof shows the transformation
crosses the current boundary.

FILE-local behavior-and-contract-preserving work may fan out independently across all files.
Canonical promotion remains serial/evidence-gated.

## Recipe-first law

A recurring mechanical transformation must be expressed as a reusable OpenRewrite recipe (or a
smaller deterministic transformer when OpenRewrite is not the truthful parser for that artifact)
before repository-wide hand editing.

OpenRewrite recipes are tools. They may refactor the M3 OpenRewrite tooling itself.

## Recipe verification and retention law

A recipe is CANDIDATE until its exact implementation has executed tests successfully.

A recipe may enter the retained/verified catalogue only when its exact commit has evidence for:

- positive before -> after transformation;
- fixed point / idempotent second application;
- changed-preimage/drift refusal;
- out-of-scope refusal;
- contract/surface preservation appropriate to its scope;
- representative corpus coverage;
- compilation of transformed Java where applicable.

For general file-local recipes, the default representative corpus is at least 100 heterogeneous
Java files before repository-wide promotion.

The saved unit is:

`recipe + recipe tests + declared scope + contract mode + exact commit + verification receipt`.

Never relabel an unexecuted or failing recipe as verified.

## Partial AST law

Full AST materialization is not the default requirement for mechanical file-local work.

The tool plane should request the smallest structural projection sufficient for the current atom.
MIndexAST partial/deferred/lazy, memory-budgeted analysis may be used by tooling to discover and
verify candidate leaves. Expansion proceeds only when the current partial context cannot establish
the required contract or pattern relationship.

This does not make MIndexAST a downstream dependency of the JDK product. Any MIndexAST capability
selected for the final JDK must be ported/implemented into the appropriate JDK-owned layer and
validated there.

## Verification order

For an applied product change, evidence progresses in M3 order:

`diff -> lint/static analysis -> compile/build -> tests -> runtime/benchmark where required`.

A passing recipe unit test proves the recipe transformation mechanics. It does not by itself prove
the resulting JDK subsystem complete.
