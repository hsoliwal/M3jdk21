# Deep filtered List composition: convergence candidate

This additive change refines the existing M3FilteredLists, M3CollectionViews,
and M3FilteredMaps owners on M3JDK21 PR #1031 at
10ce8abaa279292c73e1c118628344cf0851d14a. The private repositories are
convergence workspaces where concepts, implementations, contracts, and
evidence are refined before promotion into JDK or SWT integration. This
candidate does not advance those promotion gates.

## Concrete implementation and lineage

The original wrappers flatten their backing owner but retain a linearly
recursive predicate chain. Repeating the explicit List filter 65,536 times
over one element reproduces StackOverflowError with -Xss256k.

The Collection owner receives the exact immutable binary-carry predicate
atom from M3JDK21 PR #1018 at
0997991e2496ee07b69be5d245ebc0ed1bd9b1f3. Its complete new source is
byte-identical to that donor because its #1031 preimage is identical to
the donor's preimage. The Map owner changes only its composition expression;
#1031's storedRepresentativeKey and all other new Map behavior remain
present. The explicit List owner changes only its composition expression
to call the same package-private atom.

The named recipe is
com.synexia.m3jdk.DeepFilteredListComposition20261017. It composes the
existing M3HashPinnedJavaSnapshotRecipe and M3HashPinnedTextSnapshotRecipe
for the same m3-deep-filter-list-20261017 crate. The root agent publishes
canonical recipe resources in Synexia first, then mirrors and runs them
for the target. The helper is a first-party target refinement returned to
that canonical recipe, not a previously landed Synexia runtime owner.

## Preserved contracts

Predicates remain opaque Java callbacks. Occurrences are preserved,
including duplicate references to the same predicate object. Evaluation
order, short circuiting, thrown object identity, and retained prefix
selection do not change. A compiled Pattern.asMatchPredicate can be one
such callback. The wrapper does not inspect its regex, cache its result,
or run it on a GPU.

Lists retain their original backing owner, selected logical indexes,
encounter order, stored object identities, hidden elements, iterator
mutation rules, physical backed sublists, and JDK21 reversed-view behavior.
Sorted Set/Map wrappers preserve comparator identity, live range bounds,
and stored-key representative semantics. Detached sorted snapshots
preserve duplicate occurrences and stable comparator ties.

The stack-depth bound applies to conjunction nodes created by these
factories when appending directly to their recognized nested owner.
It does not bound recursion inside a caller predicate or arbitrary
chains of other adapters interleaved between filters. The existing
restrictions on stateful or reentrant List predicates and concurrent
backing mutation remain.

## Cost and scope

For one retained plan with d predicate occurrences, the tree has O(d)
metadata, O(log d) worst-case append depth and evaluation call depth,
and O(d) worst-case evaluation work. Testing does not allocate a traversal
buffer. Construction creates more metadata than a single recursive lambda
per append. Retaining many historical views retains their persistent
paths. No general throughput, allocation, native, or GPU improvement is
claimed. The demonstrated improvement is a bounded wrapper-created stack.

No new public API, alternative filter hierarchy, storage engine, JNI
symbol, comparator execution mechanism, or JDK integration point is
introduced. PRs #1028/#1035's alternative filtered-view family and
#1020's competing List overload remain distinct candidates.

## Payload and source checks

The recipe resources contain five sorted Java manifest targets and four
sorted text manifest targets. SOURCE_SEALS.json records exact preimage
and postimage SHA-256s and Git blob IDs.

The target mirror is
m3/collections/wrappers/deep-filter-list-20261017/recipe, outside the
original pinned wrappers/recipe tree. The two existing dedicated List/Map
verifier scripts are source-sealed text successors. Their historical
templates, manifests, preimages, inventory checks, Java tests, JNI checks,
and GC commands remain intact.

The successor validator accepts exactly two qualified edges:

| Existing checker | Historical source SHA-256 | Required current source SHA-256 |
| --- | --- | --- |
| Explicit filtered List receiver | 7a9d818d38beae4df178317f7913050b47d6442e85f1ef5d5873a0df89262101 | c6db65bc2408ea44ef4395220f862b3cad9d824f1ee09558101381cb535c4b48 |
| Writable Map receiver | 538f62a64fa5cbe1b0dd37251a7652121306d17365a7c5c111cb0d5ee7c52200 | b6733c9cd447826710feaf2480febdaa9091f6367fd7ff3195f339cc6b08efcf |

It hashes the exact historical and current manifests, verifies that each
historical postimage is the current preimage, and requires all five
current Java outputs and their payloads. Both checker successors hash
the exact validator before invoking it. The validator refuses the old
installed source as an alternative current state. The List checker also
compiles M3CollectionViews, now its actual dependency.

## Evidence locations

Author preflight is explicitly separate from actual OpenRewrite Results.
The author preflight found 55,469 new List checks at depth 65,536 and
192 seeds, plus 1,312,160 unchanged donor checks. Counts include
structural assertions, not independent JUnit test methods.
The author Collection source initially differed from the final exact
donor by import order alone. Actual SDK runtime validation uses the
final sealed payloads, not those earlier author copies.

The root's real Maven/OpenRewrite qualification passed all five tests
with Java LST and text lanes. The root's SDK Results live under
synexia-task/synexia-openrewrite-recipes/recipe-crates/m3-deep-filter-list-20261017/target/candidate.

validate_sdk_results.py compiles and executes those actual SDK Results
in an isolated overlay containing exact parent dependencies. It checks
all nine generated hashes, strict Java21 compilation, tiered depth 65,536,
interpreter depth 8,192, the original overflow control, all three public
javap descriptor outputs, the 17 successor acceptance/refusal/no-write
cases, and the complete retained List/JNI/five-collector and Map/AVL gates.
SDK_RUNTIME_RECEIPT.json and individual stdout/stderr logs are written
under wrappers-work/sdk-validation. Only their completed records are
passing evidence; product worktree files are not written by this script.

## Inherited broader HOLD

The original m3/collections/wrappers/verify.py still binds its #992
Collection/Map outputs (cfe0eb... and 1dff49...). PR #1031 already
contains later exact refinements (e51e89... and 538f62...).
That older broad receiver checker and the separately identified installed
collection inventory need their own qualified successor. The parent PR
explicitly records this hold. They are not changed or reported passing
by this candidate.

The original pinned recipe tree, proof tree, verifier, and broader
workflow remain frozen. Full owning-reactor, source-built JDK,
jtreg/JCK, SWT promotion, benchmark acceptance, and general accelerator
integration are separate gates.
