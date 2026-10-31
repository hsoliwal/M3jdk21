# Canonical Synexia receiver

`SYNEXIA_RECIPE_OWNERSHIP_V1` governs this handoff. The canonical reusable
Apache reuse recipe implementation, shared templates, manifests, fixtures and
proof tooling live in `hsoliwal/com.synexia`, under
`synexia-openrewrite-recipes/crates/m3-jdk-handoff/apache-reuse/`.
The `m3jdk21-receiver` profile binds that same Synexia-owned recipe to this target.
New generic fixes and stronger proof fixtures belong in that existing Synexia
owner. This directory is a receiving pointer and a frozen historical proof packet.

The local POM, template resources and earlier evidence below remain frozen
historical proof tooling. Preserve their original source/license/hash lineage;
they are not the current generic authoring path. The complete previous README
is preserved verbatim after the historical boundary below. Its old execution
status describes that earlier packet only.

M3JDK21 continues to own its JDK/HotSpot/JNI runtime, accepted M3 String and
precompute, public contracts and existing naming registry. Recipe custody adds
no Synexia runtime dependency. Eligible Apache-covered reuse retains applicable
LICENSE/NOTICE, attribution and modification notices; it never relicenses
inherited OpenJDK or other third-party material.

## Current source binding and reproduction

Read [SYNEXIA_APPLICATION.json](SYNEXIA_APPLICATION.json) for the exact
`synexiaRevision`, executed recipe identities, source/target hashes and recorded
gate results. That application receipt is the execution record for this handoff.
This guide and the presence of files do not independently assert a successful
build, installed policy, completed migration or target runtime qualification.

Use the pinned Java 21/Maven toolchain and approved offline dependencies. Set
both checkout paths explicitly. The Synexia checkout must already be at the
exact source commit recorded by the receipt, with no tracked or untracked edits
in the canonical crate or engine. Ignored build outputs remain outside that source
check. These commands do not change a branch or its history. Run compile, test
and verify in order, stopping on failure.

```sh
set -eu
synexia_root=/absolute/path/to/com.synexia
target_root=/absolute/path/to/M3jdk21
expected_revision=$(python3 -c 'import json,re,sys; v=json.load(open(sys.argv[1], encoding="utf-8"))["synexiaRevision"]; assert isinstance(v,str) and re.fullmatch("[0-9a-f]{40}",v); print(v)' "$target_root/m3/tooling/migration-recipes/apache-reuse/SYNEXIA_APPLICATION.json")
test "$(git -C "$synexia_root" rev-parse HEAD)" = "$expected_revision"
test -z "$(git -C "$synexia_root" status --porcelain --untracked-files=all -- synexia-openrewrite-recipes/crates/m3-jdk-handoff/apache-reuse synexia-openrewrite-recipes/src/main/java/com/synexia/rewrite/M3HashPinnedTextSnapshotRecipe.java)"
mvn -o -B -ntp -f "$synexia_root/synexia-openrewrite-recipes/crates/m3-jdk-handoff/apache-reuse/pom.xml" -Pm3jdk21-receiver -Dm3.target.root="$target_root" compile
mvn -o -B -ntp -f "$synexia_root/synexia-openrewrite-recipes/crates/m3-jdk-handoff/apache-reuse/pom.xml" -Pm3jdk21-receiver -Dm3.target.root="$target_root" test
mvn -o -B -ntp -f "$synexia_root/synexia-openrewrite-recipes/crates/m3-jdk-handoff/apache-reuse/pom.xml" -Pm3jdk21-receiver -Dm3.target.root="$target_root" verify
```

The source-owned tests stage actual OpenRewrite Results. Only successful,
reviewed Results with exact preimages and postimages may enter the existing
application process. Check receiving-tree hashes and zero-change replay. These
Maven commands do not themselves install results into the target checkout or
retire historical tooling. Missing tools, failed gates or source drift block
application; manually copying templates is not a substitute for execution.

