# Synexia public-target Apache reuse

Policy identity: `SYNEXIA_APACHE_REUSE_V1`.

Synexia is the donor-convergence and public-code polishing workspace. Every
project target may reuse eligible Synexia assets; `hsoliwal/M3jdk21` is an
explicit target, not the sole permitted receiver. Reuse includes implementation
code AND the machinery that improves it: recipes, compositions/DAGs, templates,
generated postimages, Maven wiring, tests, fixtures, benchmarks, schemas,
indexes/images, documentation, catalogues and replay/proof receipts.

## Permission and responsibility

Original Synexia assets covered by Apache-2.0 may be used, copied, modified and
redistributed under that license without a per-use royalty or an additional
project-specific permission request. No asset kind is excluded merely because
it is a recipe rather than runtime code. This policy adds no restriction to
Apache-2.0 and does not replace the actual applicable license.

Retain applicable LICENSE, NOTICE, copyright and attribution, and mark modified
files. Pin source revision, path, content hash, applicable license/notice inputs,
recipe/template identity and target mapping in the existing handoff records.
A repository-wide default is not evidence that every imported file, data set,
image, catalogue excerpt or template has that license. Unknown or incompatible
provenance remains blocked for source copying. Public availability is not a
license grant; challenge catalogues do not authorize copying solution bodies.

## Recipes and generated outputs

Recipe source is a first-class reusable asset. Its own license does not
magically relicense the files it edits or the templates/data it incorporates.
Check recipe code, dependencies, embedded templates and generated output
separately. Existing notices in transformed files survive. Fix reusable recipes
and fixtures; do not substitute hand-written target changes for their execution.

## M3JDK21 boundary

M3JDK21 owns its product names, public JDK contracts, bootstrap behavior and
runtime. Continue using `m3/docs/name-mapping.json`; this packet creates no
second naming registry. Apache-covered independent authoring/proof tooling may
remain under `m3/`. The JDK does not acquire a Synexia runtime service dependency.

Do not relabel inherited OpenJDK files Apache-2.0. Apache-2.0 and GPLv2-only are
not generally compatible for a combined derivative. The Classpath Exception
is not blanket permission to relicense copied/modified JDK source. Direct
inlining into a JDK owner therefore needs applicable, separately documented
compatibility/rights evidence. This packet neither grants another contributor's
rights nor invents a dual license. Separate compatible tooling reuse can proceed
without pretending that runtime inlining has been cleared.

## Qualification is separate from permission

License permission is not a compiler, behavioral or performance proof. Existing
source inventory, serial recipe, exact preimage, fixed-point, API/contract, test,
JNI/native and target build/runtime gates remain in force. For JDK changes,
image/jtreg/VM gates remain target-owned. This policy packet grants no automatic
source mutation, runtime admission or repository promotion authority.

The scope table is a coverage vocabulary, not an all-files license inventory or
a claim that every reactor target is already polished. OTHER_OWNED_ASSET keeps
new asset kinds visible without overriding their provenance requirements.

## Primary references

- Apache License 2.0, especially sections 2, 4 and 5:
  https://www.apache.org/licenses/LICENSE-2.0
- Apache/GPL compatibility:
  https://www.apache.org/licenses/GPL-compatibility.html
- OpenJDK GPLv2 and Classpath Exception:
  https://openjdk.org/legal/gplv2+ce.html

The executable check validates this packet's declared contract and hashes;
it is not legal advice, a license scanner or a substitute for rights review.

## SRO: Synexia recipe ownership

Invariant: `SYNEXIA_RECIPE_OWNERSHIP_V1`. Generic reusable recipes, compositions,
templates, fixtures, proofs and build wiring converge to their existing Synexia
owners. Targets consume pinned exports or thin product-specific adapters. Return
generic downstream improvements to Synexia. Retire target copies only after
verified replacement coverage, exact hashes and consumer compatibility. This is
internal project governance, not an extra restriction on Apache-2.0 recipients.

The canonical handoff owner is
`synexia-openrewrite-recipes/crates/m3-jdk-handoff/apache-reuse/` in
`hsoliwal/com.synexia`. Read `.m3/donor-convergence/RECIPE_OWNERSHIP_INVARIANT.md`.
Fix and execute the reusable recipe; manual template installation is not apply.
Preserve target runtime ownership, notices and append-only develop history.