---

## Historical target-local packet (preserved verbatim)

# Apache reuse: code, recipes and every owned asset kind

Synexia is the convergence workspace. All task-bound public targets, including
`hsoliwal/M3jdk21`, may reuse Apache-covered Synexia assets under Apache-2.0.
Recipes, compositions and templates are first-class reusable assets, not a
private implementation detail. The three reviewed resource templates below
state the complete policy; no existing source owner or license is overwritten.

## Existing owner, not a second engine

This task composes `com.m3.rewrite.backport.M3Jdk21HashPinnedTextSnapshotRecipe` from its existing owner at
`m3/tooling/migration-recipes/src/main/java/com/m3/rewrite/backport/M3Jdk21HashPinnedTextSnapshotRecipe.java` (Git blob `a2e8c916208b563517e51b738744a05357e633aa`). The POM compiles that owner directly and
JUnit verifies the exact Git blob. It neither vendors a new engine nor adds a
Synexia runtime dependency. Existing naming/handoff registries stay authoritative.

The 15-kind scope vocabulary includes a catch-all for other owned assets. It is
not a scanned inventory of every licensed file or a claim of completed polish.
Source/default Apache licensing never overrides third-party notices, inherited
JDK licensing or a template's actual license. License permission and target
compiler/runtime admission are separate.

## Reproduce

From repository root, the dependency-free packet integrity proof is:

```sh
mkdir -p m3/tooling/migration-recipes/apache-reuse/target/probe
javac --release 21 -Xlint:all -Werror -d m3/tooling/migration-recipes/apache-reuse/target/probe \
  m3/tooling/migration-recipes/apache-reuse/src/test/java/com/synexia/handoff/ApacheReuseProof.java
java -cp m3/tooling/migration-recipes/apache-reuse/target/probe com.synexia.handoff.ApacheReuseProof \
  m3/tooling/migration-recipes/apache-reuse/src/main/resources/com/m3/rewrite/backport/jdk21-hash-pinned-text/apache-public-reuse-v1
```

Then execute the actual repository-owned OpenRewrite gate:

```sh
mvn -f m3/tooling/migration-recipes/apache-reuse/pom.xml verify
```

Five JUnit tests require exact generation, fixed point, partial replay,
source drift/duplicate refusal, unchanged unrelated owners and the 50 policy
refusal cases. Passing recipe execution stages all three actual Results under
`target/generated/`. It deliberately does not write into the repository root.
Only those generated bytes may enter the existing reviewed promotion process.
Do not manually install the templates to bypass a failing/unavailable recipe.

## Current verification boundary

The retained local evidence records strict Java 21 compilation and **50 refused
mutants** on each source/target layout. These are the same 50 cases, not 100
unique tests. The portable proof is read-only and explicitly reports that it
does not execute OpenRewrite. Maven is unavailable in this isolated environment;
the local CodexPro connection also failed. Maven/JUnit/OpenRewrite, target
materialization and full repository/JDK runtime qualification remain unexecuted.
No fabricated dependency stubs or substituted transformation engine were used.

The template set is a recipe-ready proposal, not a claim that its three target
policy paths are installed. Existing policies, runtime source, LICENSE/NOTICE,
public APIs and all previous receipts remain unchanged. No rebase, force-push,
merge or default-branch mutation is part of this packet.

## Exact Synexia donation

Source: [Synexia PR #9475](https://github.com/hsoliwal/com.synexia/pull/9475),
commit `f8e19a5ab7b9641c063152b4f2cbf7560c25db25`. `SOURCE_PROVENANCE.tsv` binds the
two shared Java proof fixtures and three policy templates to exact source paths
and SHA-256 identities. Those five assets are byte-identical Apache-2.0 copies.
Only the target POM, resource namespace and destination manifest are adapted;
the existing target snapshot engine remains the implementation owner. The local
probe passes on the receiving layout without any runtime dependency on Synexia.
